package com.group2.movi.config

import com.group2.movi.BuildConfig

object MapsConfig {
    private val placeholderValues = setOf(
        "",
        "DISABLED",
        "YOUR_MAPS_API_KEY",
        "YOUR_SHARED_MAPS_API_KEY"
    )

    fun configuredApiKey(rawValue: String): String? {
        val normalized = rawValue.trim()
        return normalized.takeIf { it !in placeholderValues }
    }

    val apiKey: String?
        get() = configuredApiKey(BuildConfig.MAPS_API_KEY)

    val isConfigured: Boolean
        get() = apiKey != null
}
