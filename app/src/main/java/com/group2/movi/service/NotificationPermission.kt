package com.group2.movi.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal fun hasNotificationPermission(context: Context): Boolean =
    canPostNotifications(
        sdkInt = Build.VERSION.SDK_INT,
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    )

internal fun canPostNotifications(sdkInt: Int, permissionGranted: Boolean): Boolean {
    return sdkInt < Build.VERSION_CODES.TIRAMISU || permissionGranted
}
