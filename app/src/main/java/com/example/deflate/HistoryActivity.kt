package com.example.deflate

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class HistoryActivity : BaseActivity() {
    
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: DiaryEntryAdapter
    
    private val allEntries = mutableListOf<DiaryEntry>()
    private val filteredEntries = mutableListOf<DiaryEntry>()
    
    private var currentFilter = "all" // all, day, week, month, custom
    private var customStartDate: Long = 0L
    private var customEndDate: Long = 0L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.history_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNav()
        loadDiaryEntries()
    }
    
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        btnFilter = findViewById(R.id.btnFilter)
        recyclerView = findViewById(R.id.recyclerViewHistory)
        bottomNav = findViewById(R.id.bottomNavH)
    }
    
    private fun setupRecyclerView() {
        adapter = DiaryEntryAdapter(filteredEntries)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }
    
    private fun showFilterDialog() {
        val bottomFilterFragment = BottomFilterFragment.newInstance()
        
        bottomFilterFragment.setOnFilterSelectedListener { filter ->
            currentFilter = filter
            customStartDate = 0L
            customEndDate = 0L
            applyFilter()
        }
        
        bottomFilterFragment.setOnCustomDateRangeSelectedListener { startDate, endDate ->
            customStartDate = startDate
            customEndDate = endDate
            currentFilter = "custom"
            applyFilter()
        }
        
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, bottomFilterFragment)
            .commit()
    }
    
    private fun applyFilter() {
        filteredEntries.clear()
        
        val calendar = Calendar.getInstance()
        
        when (currentFilter) {
            "day" -> {
                // Filter entries from today
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                filteredEntries.addAll(allEntries.filter { it.timestamp >= startOfDay })
            }
            "week" -> {
                // Filter entries from the last 7 days
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.timeInMillis
                
                filteredEntries.addAll(allEntries.filter { it.timestamp >= weekAgo })
            }
            "month" -> {
                // Filter entries from the last 30 days
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val monthAgo = calendar.timeInMillis
                
                filteredEntries.addAll(allEntries.filter { it.timestamp >= monthAgo })
            }
            "custom" -> {
                // Filter entries within custom date range
                if (customStartDate > 0L && customEndDate > 0L) {
                    // Set end date to end of day
                    val endCalendar = Calendar.getInstance()
                    endCalendar.timeInMillis = customEndDate
                    endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                    endCalendar.set(Calendar.MINUTE, 59)
                    endCalendar.set(Calendar.SECOND, 59)
                    endCalendar.set(Calendar.MILLISECOND, 999)
                    val endOfDay = endCalendar.timeInMillis
                    
                    filteredEntries.addAll(allEntries.filter { 
                        it.timestamp >= customStartDate && it.timestamp <= endOfDay 
                    })
                } else {
                    filteredEntries.addAll(allEntries)
                }
            }
            else -> {
                // Show all entries
                filteredEntries.addAll(allEntries)
            }
        }
        
        // Sort by timestamp descending (newest first)
        filteredEntries.sortByDescending { it.timestamp }
        adapter.updateEntries(filteredEntries)
    }
    
    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_diary -> {
                    startActivity(Intent(this, DiaryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_insights -> {
                    startActivity(Intent(this, InsightsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadDiaryEntries() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please sign in to view history.", Toast.LENGTH_SHORT).show()
            return
        }
        
        db.collection("diaryEntries")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                allEntries.clear()
                for (document in documents) {
                    val entry = document.toObject(DiaryEntry::class.java)
                    allEntries.add(entry)
                }
                // Sort by timestamp descending (newest first)
                allEntries.sortByDescending { it.timestamp }
                // Apply current filter
                applyFilter()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading entries: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    override fun onResume() {
        super.onResume()
        loadDiaryEntries()
    }
}
