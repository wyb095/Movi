package com.group2.movi.ui.auth

import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.group2.movi.data.repository.AuthRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepo = mockk<AuthRepository>()
    private val userRepo = mockk<UserRepository>(relaxed = true)
    private val firebaseMessaging = mockk<FirebaseMessaging> {
        every { token } returns Tasks.forResult("token")
    }

    @Test
    fun `password reset success uses notice state instead of error`() = runTest {
        coEvery { authRepo.sendPasswordReset("user@example.com") } returns Result.success(Unit)

        val viewModel = AuthViewModel(authRepo, userRepo, firebaseMessaging)
        viewModel.sendPasswordReset(" user@example.com ")
        advanceUntilIdle()

        assertEquals(
            AuthUiState.Notice("Password reset email sent. Check your inbox."),
            viewModel.state.value
        )
    }

    @Test
    fun `password reset failure keeps error state`() = runTest {
        coEvery { authRepo.sendPasswordReset("user@example.com") } returns Result.failure(
            IllegalStateException("network request failed")
        )

        val viewModel = AuthViewModel(authRepo, userRepo, firebaseMessaging)
        viewModel.sendPasswordReset("user@example.com")
        advanceUntilIdle()

        assertEquals(
            AuthUiState.Error("Network error. Check your connection."),
            viewModel.state.value
        )
    }
}
