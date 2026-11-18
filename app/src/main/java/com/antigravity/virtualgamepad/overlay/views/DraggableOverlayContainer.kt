package com.antigravity.virtualgamepad.overlay.views

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

class DraggableOverlayContainer(context: Context) : FrameLayout(context) {

    var isEditMode: Boolean = false
    var onDragStart: (() -> Unit)? = null
    var onDragMove: ((dx: Int, dy: Int) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    private var initialTouchRawX = 0f
    private var initialTouchRawY = 0f

    init {
        // Guarantee this container consumes all touches within its bounding box,
        // preventing the underlying game from seeing touches under the joystick!
        isClickable = true
        isFocusable = false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // When in Edit Mode, intercept touch so the container gets it for dragging
        return if (isEditMode) {
            true
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditMode) {
            // In play mode, let children (like JoystickView) handle the touch
            super.onTouchEvent(event)
            return true // Always consume so touch does not bleed through to game underneath the joystick
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchRawX = event.rawX
                initialTouchRawY = event.rawY
                onDragStart?.invoke()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchRawX).toInt()
                val dy = (event.rawY - initialTouchRawY).toInt()
                onDragMove?.invoke(dx, dy)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onDragEnd?.invoke()
                return true
            }
        }
        return true
    }
}
