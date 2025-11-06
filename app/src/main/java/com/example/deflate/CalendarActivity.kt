package com.example.deflate

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.*
import java.text.SimpleDateFormat
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.deflate.repository.DiaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class CalendarActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var legendLayout: LinearLayout
    private lateinit var calendarView: MaterialCalendarView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var diaryRepository: DiaryRepository

    // Mood colors map
    private val moodColors = mapOf(
        "Happy" to R.color.yellow,
        "Chill" to R.color.orange,
        "Excited" to R.color.red,
        "Anxious" to R.color.purple,
        "Sad" to R.color.blue,
        "Tired" to R.color.green
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)


        enableEdgeToEdge()

        bottomNav = findViewById(R.id.bottomNav)
        setupBottomNav()

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        legendLayout = findViewById(R.id.legendLayout)
        calendarView = findViewById(R.id.calendarView)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        diaryRepository = DiaryRepository(this)

        btnBack.setOnClickListener { navigateToHome() }

        setupLegend()
        setupCalendar()
        
        // Sync entries when activity starts
        syncEntries()

        // Adjust for system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.calendar_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    /** Populate the mood legend below the calendar */
    private fun setupLegend() {
        legendLayout.removeAllViews() // clear old legend

        // Use GridLayout for 2 rows
        val gridLayout = GridLayout(this).apply {
            rowCount = 2
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            useDefaultMargins = true
        }

        val moodMap = mapOf(
            "Happy" to R.color.yellow,
            "Chill" to R.color.orange,
            "Excited" to R.color.red,
            "Anxious" to R.color.purple,
            "Tired" to R.color.green,
            "Sad" to R.color.blue,
        )

        val moodIcons = mapOf(
            "Happy" to R.drawable.mood_happy,
            "Chill" to R.drawable.mood_content,
            "Excited" to R.drawable.mood_excited,
            "Anxious" to R.drawable.mood_anxious,
            "Tired" to R.drawable.mood_tired,
            "Sad" to R.drawable.mood_sad,
        )

        moodMap.forEach { (mood, colorRes) ->
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
            }

            val moodButton = com.google.android.material.button.MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(130, 130)
                cornerRadius = 65
                backgroundTintList = ContextCompat.getColorStateList(context, colorRes)
                strokeWidth = 3
                strokeColor = ContextCompat.getColorStateList(context, android.R.color.black)
                icon = ContextCompat.getDrawable(context, moodIcons[mood] ?: 0)
                iconTint = null
                iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 0
                text = ""
            }

            val moodLabel = TextView(this).apply {
                text = mood
                textSize = 12f
                gravity = android.view.Gravity.CENTER
            }

            itemLayout.addView(moodButton)
            itemLayout.addView(moodLabel)

            // Use proper GridLayout params for child
            val params = GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.WRAP_CONTENT
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(8, 8, 8, 8)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setGravity(android.view.Gravity.CENTER)
            }

            gridLayout.addView(itemLayout, params)
        }

        legendLayout.addView(gridLayout)
    }

    /** Setup calendar arrows and listeners */
    private fun setupCalendar() {
        calendarView.setOnMonthChangedListener { _, date ->
            loadMoodsForMonth(date)
        }

        calendarView.setOnDateChangedListener { _, date, _ ->
            loadMoodForDay(date)
        }

        // Load current month initially
        calendarView.currentDate = CalendarDay.today()
        loadMoodsForMonth(CalendarDay.today())
    }

    /** Load moods for a given month and decorate each day */
    private fun loadMoodsForMonth(date: CalendarDay) {
        val user = auth.currentUser ?: return

        val startCal = Calendar.getInstance().apply {
            set(date.year, date.month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            set(date.year, date.month - 1, 1, 0, 0, 0)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startTimestamp = startCal.timeInMillis
        val endTimestamp = endCal.timeInMillis

        // Load from local database (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Get entries once (not a continuous flow for calendar)
                val entries = diaryRepository.getAllEntriesSync(user.uid)
                
                calendarView.removeDecorators() // Clear previous decorations

                entries.filter { entry ->
                    entry.timestamp >= startTimestamp && entry.timestamp <= endTimestamp
                }.forEach { entry ->
                    val mood = entry.mood
                    val timestamp = entry.timestamp

                    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                    val day = CalendarDay.from(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )

                    val color = ContextCompat.getColor(
                        this@CalendarActivity,
                        moodColors[mood] ?: android.R.color.darker_gray
                    )
                    calendarView.addDecorator(MoodBackgroundDecorator(day, color))
                }
            } catch (e: Exception) {
                Log.e("CalendarActivity", "Failed to load moods", e)
            }
        }
    }

    /** Show mood for the selected day */
    private fun loadMoodForDay(date: CalendarDay) {
        val user = auth.currentUser ?: return
        
        // Convert CalendarDay to Date
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.month - 1) // Calendar months are 0-based
            set(Calendar.DAY_OF_MONTH, date.day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        // Load from local database (offline support)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val entries = diaryRepository.getAllEntriesSync(user.uid)
                val entryForDay = entries.firstOrNull { entry ->
                    entry.timestamp >= startOfDay && entry.timestamp <= endOfDay
                }
                
                entryForDay?.let {
                    val mood = it.mood
                    val text = it.text
                    Toast.makeText(this@CalendarActivity, "$mood: $text", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this@CalendarActivity, "No entry for this day", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CalendarActivity", "Failed to load mood for day", e)
            }
        }
    }
    
    private fun syncEntries() {
        val user = auth.currentUser ?: return
        CoroutineScope(Dispatchers.IO).launch {
            diaryRepository.syncFromFirestore(user.uid)
            diaryRepository.syncUnsyncedEntries(user.uid)
        }
    }

    /** Full-day circular background shading decorator */
    class MoodBackgroundDecorator(private val day: CalendarDay, private val color: Int) :
        DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay?) = day == this.day

        override fun decorate(view: DayViewFacade?) {
            val circle = ShapeDrawable(OvalShape()).apply {
                intrinsicHeight = 80  // adjust size as needed
                intrinsicWidth = 80
                paint.color = color
            }
            view?.setBackgroundDrawable(circle)
        }
    }

    override fun onResume() {
        super.onResume()
        calendarView.currentDate = CalendarDay.today()
        loadMoodsForMonth(CalendarDay.today())
        syncEntries()
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
                    // Already on Calendar, no need to restart
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
}