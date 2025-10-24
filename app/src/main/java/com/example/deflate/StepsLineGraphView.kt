package com.example.deflate

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class StepsLineGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val path = Path()
    private val dataPoints = mutableListOf<Pair<Float, Float>>()
    

    // Data for the graph
    private var stepsData: Map<String, Int> = emptyMap()
    
    // Dynamic Y-axis properties
    private var maxSteps = 0
    private var yAxisLabels = mutableListOf<String>()
    
    // X-axis labels (day names)
    private val xAxisLabels = mutableListOf<String>()
    
    fun updateStepsData(data: Map<String, Int>) {
        stepsData = data
        android.util.Log.d("StepsLineGraphView", "📊 Updating steps data: $data")
        calculateDataPoints()
        invalidate()
    }
    
    private fun calculateDataPoints() {
        dataPoints.clear()
        
        if (stepsData.isEmpty()) {
            android.util.Log.d("StepsLineGraphView", "⚠️ No steps data to display")
            return
        }
        
        // Calculate dynamic Y-axis based on actual data
        calculateDynamicYAxis()
        
        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 60f
        val leftPadding = 80f
        
        val graphWidth = width - leftPadding - padding
        val graphHeight = height - 2 * padding
        

        val calendar = Calendar.getInstance()
        val last7Days = mutableListOf<String>()
        xAxisLabels.clear()
        
        for (i in 0 until 7) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_MONTH, -i)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
            last7Days.add(0, date)
            xAxisLabels.add(0, dayName)
        }
        
        android.util.Log.d("StepsLineGraphView", "📅 Last 7 days: $last7Days")
        android.util.Log.d("StepsLineGraphView", "📅 Day names: $xAxisLabels")

        for (i in last7Days.indices) {
            val date = last7Days[i]
            val steps = stepsData[date] ?: 0
            
            val x = leftPadding + (i * graphWidth / 6)

            val yRatio = (steps.toFloat() / maxSteps).coerceAtMost(1f)
            val y = height - padding - (yRatio * graphHeight)
            
            dataPoints.add(Pair(x, y))
            android.util.Log.d("StepsLineGraphView", "📍 Point $i: date=$date, steps=$steps, x=$x, y=$y, yRatio=$yRatio")
        }
        
        android.util.Log.d("StepsLineGraphView", "✅ Calculated ${dataPoints.size} data points")
    }
    
    private fun calculateDynamicYAxis() {
        // Find the maximum steps value in the data
        maxSteps = if (stepsData.isNotEmpty()) {
            val maxValue = stepsData.values.maxOrNull() ?: 0
            // Add 20% padding to the max value for better visualization
            (maxValue * 1.2f).toInt()
        } else {
            1000 // Default fallback
        }
        
        // Generate Y-axis labels dynamically
        yAxisLabels.clear()
        val numLabels = 6
        for (i in 0 until numLabels) {
            val value = (maxSteps * (numLabels - 1 - i)) / (numLabels - 1)
            yAxisLabels.add(formatStepsValue(value))
        }
        
        android.util.Log.d("StepsLineGraphView", "📊 Dynamic Y-axis: maxSteps=$maxSteps, labels=$yAxisLabels")
    }
    
    private fun formatStepsValue(value: Int): String {
        return when {
            value >= 1000 -> "${value / 1000}k"
            else -> value.toString()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        

        drawYAxisLabels(canvas)
        drawGridLines(canvas)

        drawXAxisLabels(canvas)

        if (dataPoints.isNotEmpty()) {
            drawLineGraph(canvas)
            drawDataPoints(canvas)
        } else {
            // Draw "No Data" message
            drawNoDataMessage(canvas)
        }
    }
    
    private fun drawYAxisLabels(canvas: Canvas) {
        paint.color = Color.BLACK
        paint.textSize = 32f
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        
        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 60f
        val leftPadding = 80f
        
        val graphHeight = height - 2 * padding

        for (i in yAxisLabels.indices) {
            val y = padding + (i * graphHeight / (yAxisLabels.size - 1))
            val text = yAxisLabels[i]

            val textBounds = Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)
            val textY = y + (textBounds.height() / 2)
            
            canvas.drawText(text, 10f, textY, paint)
        }
    }
    
    private fun drawXAxisLabels(canvas: Canvas) {
        paint.color = Color.BLACK
        paint.textSize = 28f
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        
        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 60f
        val leftPadding = 80f
        
        val graphWidth = width - leftPadding - padding

        for (i in xAxisLabels.indices) {
            val x = leftPadding + (i * graphWidth / 6)
            val text = xAxisLabels[i]
            
            canvas.drawText(text, x, height - 20f, paint)
        }
    }
    
    private fun drawGridLines(canvas: Canvas) {
        // Minimalistic horizontal grid lines only
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        
        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 60f
        val leftPadding = 80f
        
        val graphWidth = width - leftPadding - padding
        val graphHeight = height - 2 * padding

        // Only draw horizontal grid lines for Y-axis
        for (i in yAxisLabels.indices) {
            val y = padding + (i * graphHeight / (yAxisLabels.size - 1))
            canvas.drawLine(leftPadding, y, width - padding, y, paint)
        }
    }
    
    private fun drawLineGraph(canvas: Canvas) {
        if (dataPoints.size < 2) return
        
        // Minimalistic black line
        paint.color = Color.BLACK
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        
        path.reset()
        path.moveTo(dataPoints[0].first, dataPoints[0].second)
        
        for (i in 1 until dataPoints.size) {
            path.lineTo(dataPoints[i].first, dataPoints[i].second)
        }
        
        canvas.drawPath(path, paint)
    }
    
    private fun drawDataPoints(canvas: Canvas) {
        // Minimalistic black dots
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        
        for (point in dataPoints) {
            canvas.drawCircle(point.first, point.second, 4f, paint)
        }
    }
    
    private fun drawNoDataMessage(canvas: Canvas) {
        paint.color = Color.GRAY
        paint.textSize = 40f
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        canvas.drawText("No Steps Data", width / 2, height / 2, paint)
        
        paint.textSize = 28f
        canvas.drawText("Log your steps in Activities", width / 2, height / 2 + 50, paint)
    }
}