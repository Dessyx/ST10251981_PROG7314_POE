package com.example.deflate

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class BottomFilterFragment : Fragment() {
    
    private lateinit var optionDay: LinearLayout
    private lateinit var optionWeek: LinearLayout
    private lateinit var optionMonth: LinearLayout
    private lateinit var optionCustom: LinearLayout
    
    private var onFilterSelected: ((String) -> Unit)? = null
    private var onCustomDateRangeSelected: ((Long, Long) -> Unit)? = null
    
    private var startDate: Long = 0L
    private var endDate: Long = 0L
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_filter_overlay, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        setupOverlayClick(view)
    }
    
    private fun setupOverlayClick(view: View) {
        view.setOnClickListener {
            dismiss()
        }

        val bottomSheet = view.findViewById<LinearLayout>(R.id.bottomSheet)
        bottomSheet?.setOnClickListener {
        }
    }
    
    private fun initializeViews(view: View) {
        optionDay = view.findViewById(R.id.optionDay)
        optionWeek = view.findViewById(R.id.optionWeek)
        optionMonth = view.findViewById(R.id.optionMonth)
        optionCustom = view.findViewById(R.id.optionCustom)
    }
    
    private fun setupClickListeners() {
        optionDay.setOnClickListener {
            onFilterSelected?.invoke("day")
            dismiss()
        }
        
        optionWeek.setOnClickListener {
            onFilterSelected?.invoke("week")
            dismiss()
        }
        
        optionMonth.setOnClickListener {
            onFilterSelected?.invoke("month")
            dismiss()
        }
        
        optionCustom.setOnClickListener {
            showCustomDateRangeDialog()
        }
    }
    
    fun setOnFilterSelectedListener(listener: (String) -> Unit) {
        onFilterSelected = listener
    }
    
    fun setOnCustomDateRangeSelectedListener(listener: (Long, Long) -> Unit) {
        onCustomDateRangeSelected = listener
    }
    
    private fun showCustomDateRangeDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_date_range_picker, null)
        val tvStartDate = dialogView.findViewById<TextView>(R.id.tvStartDate)
        val tvEndDate = dialogView.findViewById<TextView>(R.id.tvEndDate)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<TextView>(R.id.btnApply)
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        tvStartDate.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvStartDate.text = dateFormat.format(Date(date))
            }
        }
        
        tvEndDate.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvEndDate.text = dateFormat.format(Date(date))
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnApply.setOnClickListener {
            if (startDate > 0L && endDate > 0L) {
                onCustomDateRangeSelected?.invoke(startDate, endDate)
                dialog.dismiss()
                dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                onDateSelected(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun dismiss() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }
    
    companion object {
        fun newInstance(): BottomFilterFragment {
            return BottomFilterFragment()
        }
    }
}
