package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        viewModel = AuthViewModel(app)
        viewModel.signOut()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signIn with owner credentials Snehasis and Snehasis@2007 succeeds`() = runTest(testDispatcher) {
        viewModel.signIn("Snehasis", "Snehasis@2007")
        testScheduler.advanceUntilIdle()

        assertEquals("Snehasis", viewModel.userEmail.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `signIn with wrong username fails with error`() = runTest(testDispatcher) {
        viewModel.signIn("UnknownUser", "Snehasis@2007")
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.userEmail.value)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Snehasis"))
    }

    @Test
    fun `signIn with wrong security code fails with error`() = runTest(testDispatcher) {
        viewModel.signIn("Snehasis", "WrongCode123")
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.userEmail.value)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Snehasis@2007"))
    }

    @Test
    fun `activateAsOwner instantly logs in as Snehasis`() = runTest(testDispatcher) {
        viewModel.activateAsOwner()
        testScheduler.advanceUntilIdle()

        assertEquals("Snehasis", viewModel.userEmail.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `signOut clears authenticated user`() = runTest(testDispatcher) {
        viewModel.activateAsOwner()
        testScheduler.advanceUntilIdle()
        assertEquals("Snehasis", viewModel.userEmail.value)

        viewModel.signOut()
        assertNull(viewModel.userEmail.value)
    }
}
