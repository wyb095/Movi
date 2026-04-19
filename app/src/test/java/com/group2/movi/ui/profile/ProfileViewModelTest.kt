package com.group2.movi.ui.profile

import android.net.Uri
import com.group2.movi.data.repository.AuthRepository
import com.group2.movi.data.repository.ReviewRepository
import com.group2.movi.data.repository.TaskRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.CommuteEntry
import com.group2.movi.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>()
    private val taskRepo = mockk<TaskRepository>()
    private val reviewRepo = mockk<ReviewRepository>()
    private val authRepo = mockk<AuthRepository>(relaxed = true)

    @Test
    fun `update display name failure stays on page and exposes error`() = runTest {
        var onDoneCalled = false
        coEvery { userRepo.updateDisplayName("uid", "Alice") } returns Result.failure(
            IllegalStateException("network timeout")
        )

        val viewModel = createViewModel()
        viewModel.updateDisplayName("Alice") { onDoneCalled = true }
        advanceUntilIdle()

        assertFalse(onDoneCalled)
        assertFalse(viewModel.editProfileState.value.savingDisplayName)
        assertEquals(
            "Network error. Please check your connection and try again.",
            viewModel.editProfileState.value.errorMessage
        )
    }

    @Test
    fun `update display name success finishes and clears errors`() = runTest {
        var onDoneCalled = false
        coEvery { userRepo.updateDisplayName("uid", "Alice") } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.updateDisplayName("Alice") { onDoneCalled = true }
        advanceUntilIdle()

        assertTrue(onDoneCalled)
        assertFalse(viewModel.editProfileState.value.savingDisplayName)
        assertNull(viewModel.editProfileState.value.errorMessage)
    }

    @Test
    fun `photo upload failure exposes error and stops spinner`() = runTest {
        val uri = mockk<Uri>()
        coEvery { userRepo.uploadProfilePhoto("uid", uri) } returns Result.failure(
            IllegalStateException("storage network error")
        )

        val viewModel = createViewModel()
        viewModel.uploadPhoto(uri)
        advanceUntilIdle()

        assertFalse(viewModel.editProfileState.value.uploadingPhoto)
        assertEquals(
            "Network error. Please check your connection and try again.",
            viewModel.editProfileState.value.errorMessage
        )
    }

    @Test
    fun `remove schedule delegates to repository`() = runTest {
        val removed = CommuteEntry(dayOfWeek = "TUESDAY", departureTime = "18:00", port = "LO_WU", direction = "HK_TO_SZ")
        coEvery { userRepo.removeCommuteEntry("uid", removed) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.removeSchedule(removed)
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepo.removeCommuteEntry("uid", removed) }
    }

    private fun createViewModel(): ProfileViewModel {
        every { userRepo.currentUid } returns "uid"
        every { userRepo.observeUser("uid") } returns flowOf(null)
        every { taskRepo.observeTasksAsCarrier("uid") } returns flowOf(emptyList())
        every { reviewRepo.observeReviewsForUser("uid") } returns flowOf(emptyList())
        return ProfileViewModel(userRepo, taskRepo, reviewRepo, authRepo)
    }
}
