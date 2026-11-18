package com.antigravity.virtualgamepad.input

import android.content.Context
import android.graphics.PointF
import android.util.Log
import com.antigravity.virtualgamepad.model.InputMode
import com.antigravity.virtualgamepad.repository.PreferencesRepository
import com.antigravity.virtualgamepad.shizuku.ShizukuManager

class InputRouter(private val context: Context) : InputDispatcher {

    private val tag = "InputRouter"
    var uhidDispatcher: UhidGamepadDispatcher? = null
    var touchDispatcher: AccessibilityTouchDispatcher? = null

    private var activeMode: InputMode = InputMode.TOUCH

    init {
        val prefs = PreferencesRepository.getInstance(context)
        activeMode = prefs.getSettings().inputMode
        initDispatchers()
    }

    private fun initDispatchers() {
        uhidDispatcher = UhidGamepadDispatcher()
        touchDispatcher = AccessibilityTouchDispatcher(context)
    }

    fun setJoystickCenterProvider(provider: () -> PointF) {
        touchDispatcher?.joystickCenterProvider = provider
    }

    fun setMode(mode: InputMode) {
        if (activeMode != mode) {
            Log.i(tag, "Switching input mode to $mode")
            activeMode = mode
            PreferencesRepository.getInstance(context).setInputMode(mode)
        }
    }

    fun getActiveMode(): InputMode = activeMode

    override fun onJoystickMoved(normX: Float, normY: Float, magnitude: Float) {
        // If in GAMEPAD mode but Shizuku is not available, automatically fall back to touch!
        if (activeMode == InputMode.GAMEPAD && ShizukuManager.isAvailable.value && ShizukuManager.hasPermission.value) {
            uhidDispatcher?.onJoystickMoved(normX, normY, magnitude)
        } else {
            touchDispatcher?.onJoystickMoved(normX, normY, magnitude)
        }
    }

    override fun onJoystickReleased() {
        if (activeMode == InputMode.GAMEPAD && ShizukuManager.isAvailable.value && ShizukuManager.hasPermission.value) {
            uhidDispatcher?.onJoystickReleased()
        } else {
            touchDispatcher?.onJoystickReleased()
        }
    }

    override fun onButtonPressed(action: ButtonAction) {
        if (activeMode == InputMode.GAMEPAD && ShizukuManager.isAvailable.value && ShizukuManager.hasPermission.value) {
            uhidDispatcher?.onButtonPressed(action)
        } else {
            touchDispatcher?.onButtonPressed(action)
        }
    }

    override fun onButtonReleased(action: ButtonAction) {
        if (activeMode == InputMode.GAMEPAD && ShizukuManager.isAvailable.value && ShizukuManager.hasPermission.value) {
            uhidDispatcher?.onButtonReleased(action)
        } else {
            touchDispatcher?.onButtonReleased(action)
        }
    }

    override fun release() {
        uhidDispatcher?.release()
        touchDispatcher?.release()
        uhidDispatcher = null
        touchDispatcher = null
    }
}
