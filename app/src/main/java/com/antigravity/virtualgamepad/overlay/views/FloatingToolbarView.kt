package com.antigravity.virtualgamepad.overlay.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.antigravity.virtualgamepad.model.InputMode

class FloatingToolbarView(
    context: Context,
    private var isEditMode: Boolean,
    private var currentMode: InputMode,
    private val onToggleEditMode: (Boolean) -> Unit,
    private val onToggleInputMode: () -> Unit,
    private val onCycleSize: () -> Unit,
    private val onToggleHide: (Boolean) -> Unit,
    private val onClose: () -> Unit
) : LinearLayout(context) {

    private val expandedLayout: LinearLayout
    private val collapsedView: TextView

    private val dragHandle: TextView
    private val lockButton: TextView
    private val modeButton: TextView
    private val sizeButton: TextView
    private val hideButton: TextView
    private val closeButton: TextView

    private var isHiddenState = false

    var onToolbarDragStart: (() -> Unit)? = null
    var onToolbarDragMove: ((dx: Int, dy: Int) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        // 1. Collapsed Pill View (For viewing cutscenes with full screen clear)
        collapsedView = TextView(context).apply {
            text = "🎮 SHOW"
            textSize = 12f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#E0101820"))
                setStroke(dp(1), Color.parseColor("#00E5FF"))
            }
            background = bg
            visibility = View.GONE

            var downX = 0f
            var downY = 0f
            var isDragging = false

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        isDragging = false
                        onToolbarDragStart?.invoke()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isDragging = true
                            onToolbarDragMove?.invoke(dx, dy)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            setHidden(false)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        addView(collapsedView)

        // 2. Expanded Toolbar Layout
        expandedLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#E0151E28"))
                setStroke(dp(1), Color.parseColor("#8000E5FF"))
            }
            background = bg
        }
        addView(expandedLayout)

        // Dedicated Drag Handle (⠿)
        dragHandle = TextView(context).apply {
            text = "⠿"
            textSize = 14f
            setTextColor(Color.parseColor("#90FFFFFF"))
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(4), dp(7), dp(4))
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#25FFFFFF"))
            }
            background = bg
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(2)
                marginEnd = dp(4)
            }
            layoutParams = params

            var downX = 0f
            var downY = 0f

            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        onToolbarDragStart?.invoke()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        onToolbarDragMove?.invoke(dx, dy)
                        true
                    }
                    else -> true
                }
            }
        }
        expandedLayout.addView(dragHandle)

        // Lock / Edit Button
        lockButton = createPillButton(
            text = if (isEditMode) "🔓 EDIT" else "🔒 PLAY",
            bgColor = if (isEditMode) Color.parseColor("#FFD700") else Color.parseColor("#00E5FF"),
            textColor = Color.BLACK
        ) { btn ->
            isEditMode = !isEditMode
            btn.text = if (isEditMode) "🔓 EDIT" else "🔒 PLAY"
            (btn.background as? GradientDrawable)?.setColor(
                if (isEditMode) Color.parseColor("#FFD700") else Color.parseColor("#00E5FF")
            )
            onToggleEditMode(isEditMode)
        }
        expandedLayout.addView(lockButton)

        // Hide / Cutscene Mode Button (Hides overlay so user can see cutscenes)
        hideButton = createPillButton(
            text = "👁️ HIDE",
            bgColor = Color.parseColor("#2A3B4C"),
            textColor = Color.parseColor("#FFD700")
        ) {
            setHidden(true)
        }
        expandedLayout.addView(hideButton)

        // Size Adjust Button
        sizeButton = createPillButton(
            text = "SIZE",
            bgColor = Color.parseColor("#2A3B4C"),
            textColor = Color.WHITE
        ) {
            onCycleSize()
        }
        expandedLayout.addView(sizeButton)

        // Mode Switcher (TOUCH / PAD)
        modeButton = createPillButton(
            text = if (currentMode == InputMode.GAMEPAD) "PAD" else "TOUCH",
            bgColor = Color.parseColor("#2A3B4C"),
            textColor = Color.parseColor("#00E5FF")
        ) {
            onToggleInputMode()
        }
        expandedLayout.addView(modeButton)

        // Close Button
        closeButton = createPillButton(
            text = "✕",
            bgColor = Color.parseColor("#441A1A"),
            textColor = Color.parseColor("#FF5252")
        ) {
            onClose()
        }
        expandedLayout.addView(closeButton)
    }

    fun setHidden(hidden: Boolean) {
        isHiddenState = hidden
        if (hidden) {
            expandedLayout.visibility = View.GONE
            collapsedView.visibility = View.VISIBLE
        } else {
            collapsedView.visibility = View.GONE
            expandedLayout.visibility = View.VISIBLE
        }
        onToggleHide(hidden)
    }

    fun updateInputMode(mode: InputMode) {
        currentMode = mode
        modeButton.text = if (mode == InputMode.GAMEPAD) "PAD" else "TOUCH"
    }

    private fun createPillButton(
        text: String,
        bgColor: Int,
        textColor: Int,
        onClick: (TextView) -> Unit
    ): TextView {
        val tv = TextView(context).apply {
            this.text = text
            this.textSize = 11f
            this.setTextColor(textColor)
            this.gravity = Gravity.CENTER
            this.setPadding(dp(7), dp(4), dp(7), dp(4))

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(bgColor)
            }
            background = bg

            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            layoutParams = params

            setOnClickListener { onClick(this) }
        }
        return tv
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
