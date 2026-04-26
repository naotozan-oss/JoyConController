package com.joyconcontroller.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * Lightweight custom view that renders the cursor overlay.
 *
 * Design:
 *  - Semi-transparent circle with a bright centre dot
 *  - Tap animation: cursor scales up briefly then returns to normal
 *  - Draw cost: two paint calls per frame → very cheap
 */
class CursorView(context: Context) : View(context) {

    // Cursor position in screen coordinates
    private val position = PointF(0f, 0f)

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 100, 200, 255)   // translucent blue
        style = Paint.Style.FILL
    }

    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 30, 140, 255)    // solid blue centre
        style = Paint.Style.FILL
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)   // white ring
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Base radii
    private val baseOuterRadius = 22f
    private val baseInnerRadius = 8f

    // Animated scale (1.0 = normal, > 1.0 = tap animation)
    private var currentScale = 1.0f
    private var tapAnimator: ValueAnimator? = null

    // ─── Public API ───────────────────────────────────────────────────────────

    fun updatePosition(x: Float, y: Float) {
        position.set(x, y)
        invalidate()
    }

    /** Call when a tap gesture is dispatched to show visual feedback */
    fun animateTap() {
        tapAnimator?.cancel()
        tapAnimator = ValueAnimator.ofFloat(1.0f, 1.8f, 1.0f).apply {
            duration = 200L
            interpolator = OvershootInterpolator(3f)
            addUpdateListener { anim ->
                currentScale = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = position.x
        val cy = position.y
        val outerR = baseOuterRadius * currentScale
        val innerR = baseInnerRadius * currentScale

        canvas.drawCircle(cx, cy, outerR, outerPaint)
        canvas.drawCircle(cx, cy, outerR, ringPaint)
        canvas.drawCircle(cx, cy, innerR, innerPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tapAnimator?.cancel()
    }
}
