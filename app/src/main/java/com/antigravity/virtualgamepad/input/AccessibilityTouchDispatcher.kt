package com.antigravity.virtualgamepad.input

import android.content.Context
import android.graphics.PointF
import com.antigravity.virtualgamepad.service.GamepadAccessibilityService

class AccessibilityTouchDispatcher(private val context: Context) : InputDispatcher {

    var joystickCenterProvider: (() -> PointF)? = null

    override fun onJoystickMoved(normX: Float, normY: Float, magnitude: Float) {
        val a11y = GamepadAccessibilityService.instance ?: return
        if (magnitude < 0.05f) {
            onJoystickReleased()
            return
        }

        // Get actual screen center of the on-screen joystick
        val center = joystickCenterProvider?.invoke()
        val anchorX = center?.x ?: (context.resources.displayMetrics.widthPixels * 0.20f)
        val anchorY = center?.y ?: (context.resources.displayMetrics.heightPixels * 0.70f)

        a11y.onJoystickMove(anchorX, anchorY, normX, normY, magnitude)
    }

    override fun onJoystickReleased() {
        val a11y = GamepadAccessibilityService.instance ?: return
        a11y.onJoystickRelease()
    }

    override fun onButtonPressed(action: ButtonAction) {
        val a11y = GamepadAccessibilityService.instance ?: return
        when (action) {
            ButtonAction.PAUSE -> {
                val dm = context.resources.displayMetrics
                // Tap center-top for tactical pause in KOTOR
                a11y.dispatchTap(dm.widthPixels * 0.5f, dm.heightPixels * 0.08f)
            }
        }
    }

    override fun onButtonReleased(action: ButtonAction) {
        // Tap finishes automatically
    }

    override fun release() {
        onJoystickReleased()
    }
}
