package com.antigravity.virtualgamepad.overlay.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import com.antigravity.virtualgamepad.input.ButtonAction
import com.antigravity.virtualgamepad.model.ElementConfig
import kotlin.math.min

class ActionButtonView(
    context: Context,
    var config: ElementConfig,
    val action: ButtonAction,
    private val onPress: (ButtonAction) -> Unit,
    private val onRelease: (ButtonAction) -> Unit
) : View(context) {

    private var isPressedState = false

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#80151E28")
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#D000E5FF")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val textBounds = Rect()

    init {
        alpha = config.opacity
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val radius = min(w, h) / 2f
        textPaint.textSize = radius * 0.50f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) * 0.90f

        bgPaint.color = if (isPressedState) Color.parseColor("#B000E5FF") else Color.parseColor("#80151E28")
        borderPaint.color = if (isPressedState) Color.WHITE else Color.parseColor("#B000E5FF")
        textPaint.color = if (isPressedState) Color.BLACK else Color.WHITE

        canvas.drawCircle(cx, cy, radius, bgPaint)
        canvas.drawCircle(cx, cy, radius, borderPaint)

        val text = config.label
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val yPos = cy - textBounds.exactCenterY()
        canvas.drawText(text, cx, yPos, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressedState = true
                invalidate()
                onPress(action)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressedState = false
                invalidate()
                onRelease(action)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
