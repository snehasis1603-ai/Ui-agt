package com.example

import com.example.network.AppConnectionException
import com.example.network.AppHttpException
import com.example.network.AppTimeoutException
import com.example.network.NetworkErrorHandler
import com.example.network.NetworkErrorType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NetworkErrorHandlerTest {

    @Test
    fun testSocketTimeoutExceptionMapping() {
        val exception = SocketTimeoutException("Read timed out")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.TIMEOUT, error.type)
        assertEquals(408, error.statusCode)
        assertTrue(error.isRetryable)
        assertTrue(error.userMessage.contains("timed out", ignoreCase = true))
    }

    @Test
    fun testAppTimeoutExceptionMapping() {
        val exception = AppTimeoutException("Custom timeout")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.TIMEOUT, error.type)
        assertTrue(error.isRetryable)
    }

    @Test
    fun testUnknownHostExceptionMapping() {
        val exception = UnknownHostException("generativelanguage.googleapis.com")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.CONNECTION_FAILURE, error.type)
        assertTrue(error.isRetryable)
        assertTrue(error.userMessage.contains("Network connection failure", ignoreCase = true))
    }

    @Test
    fun testConnectExceptionMapping() {
        val exception = ConnectException("Connection refused")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.CONNECTION_FAILURE, error.type)
        assertTrue(error.isRetryable)
    }

    @Test
    fun testHttp400BadRequestMapping() {
        val response = Response.error<String>(
            400,
            "{\"error\":{\"message\":\"API_KEY_INVALID\"}}".toResponseBody("application/json".toMediaTypeOrNull())
        )
        val exception = HttpException(response)
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.HTTP_BAD_REQUEST, error.type)
        assertEquals(400, error.statusCode)
        assertFalse(error.isRetryable)
        assertTrue(error.userMessage.contains("API key", ignoreCase = true))
    }

    @Test
    fun testHttp401UnauthorizedMapping() {
        val exception = AppHttpException(401, "Unauthorized", "Token expired")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.HTTP_UNAUTHORIZED, error.type)
        assertEquals(401, error.statusCode)
        assertFalse(error.isRetryable)
    }

    @Test
    fun testHttp403ForbiddenMapping() {
        val exception = AppHttpException(403, "Forbidden", "Permission denied")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.HTTP_FORBIDDEN, error.type)
        assertEquals(403, error.statusCode)
        assertFalse(error.isRetryable)
    }

    @Test
    fun testHttp404NotFoundMapping() {
        val exception = AppHttpException(404, "Not Found", "Model not found")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.HTTP_NOT_FOUND, error.type)
        assertEquals(404, error.statusCode)
    }

    @Test
    fun testHttp429RateLimitMapping() {
        val exception = AppHttpException(429, "Too Many Requests", "Quota exceeded")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.HTTP_RATE_LIMITED, error.type)
        assertEquals(429, error.statusCode)
        assertTrue(error.isRetryable)
        assertTrue(error.userMessage.contains("Rate Limit", ignoreCase = true))
    }

    @Test
    fun testHttp503ServerErrorMapping() {
        val exception = AppHttpException(503, "Service Unavailable", "Backend failure")
        val error = NetworkErrorHandler.parse(exception)

        assertEquals(NetworkErrorType.HTTP_SERVER_ERROR, error.type)
        assertEquals(503, error.statusCode)
        assertTrue(error.isRetryable)
        assertTrue(error.userMessage.contains("Temporarily Unavailable", ignoreCase = true))
    }
}
