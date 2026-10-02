package com.israadev.nuxlauncher.core.device

import android.app.Activity
import android.content.Context
import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice

private const val TAG = "PhysicalMouseChecker"

object PhysicalMouseChecker {
    /**
     * Menandakan apakah saat ini ada mouse fisik yang terhubung ke perangkat
     */
    var physicalMouseConnected: Boolean = false
        private set

    private var isInitialized = false

    fun initChecker(activity: Activity) {
        if (isInitialized) {
            physicalMouseConnected = isPhysicalMouseConnected()
            return
        }
        isInitialized = true

        physicalMouseConnected = isPhysicalMouseConnected()
        Log.i(TAG, "Initialization complete, physical mouse connection status: $physicalMouseConnected")

        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                if (deviceId.isMouseId()) {
                    Log.i(TAG, "Physical mouse connected, deviceId: $deviceId")
                    physicalMouseConnected = true
                }
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                if (deviceId.isMouseId()) {
                    Log.i(TAG, "Physical mouse disconnected, deviceId: $deviceId")
                    physicalMouseConnected = false
                } else {
                    physicalMouseConnected = isPhysicalMouseConnected()
                    Log.i(TAG, "Fallback check for physical mouse connection status: $physicalMouseConnected")
                }
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                // Device properties changed
                physicalMouseConnected = isPhysicalMouseConnected()
            }
        }

        try {
            val inputManager = activity.getSystemService(Context.INPUT_SERVICE) as? InputManager
            inputManager?.registerInputDeviceListener(listener, null)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to register InputDeviceListener", e)
        }
    }
}

/**
 * Memastikan apakah deviceId ini merupakan mouse fisik
 */
private fun Int.isMouseId(): Boolean {
    return InputDevice.getDevice(this)?.let { device ->
        device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
    } ?: false
}

/**
 * Memeriksa apakah ada mouse fisik yang sedang terhubung
 */
private fun isPhysicalMouseConnected(): Boolean {
    return try {
        InputDevice.getDeviceIds()
            .takeIf { it.isNotEmpty() }
            ?.any { it.isMouseId() }
            ?: false
    } catch (e: Throwable) {
        false
    }
}
