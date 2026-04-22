package com.group2.movi

import android.app.Application
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.group2.movi.config.MapsConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoviApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i(
            "MoviApplication",
            "Firebase project_id=${getString(R.string.project_id)}, google_app_id=${getString(R.string.google_app_id)}"
        )
        val mapsApiKey = MapsConfig.apiKey
        if (!Places.isInitialized() && mapsApiKey != null) {
            Places.initializeWithNewPlacesApiEnabled(this, mapsApiKey)
        }
    }
}
