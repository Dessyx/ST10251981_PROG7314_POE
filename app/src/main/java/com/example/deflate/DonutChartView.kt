package com.example.deflate

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    
    // Data
    private var moodCount = 0
    private var weightCount = 0
    private var activitiesCount = 0
    
    // Colors
    private val moodColor = Color.parseColor("#FFB6C1") // Light pink
    private val weightColor = Color.parseColor("#87CEEB") // Sky blue
    private val activitiesColor = Color.parseColor("#98FB98") // Pale green
    private val noDataColor = Color.parseColor("#E0E0E0") // Light gray
    
    fun updateData(mood: Int, weight: Int, activities: Int) {
        moodCount = mood
        weightCount = weight
        activitiesCount = activities
        android.util.Log.d("DonutChartView", "📊 Updated data: mood=$mood, weight=$weight, activities=$activities")
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val radius = Math.min(width, height) / 2 * 0.7f
        
        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        val total = moodCount + weightCount + activitiesCount
        
        if (total == 0) {
            // Draw empty donut
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 40f
            paint.color = noDataColor
            canvas.drawCircle(centerX, centerY, radius - 20f, paint)
            return
        }
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 40f
        
        var startAngle = -90f // Start from top
        
        // Draw mood section
        if (moodCount > 0) {
            val sweepAngle = (moodCount.toFloat() / total) * 360f
            paint.color = moodColor
            canvas.drawArc(rect, startAngle, sweepAngle, false, paint)
            startAngle += sweepAngle
        }
        
        // Draw weight section
        if (weightCount > 0) {
            val sweepAngle = (weightCount.toFloat() / total) * 360f
            paint.color = weightColor
            canvas.drawArc(rect, startAngle, sweepAngle, false, paint)
            startAngle += sweepAngle
        }
        
        // Draw activities section
        if (activitiesCount > 0) {
            val sweepAngle = (activitiesCount.toFloat() / total) * 360f
            paint.color = activitiesColor
            canvas.drawArc(rect, startAngle, sweepAngle, false, paint)
        }
    }
}

