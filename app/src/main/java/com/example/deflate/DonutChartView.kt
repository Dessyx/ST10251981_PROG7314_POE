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
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
    
    init {
        setupTextPaint()
    }
    
    private fun setupTextPaint() {
        textPaint.color = Color.BLACK
        textPaint.textSize = 32f
        textPaint.textAlign = Paint.Align.CENTER
    }
    
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
        val radius = Math.min(width, height) / 2 * 0.6f
        
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
            
            // Draw "No Data" text
            textPaint.textSize = 24f
            canvas.drawText("No Data", centerX, centerY, textPaint)
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

        drawLabels(canvas, centerX, centerY, radius)
    }
    
    private fun drawLabels(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val total = moodCount + weightCount + activitiesCount
        if (total == 0) return
        
        val labelRadius = radius + 80f
        val labelSize = 18f
        textPaint.textSize = labelSize
        
        var currentAngle = -90f // Start from top
        
        // Draw mood label
        if (moodCount > 0) {
            val sweepAngle = (moodCount.toFloat() / total) * 360f
            val labelAngle = currentAngle + (sweepAngle / 2f)
            drawLabelAtAngle(canvas, centerX, centerY, labelRadius, labelAngle, "Mood", moodColor)
            currentAngle += sweepAngle
        }
        
        // Draw weight label
        if (weightCount > 0) {
            val sweepAngle = (weightCount.toFloat() / total) * 360f
            val labelAngle = currentAngle + (sweepAngle / 2f)
            drawLabelAtAngle(canvas, centerX, centerY, labelRadius, labelAngle, "Weight", weightColor)
            currentAngle += sweepAngle
        }
        
        // Draw activities label
        if (activitiesCount > 0) {
            val sweepAngle = (activitiesCount.toFloat() / total) * 360f
            val labelAngle = currentAngle + (sweepAngle / 2f)
            drawLabelAtAngle(canvas, centerX, centerY, labelRadius, labelAngle, "Activities", activitiesColor)
        }
    }
    
    private fun drawLabelAtAngle(canvas: Canvas, centerX: Float, centerY: Float, labelRadius: Float, angle: Float, label: String, color: Int) {
        val radians = Math.toRadians(angle.toDouble())
        val x = centerX + (labelRadius * Math.cos(radians)).toFloat()
        val y = centerY + (labelRadius * Math.sin(radians)).toFloat()
        
        // Draw color indicator circle
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(x - 20f, y - 5f, 8f, paint)
        textPaint.color = Color.BLACK
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(label, x - 10f, y + 5f, textPaint)
    }
}

