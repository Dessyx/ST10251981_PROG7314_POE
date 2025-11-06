package com.example.deflate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.example.deflate.repository.DiaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.util.*

class HistoryActivity : BaseActivity() {
    
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var diaryRepository: DiaryRepository
    private lateinit var adapter: DiaryEntryAdapter
    
    private val allEntries = mutableListOf<DiaryEntry>()
    private val filteredEntries = mutableListOf<DiaryEntry>()
    
    private var currentFilter = "all" // all, day, week, month, custom
    private var customStartDate: Long = 0L
    private var customEndDate: Long = 0L
    private var flowJob: Job? = null
    
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
        diaryRepository = DiaryRepository(this)
        
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNav()
        loadDiaryEntries()
        
        // Sync when activity starts
        syncEntries()
    }
    
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        btnFilter = findViewById(R.id.btnFilter)
        recyclerView = findViewById(R.id.recyclerViewHistory)
        bottomNav = findViewById(R.id.bottomNavH)
    }
    
    private fun setupRecyclerView() {
        adapter = DiaryEntryAdapter(filteredEntries) { entry ->
            showDiaryEntryDialog(entry)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun showDiaryEntryDialog(entry: DiaryEntry) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_diary_entry, null)
        
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
        val tvMood = dialogView.findViewById<TextView>(R.id.tvDialogMood)
        val tvText = dialogView.findViewById<TextView>(R.id.tvDialogText)
        val moodIconBackground = dialogView.findViewById<android.view.View>(R.id.moodIconBackgroundDialog)
        
        tvDate.text = entry.datePretty
        tvMood.text = entry.mood
        tvText.text = entry.text
        
        // Set mood color
        val (_, colorRes) = getMoodResources(entry.mood)
        val moodColor = ContextCompat.getColor(this, colorRes)
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
        drawable.setColor(moodColor)
        moodIconBackground.background = drawable
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()
        
        dialog.show()
    }
    
    private fun getMoodResources(mood: String): Pair<Int, Int> {
        return when (mood.lowercase()) {
            "happy" -> Pair(R.drawable.mood_happy, R.color.yellow)
            "sad" -> Pair(R.drawable.mood_sad, R.color.blue)
            "anxious" -> Pair(R.drawable.mood_anxious, R.color.purple)
            "tired" -> Pair(R.drawable.mood_tired, R.color.green)
            "excited" -> Pair(R.drawable.mood_excited, R.color.red)
            "content" -> Pair(R.drawable.mood_content, R.color.orange)
            else -> Pair(R.drawable.mood_happy, R.color.yellow)
        }
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
        
        // Cancel any existing flow collection
        flowJob?.cancel()
        
        // Observe entries from local database
        flowJob = CoroutineScope(Dispatchers.Main).launch {
            diaryRepository.getAllEntries(user.uid).collect { entries ->
                // Remove duplicates based on timestamp and text
                val uniqueEntries = entries.distinctBy { entry ->
                    "${entry.timestamp}_${entry.text}"
                }
                
                allEntries.clear()
                allEntries.addAll(uniqueEntries)
                allEntries.sortByDescending { it.timestamp }
                applyFilter()
            }
        }
    }
    
    private fun syncEntries() {
        val user = auth.currentUser ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            // Remove any existing duplicates first
            diaryRepository.removeDuplicateEntries(user.uid)
            // Sync from Firestore to local
            diaryRepository.syncFromFirestore(user.uid)
            // Sync unsynced local entries to Firestore
            diaryRepository.syncUnsyncedEntries(user.uid)
        }
    }
    
    override fun onResume() {
        super.onResume()
        syncEntries()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        flowJob?.cancel()
    }
}
