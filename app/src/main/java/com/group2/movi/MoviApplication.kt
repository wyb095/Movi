package com.group2.movi

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp(Application::class)
class MoviApplication : Hilt_MoviApplication() {

    override fun onCreate() {
        super.onCreate()
        if (hasUsableMapsApiKey() && !Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
        }
    }

    private fun hasUsableMapsApiKey(): Boolean {
        return BuildConfig.MAPS_API_KEY.isNotBlank() &&
            BuildConfig.MAPS_API_KEY != DEFAULT_MAPS_API_KEY
    }

    private companion object {
        const val DEFAULT_MAPS_API_KEY = "DEFAULT_API_KEY"
    }
}
