package com.example.deflate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class StreakContainerFragment : Fragment() {
    
    private lateinit var tvStreakTitle: TextView
    private lateinit var tvStreakDays: TextView
    private lateinit var progressFill: View
    
    private var maxDays = 30
    private var currentDays = 0
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_streak_container, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
    }
    
    private fun initializeViews(view: View) {
        tvStreakTitle = view.findViewById(R.id.tvStreakTitle)
        tvStreakDays = view.findViewById(R.id.tvStreakDays)
        progressFill = view.findViewById(R.id.progressFill)
    }
    
    fun updateStreakData(days: Int, maxDays: Int = 30) {
        this.currentDays = days
        this.maxDays = maxDays
        
        // Update the progress bar
        updateProgressBar()
        
        // Update the text
        tvStreakDays.text = "$days days"
    }
    
    fun updateTitle(title: String) {
        tvStreakTitle.text = title
    }
    
    private fun updateProgressBar() {
        if (maxDays > 0) {
            val progressPercentage = (currentDays.toFloat() / maxDays.toFloat()).coerceAtMost(1f)
            val parentWidth = progressFill.parent as View
            val targetWidth = (parentWidth.width * progressPercentage).toInt()
            
            val layoutParams = progressFill.layoutParams
            layoutParams.width = targetWidth
            progressFill.layoutParams = layoutParams

            progressFill.post {
                val parent = progressFill.parent as View
                val parentWidth = parent.width
                val targetWidth = (parentWidth * progressPercentage).toInt()
                
                val layoutParams = progressFill.layoutParams
                layoutParams.width = targetWidth.coerceAtLeast(0)
                progressFill.layoutParams = layoutParams
            }
        }
    }
    
    companion object {
        fun newInstance(): StreakContainerFragment {
            return StreakContainerFragment()
        }
    }
}
