package com.dodam.analogueapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class HorizontalRecorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waveformPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f
        val centerX = width / 2f

        // 파동 그리기 영역 설정
        val maxWaveWidth = width * 0.5f
        val lineSpacing = 25f
        var y = 50f

        while (y < height - 50f) {
            // 실시간 파동 애니메이션 효과
            val waveFactor = Math.abs(Math.sin(y * 0.02 + System.currentTimeMillis() * 0.005) * Math.sin(y * 0.04)).toFloat()
            val currentLineWidth = maxWaveWidth * waveFactor

            if (currentLineWidth > 10f) {
                canvas.drawLine(centerX - currentLineWidth / 2, y, centerX + currentLineWidth / 2, y, waveformPaint)
            }
            y += lineSpacing
        }
        invalidate() // 지속적인 갱신
    }

    fun setTime(newTime: String) {
        // 타이머 텍스트 업데이트 로직 (필요 시)
    }
}