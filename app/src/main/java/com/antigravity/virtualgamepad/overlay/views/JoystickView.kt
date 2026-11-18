package com.antigravity.virtualgamepad.overlay.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.antigravity.virtualgamepad.model.ElementConfig
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class JoystickView(
    context: Context,
    var config: ElementConfig,
    var deadzone: Float = 0.08f,
    private val onMove: (normX: Float, normY: Float, magnitude: Float) -> Unit,
    private val onRelease: () -> Unit
) : View(context) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#5000E5FF")
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#D000E5FF")
    }

    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.WHITE
    }

    private val deadzonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#30FFFFFF")
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f
    private var deadzoneRadius = 0f

    private var knobX = 0f
    private var knobY = 0f

    private var returnAnimator: ValueAnimator? = null

    init {
        alpha = config.opacity
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(centerX, centerY) * 0.90f
        knobRadius = baseRadius * 0.38f
        deadzoneRadius = baseRadius * deadzone

        knobX = centerX
        knobY = centerY

        basePaint.shader = RadialGradient(
            centerX, centerY, baseRadius,
            intArrayOf(Color.parseColor("#35003344"), Color.parseColor("#60001822")),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. Base circle
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, ringPaint)

        // 2. Deadzone
        canvas.drawCircle(centerX, centerY, deadzoneRadius, deadzonePaint)

        // 3. Thumbstick knob
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                returnAnimator?.cancel()
                updateKnobPosition(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateKnobPosition(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handleRelease()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                // On some Samsung / One UI 6 builds, simultaneous synthetic gesture injection
                // can trigger a transient ACTION_CANCEL. Do not snap back to center immediately;
                // the stick only releases on a true ACTION_UP or loss of window focus.
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            handleRelease()
        }
    }

    private fun updateKnobPosition(touchX: Float, touchY: Float) {
        val dx = touchX - centerX
        val dy = touchY - centerY
        val distance = sqrt(dx * dx + dy * dy)
        val maxDistance = baseRadius - knobRadius

        val clampedDistance = min(distance, maxDistance)
        val angle = atan2(dy, dx)

        knobX = centerX + cos(angle) * clampedDistance
        knobY = centerY + sin(angle) * clampedDistance

        val rawMag = clampedDistance / maxDistance
        if (rawMag < deadzone) {
            onMove(0f, 0f, 0f)
        } else {
            val scaledMag = (rawMag - deadzone) / (1f - deadzone)
            val normX = cos(angle) * scaledMag
            val normY = sin(angle) * scaledMag
            onMove(normX, normY, scaledMag)
        }

        invalidate()
    }

    private fun handleRelease() {
        onRelease()

        val startX = knobX
        val startY = knobY
        returnAnimator?.cancel()
        returnAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 160
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                knobX = startX + (centerX - startX) * fraction
                knobY = startY + (centerY - startY) * fraction
                invalidate()
            }
            start()
        }
    }
}
