package com.lbs.schoolhelper.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.lbs.schoolhelper.R

class TimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    // 상품 레퍼런스처럼 연속된 호 대신 일정한 간격의 방사형 세그먼트를 사용한다.
    // 타이머 동작과 중앙 텍스트는 그대로 두고 진행률 표현만 변경한다.
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.divider_soft)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.brand_blue)
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

    private val density = resources.displayMetrics.density
    private val segmentCount = 60
    private val segmentStep = 360f / segmentCount
    private var progressFraction: Float = 1f
    private var centerTimeText: String = "40:00"
    private var centerLabelText: String = context.getString(R.string.home_timer_remaining)

    fun setTimerState(progressFraction: Float, timeText: String, labelText: String) {
        // 진행률은 0~1 범위로 고정해 잘못된 값이 들어와도 드로잉이 깨지지 않게 한다.
        this.progressFraction = progressFraction.coerceIn(0f, 1f)
        centerTimeText = timeText
        centerLabelText = labelText
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 2f - dp(12f)

        // 한 줄의 굵은 눈금으로 구성해 진행 상태를 단순하고 선명하게 보여준다.
        drawSegmentRing(
            canvas = canvas,
            centerX = centerX,
            centerY = centerY,
            outerRadius = radius,
            innerRadius = radius - dp(22f),
            strokeWidth = dp(6f)
        )

        canvas.drawText(centerTimeText, centerX, centerY + 6f, timePaint)
        canvas.drawText(centerLabelText, centerX, centerY + 52f, labelPaint)
    }

    private fun drawSegmentRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        innerRadius: Float,
        strokeWidth: Float
    ) {
        trackPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
        val activeSegments = progressFraction * segmentCount

        // 먼저 전체 트랙을 그리고, 진행된 세그먼트만 같은 위치에 덧그린다.
        for (index in 0 until segmentCount) {
            drawRadialSegment(canvas, centerX, centerY, outerRadius, innerRadius, index, trackPaint)
            if (index + 0.5f < activeSegments) {
                drawRadialSegment(canvas, centerX, centerY, outerRadius, innerRadius, index, progressPaint)
            }
        }
    }

    private fun drawRadialSegment(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        innerRadius: Float,
        index: Int,
        paint: Paint
    ) {
        val angle = Math.toRadians((-90f + index * segmentStep).toDouble())
        val startX = centerX + (kotlin.math.cos(angle) * innerRadius).toFloat()
        val startY = centerY + (kotlin.math.sin(angle) * innerRadius).toFloat()
        val endX = centerX + (kotlin.math.cos(angle) * outerRadius).toFloat()
        val endY = centerY + (kotlin.math.sin(angle) * outerRadius).toFloat()
        canvas.drawLine(startX, startY, endX, endY, paint)
    }

    private fun dp(value: Float): Float = value * density
}
