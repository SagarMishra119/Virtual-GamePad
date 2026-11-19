package com.antigravity.virtualgamepad.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.antigravity.virtualgamepad.repository.PreferencesRepository

class GamepadAccessibilityService : AccessibilityService() {

    private val tag = "GamepadA11yService"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeStroke: GestureDescription.StrokeDescription? = null
    private var isHolding = false
    private var lastX = 0f
    private var lastY = 0f
    private var currentTargetX = 0f
    private var currentTargetY = 0f
    private var isDispatching = false
    private var microStep = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(tag, "Accessibility Service Connected and Ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            val pkg = event.packageName?.toString() ?: return

            val prefsRepo = PreferencesRepository.getInstance(this)
            // If user turned off the overlay or it is not armed, DO NOTHING!
            if (!prefsRepo.isServiceArmed()) {
                return
            }

            // Ignore Android system UI, volume, transient overlays, keyboards, permission popups
            if (pkg == "com.android.systemui" ||
                pkg == "android" ||
                pkg.contains("inputmethod", ignoreCase = true) ||
                pkg.contains("permissioncontroller", ignoreCase = true) ||
                pkg.contains("gametools", ignoreCase = true) ||
                pkg.contains("gamebooster", ignoreCase = true) ||
                pkg.contains("gamepad", ignoreCase = true)
            ) {
                return
            }

            if (isLauncherPackage(pkg) || pkg == packageName) {
                Log.i(tag, "Launcher/Home/App active: $pkg -> Hide overlay")
                OverlayService.hide()
                return
            }

            val isTarget = prefsRepo.isGameSelected(pkg) || pkg.contains("kotor", ignoreCase = true)
            if (isTarget) {
                Log.i(tag, "Selected target game active: $pkg -> Show overlay")
                OverlayService.show(this)
            } else {
                Log.i(tag, "Non-target app active: $pkg -> Hide overlay")
                OverlayService.hide()
            }
        }
    }

    private fun isLauncherPackage(pkg: String): Boolean {
        return pkg == "com.android.launcher" ||
                pkg.contains("launcher", ignoreCase = true) ||
                pkg.contains("home", ignoreCase = true) ||
                pkg.contains("quickstep", ignoreCase = true) ||
                pkg == "com.google.android.apps.nexuslauncher" ||
                pkg == "com.sec.android.app.launcher" ||
                pkg == "com.miui.home" ||
                pkg == "com.oppo.launcher" ||
                pkg == "com.huawei.android.launcher" ||
                pkg == "com.oneplus.launcher"
    }

    override fun onInterrupt() {
        Log.w(tag, "Accessibility Service Interrupted")
        onJoystickRelease()
    }

    override fun onDestroy() {
        super.onDestroy()
        onJoystickRelease()
        instance = null
    }

    /**
     * Continuous drag gesture pump for joystick movement in KOTOR and Android games.
     * Uses continueStroke() with high-frequency keepalive to keep character running smoothly!
     */
    fun onJoystickMove(
        anchorX: Float,
        anchorY: Float,
        normX: Float,
        normY: Float,
        magnitude: Float
    ) {
        if (magnitude < 0.05f) {
            onJoystickRelease()
            return
        }

        // Distance in pixels to drag outwards from anchor (420px - 600px guarantees full running sprint in KOTOR)
        val dm = resources.displayMetrics
        val density = dm.density
        val maxDragPx = (150f * density).coerceIn(420f, 600f)
        val targetX = (anchorX + normX * maxDragPx * magnitude).coerceIn(20f, (dm.widthPixels - 20f).coerceAtLeast(100f))
        val targetY = (anchorY + normY * maxDragPx * magnitude).coerceIn(20f, (dm.heightPixels - 20f).coerceAtLeast(100f))

        currentTargetX = targetX
        currentTargetY = targetY

        if (!isHolding || activeStroke == null) {
            isHolding = true
            lastX = targetX
            lastY = targetY

            val path = Path().apply {
                moveTo(anchorX, anchorY)
                lineTo(targetX, targetY)
            }

            try {
                // willContinue = true keeps the touch held down on screen!
                activeStroke = GestureDescription.StrokeDescription(path, 0L, 60L, true)
                val gesture = GestureDescription.Builder().addStroke(activeStroke!!).build()
                isDispatching = true
                dispatchGesture(gesture, gestureCallback, mainHandler)
            } catch (e: Exception) {
                Log.e(tag, "Error starting gesture", e)
                isHolding = false
                activeStroke = null
                isDispatching = false
                dispatchDirectDrag(anchorX, anchorY, targetX, targetY)
            }
        }
    }

    private val gestureCallback = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            isDispatching = false
            if (isHolding && activeStroke != null) {
                try {
                    microStep = !microStep
                    // Subtle 1.5px sub-pixel offset ensures Android's InputDispatcher NEVER drops
                    // the ACTION_MOVE events as duplicates, sending active 25 Hz move events to KOTOR!
                    val jitter = if (microStep) 1.5f else -1.5f
                    val dm = resources.displayMetrics
                    val nextX = (currentTargetX + jitter).coerceIn(20f, (dm.widthPixels - 20f).coerceAtLeast(100f))
                    val nextY = (currentTargetY + jitter).coerceIn(20f, (dm.heightPixels - 20f).coerceAtLeast(100f))

                    val path = Path().apply {
                        moveTo(lastX, lastY)
                        lineTo(nextX, nextY)
                    }
                    lastX = nextX
                    lastY = nextY

                    activeStroke = activeStroke?.continueStroke(path, 0L, 40L, true)
                    if (activeStroke != null) {
                        val gesture = GestureDescription.Builder().addStroke(activeStroke!!).build()
                        isDispatching = true
                        dispatchGesture(gesture, this, mainHandler)
                    } else {
                        isHolding = false
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error continuing stroke", e)
                    isHolding = false
                    activeStroke = null
                }
            }
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            isDispatching = false
            activeStroke = null
            isHolding = false
        }
    }

    fun onJoystickRelease() {
        if (!isHolding) return
        isHolding = false

        if (activeStroke != null) {
            try {
                // Finish stroke with willContinue = false to trigger ACTION_UP (release touch)
                val path = Path().apply {
                    moveTo(lastX, lastY)
                    lineTo(lastX, lastY)
                }
                val finishStroke = activeStroke?.continueStroke(path, 0L, 20L, false)
                if (finishStroke != null) {
                    val gesture = GestureDescription.Builder().addStroke(finishStroke).build()
                    dispatchGesture(gesture, null, mainHandler)
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                activeStroke = null
                isDispatching = false
            }
        }
    }

    private fun dispatchDirectDrag(startX: Float, startY: Float, endX: Float, endY: Float) {
        try {
            val dm = resources.displayMetrics
            val safeEndX = endX.coerceIn(20f, (dm.widthPixels - 20f).coerceAtLeast(100f))
            val safeEndY = endY.coerceIn(20f, (dm.heightPixels - 20f).coerceAtLeast(100f))
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(safeEndX, safeEndY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 120L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, mainHandler)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun dispatchTap(x: Float, y: Float) {
        try {
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, mainHandler)
        } catch (e: Exception) {
            Log.e(tag, "Failed to dispatch tap", e)
        }
    }

    companion object {
        @Volatile
        var instance: GamepadAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
