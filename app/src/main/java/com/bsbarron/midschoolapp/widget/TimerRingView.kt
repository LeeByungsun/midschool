package com.bsbarron.midschoolapp.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.bsbarron.midschoolapp.R

class TimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.divider_soft)
        strokeWidth = 18f
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.brand_blue)
        strokeWidth = 18f
    }

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.brand_navy)
        textAlign = Paint.Align.CENTER
        textSize = 54f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = 32f
    }

    private val arcBounds = RectF()
    private var progressFraction: Float = 1f
    private var centerTimeText: String = "40:00"
    private var centerLabelText: String = "남은 시간"

    fun setTimerState(progressFraction: Float, timeText: String, labelText: String) {
        this.progressFraction = progressFraction.coerceIn(0f, 1f)
        centerTimeText = timeText
        centerLabelText = labelText
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val inset = 24f
        arcBounds.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(arcBounds, -90f, 360f, false, trackPaint)
        canvas.drawArc(arcBounds, -90f, 360f * progressFraction, false, progressPaint)

        val centerX = width / 2f
        val centerY = height / 2f
        canvas.drawText(centerTimeText, centerX, centerY + 6f, timePaint)
        canvas.drawText(centerLabelText, centerX, centerY + 52f, labelPaint)
    }
}
