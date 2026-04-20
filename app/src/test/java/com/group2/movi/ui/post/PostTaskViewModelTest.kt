package com.group2.movi.ui.post

import com.group2.movi.domain.model.TaskCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import android.net.Uri
import io.mockk.mockk
import org.junit.Test

class PostTaskViewModelTest {

    @Test
    fun `step 0 validation trims title before checking length`() {
        val blankTitle = PostFormState(
            step = 0,
            category = TaskCategory.FOOD,
            title = "     "
        )
        val trimmedTitle = PostFormState(
            step = 0,
            category = TaskCategory.FOOD,
            title = "  Bring snacks  ",
            description = "   "
        )

        assertFalse(blankTitle.stepValid)
        assertTrue(trimmedTitle.stepValid)
    }

    @Test
    fun `step 1 validation rejects whitespace-only addresses`() {
        val invalid = PostFormState(
            step = 1,
            pickupAddress = "   ",
            dropoffAddress = "\n\t"
        )
        val valid = PostFormState(
            step = 1,
            pickupAddress = " Shenzhen Bay Port ",
            dropoffAddress = " Kowloon Tong "
        )

        assertFalse(invalid.stepValid)
        assertTrue(valid.stepValid)
    }

    @Test
    fun `selected photo must be uploaded before submit can proceed`() {
        val selectedPhoto = mockk<Uri>()
        val stateWithPendingPhoto = PostFormState(
            localPhotoUri = selectedPhoto,
            uploadedPhotoUrl = null
        )
        val stateWithUploadedPhoto = PostFormState(
            localPhotoUri = selectedPhoto,
            uploadedPhotoUrl = "https://example.com/task.jpg"
        )

        assertTrue(stateWithPendingPhoto.hasSelectedPhoto)
        assertFalse(stateWithPendingPhoto.hasUploadedPhoto)
        assertFalse(stateWithPendingPhoto.photoReadyForSubmit)
        assertTrue(stateWithUploadedPhoto.hasUploadedPhoto)
        assertTrue(stateWithUploadedPhoto.photoReadyForSubmit)
    }
}
