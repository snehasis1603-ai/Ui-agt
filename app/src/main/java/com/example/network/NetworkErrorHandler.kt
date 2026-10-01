package com.example.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Types of network errors that can occur during Retrofit calls.
 */
internal object SafeLog {
    fun d(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        try {
            Log.e(tag, msg, tr)
        } catch (_: Throwable) {
            println("[$tag] ERROR: $msg ${tr?.message ?: ""}")
        }
    }
}

enum class NetworkErrorType {
    TIMEOUT,
    CONNECTION_FAILURE,
    HTTP_BAD_REQUEST,       // 400
    HTTP_UNAUTHORIZED,      // 401
    HTTP_FORBIDDEN,         // 403
    HTTP_NOT_FOUND,         // 404
    HTTP_RATE_LIMITED,      // 429
    HTTP_SERVER_ERROR,      // 500-599
    HTTP_OTHER,
    SSL_ERROR,
    UNKNOWN
}

/**
 * Encapsulates structured error information with user-friendly descriptions and technical details.
 */
data class NetworkErrorDetails(
    val type: NetworkErrorType,
    val userMessage: String,
    val technicalMessage: String,
    val statusCode: Int? = null,
    val isRetryable: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Custom exceptions to wrap and preserve underlying network causes.
 */
class AppTimeoutException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

class AppConnectionException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

class AppHttpException(
    val statusCode: Int,
    val statusMessage: String,
    val errorBody: String?,
    cause: Throwable? = null
) : IOException("HTTP $statusCode $statusMessage: $errorBody", cause)

/**
 * OkHttp Interceptor that logs network traffic, catches network errors,
 * inspects HTTP error status codes, and logs diagnostic information.
 */
class NetworkErrorLoggingInterceptor : Interceptor {
    companion object {
        private const val TAG = "RetrofitNetwork"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.encodedPath
        val method = request.method
        val startTime = System.currentTimeMillis()

        SafeLog.d(TAG, "==> START $method $url")

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: SocketTimeoutException) {
            val elapsed = System.currentTimeMillis() - startTime
            SafeLog.e(TAG, "<== TIMEOUT [${elapsed}ms] for $method $url: ${e.message}", e)
            throw AppTimeoutException(
                "Request timed out after ${elapsed}ms connecting to $url",
                e
            )
        } catch (e: UnknownHostException) {
            val elapsed = System.currentTimeMillis() - startTime
            SafeLog.e(TAG, "<== CONNECTION FAILED (Unknown Host) [${elapsed}ms] for $method $url: ${e.message}", e)
            throw AppConnectionException(
                "Unable to resolve host. Please verify internet connectivity.",
                e
            )
        } catch (e: ConnectException) {
            val elapsed = System.currentTimeMillis() - startTime
            SafeLog.e(TAG, "<== CONNECTION REFUSED [${elapsed}ms] for $method $url: ${e.message}", e)
            throw AppConnectionException(
                "Connection failed. Server refused connection or is unreachable.",
                e
            )
        } catch (e: NoRouteToHostException) {
            val elapsed = System.currentTimeMillis() - startTime
            SafeLog.e(TAG, "<== NO ROUTE TO HOST [${elapsed}ms] for $method $url: ${e.message}", e)
            throw AppConnectionException(
                "Network unreachable. Check your router or cellular connection.",
                e
            )
        } catch (e: SSLException) {
            val elapsed = System.currentTimeMillis() - startTime
            SafeLog.e(TAG, "<== SSL HANDSHAKE ERROR [${elapsed}ms] for $method $url: ${e.message}", e)
            throw IOException("Secure connection could not be established: ${e.message}", e)
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - startTime
            SafeLog.e(TAG, "<== NETWORK IO ERROR [${elapsed}ms] for $method $url: ${e.message}", e)
            throw e
        }

        val elapsed = System.currentTimeMillis() - startTime
        val code = response.code

        if (!response.isSuccessful) {
            // Read a peek of the error response without consuming the original response body
            val peekedErrorBody = try {
                response.peekBody(1024 * 32).string()
            } catch (e: Exception) {
                "[Unable to read error body: ${e.message}]"
            }

            SafeLog.e(
                TAG,
                "<== HTTP ERROR $code ${response.message} [${elapsed}ms] for $method $url\nResponse Body: $peekedErrorBody"
            )
        } else {
            SafeLog.d(TAG, "<== SUCCESS $code ${response.message} [${elapsed}ms] for $method $url")
        }

        return response
    }
}

/**
 * Parser utility to map network exceptions into user-friendly messages and structured error details.
 */
object NetworkErrorHandler {
    private const val TAG = "NetworkErrorHandler"

    fun parse(throwable: Throwable): NetworkErrorDetails {
        SafeLog.e(TAG, "Parsing network exception: ${throwable::class.java.simpleName}: ${throwable.message}", throwable)

        return when (throwable) {
            is AppTimeoutException, is SocketTimeoutException -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.TIMEOUT,
                    userMessage = "Connection timed out. The server took too long to respond. Please check your connection and try again.",
                    technicalMessage = "SocketTimeoutException: ${throwable.message}",
                    statusCode = 408,
                    isRetryable = true
                )
            }

            is AppConnectionException, is UnknownHostException, is ConnectException, is NoRouteToHostException -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.CONNECTION_FAILURE,
                    userMessage = "Network connection failure. Unable to reach server. Please verify your Wi-Fi or mobile data.",
                    technicalMessage = "${throwable::class.java.simpleName}: ${throwable.message}",
                    statusCode = null,
                    isRetryable = true
                )
            }

            is SSLException -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.SSL_ERROR,
                    userMessage = "Secure connection error. Unable to verify server security certificate.",
                    technicalMessage = "SSLException: ${throwable.message}",
                    statusCode = null,
                    isRetryable = false
                )
            }

            is AppHttpException -> {
                mapStatusCode(
                    code = throwable.statusCode,
                    statusMsg = throwable.statusMessage,
                    rawErrorBody = throwable.errorBody
                )
            }

            is HttpException -> {
                val code = throwable.code()
                val statusMsg = throwable.message()
                val errorBody = try {
                    throwable.response()?.errorBody()?.string()
                } catch (e: Exception) {
                    null
                }
                mapStatusCode(code = code, statusMsg = statusMsg, rawErrorBody = errorBody)
            }

            else -> {
                val message = throwable.localizedMessage ?: "An unexpected error occurred."
                // Check if message contains known HTTP code hints
                when {
                    message.contains("400") -> mapStatusCode(400, "Bad Request", message)
                    message.contains("401") -> mapStatusCode(401, "Unauthorized", message)
                    message.contains("403") -> mapStatusCode(403, "Forbidden", message)
                    message.contains("404") -> mapStatusCode(404, "Not Found", message)
                    message.contains("429") -> mapStatusCode(429, "Too Many Requests", message)
                    message.contains("500") -> mapStatusCode(500, "Internal Server Error", message)
                    message.contains("502") || message.contains("503") || message.contains("504") ->
                        mapStatusCode(503, "Service Unavailable", message)
                    message.contains("timeout", ignoreCase = true) -> {
                        NetworkErrorDetails(
                            type = NetworkErrorType.TIMEOUT,
                            userMessage = "Request timed out. Please try again.",
                            technicalMessage = message,
                            statusCode = 408,
                            isRetryable = true
                        )
                    }
                    else -> {
                        NetworkErrorDetails(
                            type = NetworkErrorType.UNKNOWN,
                            userMessage = "Communication failure: $message",
                            technicalMessage = "${throwable::class.java.name}: $message",
                            statusCode = null,
                            isRetryable = true
                        )
                    }
                }
            }
        }
    }

    private fun mapStatusCode(code: Int, statusMsg: String, rawErrorBody: String?): NetworkErrorDetails {
        val technical = "HTTP $code ($statusMsg) ${rawErrorBody?.take(200) ?: ""}".trim()

        return when (code) {
            400 -> {
                val isApiKeyIssue = rawErrorBody?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                        rawErrorBody?.contains("INVALID_KEY", ignoreCase = true) == true
                val isBadFormat = rawErrorBody?.contains("EMAIL_NOT_FOUND", ignoreCase = true) == true ||
                        rawErrorBody?.contains("INVALID_PASSWORD", ignoreCase = true) == true

                val userMessage = when {
                    isApiKeyIssue -> "Configuration Error (HTTP 400): Invalid or misconfigured API key."
                    isBadFormat -> "Invalid credentials provided (HTTP 400). Please check your email and password."
                    else -> "Bad Request (HTTP 400): The service rejected the request parameters. Check your credentials or configuration."
                }

                NetworkErrorDetails(
                    type = NetworkErrorType.HTTP_BAD_REQUEST,
                    userMessage = userMessage,
                    technicalMessage = technical,
                    statusCode = 400,
                    isRetryable = false
                )
            }

            401 -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.HTTP_UNAUTHORIZED,
                    userMessage = "Authentication Failed (HTTP 401): Invalid or expired authorization token.",
                    technicalMessage = technical,
                    statusCode = 401,
                    isRetryable = false
                )
            }

            403 -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.HTTP_FORBIDDEN,
                    userMessage = "Access Forbidden (HTTP 403): You do not have permission to access this AI resource or API key quota is restricted.",
                    technicalMessage = technical,
                    statusCode = 403,
                    isRetryable = false
                )
            }

            404 -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.HTTP_NOT_FOUND,
                    userMessage = "Service Not Found (HTTP 404): The requested AI endpoint or model could not be found.",
                    technicalMessage = technical,
                    statusCode = 404,
                    isRetryable = false
                )
            }

            429 -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.HTTP_RATE_LIMITED,
                    userMessage = "Rate Limit Exceeded (HTTP 429): Too many requests sent. Please pause a moment before trying again.",
                    technicalMessage = technical,
                    statusCode = 429,
                    isRetryable = true
                )
            }

            in 500..599 -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.HTTP_SERVER_ERROR,
                    userMessage = "AI Service Temporarily Unavailable (HTTP $code): The remote servers encountered an error. Please try again shortly.",
                    technicalMessage = technical,
                    statusCode = code,
                    isRetryable = true
                )
            }

            else -> {
                NetworkErrorDetails(
                    type = NetworkErrorType.HTTP_OTHER,
                    userMessage = "HTTP Error $code: $statusMsg",
                    technicalMessage = technical,
                    statusCode = code,
                    isRetryable = code >= 500
                )
            }
        }
    }
}
