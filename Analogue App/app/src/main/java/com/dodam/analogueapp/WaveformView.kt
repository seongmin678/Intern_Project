package com.dodam.analogueapp // ⚠️ 본인의 패키지명으로 수정하세요

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.LinkedList

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.parseColor("#FF5252")
        strokeWidth = 5f
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val amplitudes = LinkedList<Int>()
    private val maxSpikes = 60

    fun addAmplitude(amplitude: Int) {
        amplitudes.addLast(amplitude)
        if (amplitudes.size > maxSpikes) {
            amplitudes.removeFirst()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        var currentX = width.toFloat()
        val spikeWidth = width / maxSpikes.toFloat()

        for (i in amplitudes.indices.reversed()) {
            val amplitude = amplitudes[i]
            val scaledHeight = (amplitude / 32767f) * (height * 0.8f)
            val top = centerY - (scaledHeight / 2)
            val bottom = centerY + (scaledHeight / 2)
            canvas.drawLine(currentX, top, currentX, bottom, paint)
            currentX -= spikeWidth * 1.5f
            if (currentX < 0) break
        }
    }
}