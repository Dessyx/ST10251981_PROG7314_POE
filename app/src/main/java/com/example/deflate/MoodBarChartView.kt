package com.example.deflate

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class MoodBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val moodOrder = listOf("Happy", "Excited", "Content", "Anxious", "Tired", "Sad")
    
    private val moodColors = mapOf(
        "Happy" to Color.parseColor("#FFFBD5"),      
        "Excited" to Color.parseColor("#FBDEE0"),    
        "Content" to Color.parseColor("#FFE1D7"),    
        "Anxious" to Color.parseColor("#F8E1FD"),    
        "Tired" to Color.parseColor("#DFFAD4"),      
        "Sad" to Color.parseColor("#D3F5FF")      
    )
    
    private var moodData = mutableMapOf<String, Int>()
    private var maxValue = 0
    
    init {
        setupPaints()
    }
    
    private fun setupPaints() {
        // Bar paint
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 4f
        
        // Text paint
        textPaint.color = Color.BLACK
        textPaint.textSize = 32f
        textPaint.textAlign = Paint.Align.CENTER
        
        // Grid paint
        gridPaint.color = Color.LTGRAY
        gridPaint.strokeWidth = 2f
        gridPaint.style = Paint.Style.STROKE
    }
    
    fun updateMoodData(data: Map<String, Int>) {
        android.util.Log.d("MoodBarChartView", "📊 Received mood data: $data")
        moodData.clear()
        moodData.putAll(data)
        maxValue = if (data.isNotEmpty()) data.values.maxOrNull() ?: 1 else 1
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (moodData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        
        val width = width.toFloat()
        val height = height.toFloat()
        val margin = 60f
        val chartWidth = width - 2 * margin
        val chartHeight = height - 2 * margin
        

        canvas.drawColor(Color.WHITE)
        
        textPaint.textSize = 28f
        canvas.save()
        canvas.rotate(-90f, margin / 4, height / 2)
        canvas.drawText("Count", margin / 4, height / 2, textPaint)
        canvas.restore()
        

        val barWidth = chartWidth / moodData.size - 20f
        val barSpacing = 20f
        

        var xOffset = margin + barSpacing / 2

         val maxGridValue = maxValue.coerceAtLeast(5)
         moodOrder.forEach { mood ->
             val count = moodData[mood] ?: 0
             val barHeight = (count.toFloat() / maxGridValue) * chartHeight
             paint.color = moodColors[mood] ?: Color.GRAY
             val barLeft = xOffset
             val barTop = margin + chartHeight - barHeight
             val barRight = xOffset + barWidth
             val barBottom = margin + chartHeight
             val cornerRadius = 70f 
             val rect = RectF(barLeft, barTop, barRight, barBottom)
             canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            
            // Draw count on top of bar
            textPaint.textSize = 18f
            textPaint.color = Color.BLACK
            canvas.drawText(
                count.toString(),
                xOffset + barWidth / 2 + 15f, 
                barTop - 10f,
                textPaint
            )
            
            // Draw mood label at bottom
            textPaint.textSize = 20f
            canvas.drawText(
                mood,
                xOffset + barWidth / 2,
                margin + chartHeight + 30f,
                textPaint
            )
            
            xOffset += barWidth + barSpacing
        }

        val gridLineCount = 5
        
        for (i in 0..gridLineCount) {
            val y = margin + (i * chartHeight / gridLineCount)
            val value = (maxGridValue * (gridLineCount - i) / gridLineCount)
            
            // Draw grid line
            canvas.drawLine(margin, y, margin + chartWidth, y, gridPaint)
            
            // Draw Y-axis value
            textPaint.textSize = 20f
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                value.toString(),
                margin - 10f,
                y + 8f,
                textPaint
            )
        }
        
        // Draw X-axis line
        canvas.drawLine(margin, margin + chartHeight, margin + chartWidth, margin + chartHeight, gridPaint)
    }
    
    private fun drawEmptyState(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        
        textPaint.textSize = 40f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.GRAY
        canvas.drawText("No mood data available", width / 2f, height / 2f, textPaint)
    }
}
