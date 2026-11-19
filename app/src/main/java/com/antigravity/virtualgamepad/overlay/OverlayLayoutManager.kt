package com.antigravity.virtualgamepad.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.antigravity.virtualgamepad.input.ButtonAction
import com.antigravity.virtualgamepad.input.InputRouter
import com.antigravity.virtualgamepad.model.ElementConfig
import com.antigravity.virtualgamepad.model.ElementType
import com.antigravity.virtualgamepad.model.InputMode
import com.antigravity.virtualgamepad.model.LayoutSettings
import com.antigravity.virtualgamepad.overlay.views.ActionButtonView
import com.antigravity.virtualgamepad.overlay.views.DraggableOverlayContainer
import com.antigravity.virtualgamepad.overlay.views.FloatingToolbarView
import com.antigravity.virtualgamepad.overlay.views.JoystickView
import com.antigravity.virtualgamepad.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OverlayLayoutManager(
    private val context: Context,
    private val inputRouter: InputRouter,
    private val onCloseRequested: () -> Unit
) {

    private val tag = "OverlayLayoutManager"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefsRepo = PreferencesRepository.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private class ElementHolder(
        val config: ElementConfig,
        val container: DraggableOverlayContainer,
        val content: View,
        val params: WindowManager.LayoutParams,
        var isAddedToWm: Boolean = false
    )

    private val elementHolders = mutableMapOf<String, ElementHolder>()
    private var toolbarView: FloatingToolbarView? = null
    private var toolbarParams: WindowManager.LayoutParams? = null
    private var isToolbarAdded = false
    private var savedToolbarX = 0
    private var savedToolbarY = 0

    private var isEditMode = false
    private var isManuallyHidden = false
    private var isTargetAppActive = false

    fun init() {
        removeAll()

        val settings = prefsRepo.getSettings()
        val metrics = context.resources.displayMetrics
        savedToolbarY = dpToPx(16)

        // 1. Build and add elements to WindowManager once
        for (config in settings.elements) {
            buildAndAddElement(config, settings, metrics)
        }

        // 2. Build and add toolbar to WindowManager once
        buildAndAddToolbar()

        // 3. Connect touch anchor provider (positioned outside joystick window so game receives touches!)
        inputRouter.setJoystickCenterProvider {
            getTouchAnchorScreen()
        }

        // 4. Initial visibility: hide if target app is not active yet
        updateWindowsVisibility()

        // 5. Observe real-time settings changes
        scope.launch {
            prefsRepo.settingsFlow.collect { newSettings ->
                applySettingsRealTime(newSettings)
            }
        }
    }

    private fun getScreenBounds(): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            Point(dm.widthPixels, dm.heightPixels)
        }
    }

    /**
     * Calculates the touch anchor point on the game screen.
     * Positioned safely outside the joystick window boundary so synthetic touches hit KOTOR directly
     * and strictly clamped within physical screen bounds so gestures are never rejected.
     */
    fun getTouchAnchorScreen(): PointF {
        val holder = elementHolders["joystick_movement"]
        val bounds = getScreenBounds()
        val screenW = bounds.x
        val screenH = bounds.y
        val density = context.resources.displayMetrics.density
        val dragDist = (150f * density).coerceIn(420f, 600f)

        return if (holder != null) {
            val params = holder.params
            val offsetRight = (dragDist + dpToPx(30)).toInt()
            val minAnchorX = (params.x + params.width + 40).toFloat()
            val maxAnchorX = (screenW * 0.48f).coerceAtLeast(minAnchorX)
            val anchorX = (params.x + params.width + offsetRight).toFloat().coerceIn(minAnchorX, maxAnchorX)
            val anchorY = (params.y + params.height / 2f).toFloat().coerceIn(
                dragDist + 40f,
                (screenH - dragDist - 40f).coerceAtLeast(100f)
            )
            PointF(anchorX, anchorY)
        } else {
            PointF(screenW * 0.35f, screenH * 0.65f)
        }
    }

    private fun buildAndAddElement(
        config: ElementConfig,
        settings: LayoutSettings,
        metrics: DisplayMetrics
    ) {
        if (elementHolders.containsKey(config.id)) return

        val shouldShow = isTargetAppActive && !isManuallyHidden && config.isEnabled
        val bounds = getScreenBounds()
        val sizePx = dpToPx(config.sizeDp)
        val xPx = dpToPx(config.xDp).coerceIn(0, (bounds.x - sizePx).coerceAtLeast(0))
        val yPx = dpToPx(config.yDp).coerceIn(0, (bounds.y - sizePx).coerceAtLeast(0))

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val initialFlags = if (shouldShow) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
        } else {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutFlag,
            initialFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (shouldShow) xPx else -10000
            y = if (shouldShow) yPx else -10000
            alpha = if (shouldShow) config.opacity else 0f
        }

        val container = DraggableOverlayContainer(context).apply {
            this.isEditMode = this@OverlayLayoutManager.isEditMode
            visibility = if (shouldShow) View.VISIBLE else View.GONE
        }

        val content: View = when (config.type) {
            ElementType.JOYSTICK -> {
                JoystickView(
                    context = context,
                    config = config,
                    deadzone = settings.deadzone,
                    onMove = { normX, normY, mag -> inputRouter.onJoystickMoved(normX, normY, mag) },
                    onRelease = { inputRouter.onJoystickReleased() }
                )
            }
            ElementType.BUTTON_PAUSE -> {
                ActionButtonView(
                    context = context,
                    config = config,
                    action = ButtonAction.PAUSE,
                    onPress = { inputRouter.onButtonPressed(it) },
                    onRelease = { inputRouter.onButtonReleased(it) }
                )
            }
        }

        container.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setupContainerDragging(container, params, config)

        try {
            windowManager.addView(container, params)
            val holder = ElementHolder(config, container, content, params, isAddedToWm = true)
            elementHolders[config.id] = holder
        } catch (e: Exception) {
            Log.e(tag, "Failed to add window for ${config.id}", e)
        }
    }

    private fun setupContainerDragging(
        container: DraggableOverlayContainer,
        params: WindowManager.LayoutParams,
        config: ElementConfig
    ) {
        var startX = 0
        var startY = 0

        container.onDragStart = {
            startX = params.x
            startY = params.y
        }

        container.onDragMove = { dx, dy ->
            val bounds = getScreenBounds()
            params.x = (startX + dx).coerceIn(0, bounds.x - params.width)
            params.y = (startY + dy).coerceIn(0, bounds.y - params.height)
            try {
                windowManager.updateViewLayout(container, params)
            } catch (e: Exception) {
                // ignore
            }
        }

        container.onDragEnd = {
            prefsRepo.updateElement(config.id) {
                it.copy(xDp = pxToDp(params.x), yDp = pxToDp(params.y))
            }
        }
    }

    private fun buildAndAddToolbar() {
        if (isToolbarAdded) return

        val shouldShow = isTargetAppActive
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val initialFlags = if (shouldShow) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
        } else {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        toolbarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            initialFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = if (shouldShow) savedToolbarX else -10000
            y = if (shouldShow) savedToolbarY else -10000
            alpha = if (shouldShow) 1f else 0f
        }

        toolbarView = FloatingToolbarView(
            context = context,
            isEditMode = isEditMode,
            currentMode = inputRouter.getActiveMode(),
            onToggleEditMode = { editMode ->
                setEditMode(editMode)
            },
            onToggleInputMode = {
                val nextMode = if (inputRouter.getActiveMode() == InputMode.GAMEPAD) {
                    InputMode.TOUCH
                } else {
                    InputMode.GAMEPAD
                }
                inputRouter.setMode(nextMode)
                toolbarView?.updateInputMode(nextMode)
            },
            onCycleSize = {
                cycleJoystickSize()
            },
            onToggleHide = { hide ->
                isManuallyHidden = hide
                updateWindowsVisibility()
            },
            onClose = {
                onCloseRequested()
            }
        )

        var dragStartX = 0
        var dragStartY = 0
        toolbarView?.onToolbarDragStart = {
            toolbarParams?.let { lp ->
                dragStartX = lp.x
                dragStartY = lp.y
            }
        }
        toolbarView?.onToolbarDragMove = { dx, dy ->
            toolbarParams?.let { lp ->
                lp.x = dragStartX + dx
                lp.y = (dragStartY + dy).coerceAtLeast(0)
                savedToolbarX = lp.x
                savedToolbarY = lp.y
                try {
                    windowManager.updateViewLayout(toolbarView, lp)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        try {
            windowManager.addView(toolbarView, toolbarParams)
            isToolbarAdded = true
        } catch (e: Exception) {
            Log.e(tag, "Failed to add toolbar to window manager", e)
        }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        for ((_, holder) in elementHolders) {
            val container = holder.container
            container.isEditMode = enabled
            if (isEditMode) {
                val editBorder = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setStroke(dpToPx(2), Color.parseColor("#FFD700"))
                    setColor(Color.parseColor("#35FFD700"))
                    cornerRadius = dpToPx(16).toFloat()
                }
                container.background = editBorder
            } else {
                container.background = null
            }
        }
    }

    private fun cycleJoystickSize() {
        val currentConfig = prefsRepo.getSettings().elements.find { it.id == "joystick_movement" } ?: return

        val newSize = when (currentConfig.sizeDp) {
            in 0..119 -> 140
            in 120..149 -> 170
            in 150..179 -> 200
            else -> 100
        }

        prefsRepo.updateElement(currentConfig.id) { it.copy(sizeDp = newSize) }
    }

    fun setOverlayVisible(visible: Boolean) {
        if (isTargetAppActive == visible) return
        isTargetAppActive = visible
        updateWindowsVisibility()
    }

    /**
     * Stably hides/shows windows by adjusting width/height/flags with updateViewLayout.
     * NEVER calls removeView/addView in a loop, preventing WindowManager corruption.
     */
    private fun updateWindowsVisibility() {
        val shouldShowElements = isTargetAppActive && !isManuallyHidden

        // 1. Update Elements (Joystick & Buttons)
        for ((_, holder) in elementHolders) {
            val config = holder.config
            val container = holder.container
            val params = holder.params

            if (shouldShowElements && config.isEnabled) {
                val bounds = getScreenBounds()
                val sizePx = dpToPx(config.sizeDp)
                params.width = sizePx
                params.height = sizePx
                params.x = dpToPx(config.xDp).coerceIn(0, (bounds.x - sizePx).coerceAtLeast(0))
                params.y = dpToPx(config.yDp).coerceIn(0, (bounds.y - sizePx).coerceAtLeast(0))
                params.alpha = config.opacity
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                container.visibility = View.VISIBLE
            } else {
                params.x = -10000
                params.y = -10000
                params.alpha = 0f
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                container.visibility = View.GONE
            }

            if (holder.isAddedToWm) {
                try {
                    windowManager.updateViewLayout(container, params)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        // 2. Update Toolbar Visibility
        if (toolbarView != null && toolbarParams != null && isToolbarAdded) {
            if (isTargetAppActive) {
                toolbarParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
                toolbarParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT
                toolbarParams!!.x = savedToolbarX
                toolbarParams!!.y = savedToolbarY
                toolbarParams!!.alpha = 1f
                toolbarParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                toolbarView!!.visibility = View.VISIBLE
            } else {
                toolbarParams!!.x = -10000
                toolbarParams!!.alpha = 0f
                toolbarParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                toolbarView!!.visibility = View.GONE
            }
            try {
                windowManager.updateViewLayout(toolbarView, toolbarParams)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun applySettingsRealTime(settings: LayoutSettings) {
        val shouldShow = isTargetAppActive && !isManuallyHidden

        for (config in settings.elements) {
            val holder = elementHolders[config.id]
            if (holder != null) {
                val params = holder.params
                val container = holder.container

                if (shouldShow && config.isEnabled) {
                    val bounds = getScreenBounds()
                    val newSizePx = dpToPx(config.sizeDp)
                    params.width = newSizePx
                    params.height = newSizePx
                    params.x = dpToPx(config.xDp).coerceIn(0, (bounds.x - newSizePx).coerceAtLeast(0))
                    params.y = dpToPx(config.yDp).coerceIn(0, (bounds.y - newSizePx).coerceAtLeast(0))
                    params.alpha = config.opacity
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    container.visibility = View.VISIBLE
                } else {
                    params.x = -10000
                    params.y = -10000
                    params.alpha = 0f
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    container.visibility = View.GONE
                }

                holder.content.alpha = config.opacity

                if (holder.content is JoystickView) {
                    holder.content.deadzone = settings.deadzone
                }

                if (holder.isAddedToWm) {
                    try {
                        windowManager.updateViewLayout(container, params)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } else {
                buildAndAddElement(config, settings, context.resources.displayMetrics)
            }
        }
    }

    /**
     * Responds to device orientation and configuration changes,
     * ensuring elements stay within active screen boundaries in landscape/portrait.
     */
    fun onConfigurationChanged(newConfig: Configuration) {
        val bounds = getScreenBounds()
        for ((_, holder) in elementHolders) {
            val params = holder.params
            val container = holder.container
            if (holder.isAddedToWm && container.visibility == View.VISIBLE) {
                val sizePx = params.width
                params.x = params.x.coerceIn(0, (bounds.x - sizePx).coerceAtLeast(0))
                params.y = params.y.coerceIn(0, (bounds.y - sizePx).coerceAtLeast(0))
                try {
                    windowManager.updateViewLayout(container, params)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        if (toolbarView != null && toolbarParams != null && isToolbarAdded && toolbarView!!.visibility == View.VISIBLE) {
            savedToolbarX = 0
            savedToolbarY = dpToPx(16)
            toolbarParams!!.x = savedToolbarX
            toolbarParams!!.y = savedToolbarY
            try {
                windowManager.updateViewLayout(toolbarView, toolbarParams)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * Completely removes all views from WindowManager immediately upon termination.
     */
    fun removeAll() {
        for ((_, holder) in elementHolders) {
            if (holder.isAddedToWm) {
                if (holder.content is JoystickView) {
                    holder.content.cancelAnimations()
                }
                try {
                    windowManager.removeViewImmediate(holder.container)
                } catch (e: Exception) {
                    try {
                        windowManager.removeView(holder.container)
                    } catch (e2: Exception) {
                        // ignore
                    }
                }
                holder.isAddedToWm = false
            }
        }
        elementHolders.clear()

        if (isToolbarAdded && toolbarView != null) {
            try {
                windowManager.removeViewImmediate(toolbarView)
            } catch (e: Exception) {
                try {
                    windowManager.removeView(toolbarView)
                } catch (e2: Exception) {
                    // ignore
                }
            }
            isToolbarAdded = false
        }
        toolbarView = null
        toolbarParams = null
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun pxToDp(px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }
}
