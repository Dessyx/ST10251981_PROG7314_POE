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
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var legendLayout: LinearLayout
    private lateinit var calendarView: MaterialCalendarView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bottomNav: BottomNavigationView

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

        bottomNav = findViewById(R.id.bottomNav)
        setupBottomNav()

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        legendLayout = findViewById(R.id.legendLayout)
        calendarView = findViewById(R.id.calendarView)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnBack.setOnClickListener { navigateToHome() }

        setupLegend()
        setupCalendar()

        // Adjust for system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.calendar_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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

        // Make sure legendLayout centers all items
        legendLayout.orientation = LinearLayout.HORIZONTAL
        legendLayout.gravity = android.view.Gravity.CENTER
        legendLayout.setPadding(0, 0, 0, 0)

        // Map of moods to colors
        val moodMap = mapOf(
            "Happy" to R.color.yellow,
            "Chill" to R.color.orange,
            "Excited" to R.color.red,
            "Anxious" to R.color.purple,
            "Tired" to R.color.green,
            "Sad" to R.color.blue,
        )

        // Map of moods to icons
        val moodIcons = mapOf(
            "Happy" to R.drawable.mood_happy,
            "Chill" to R.drawable.mood_content,
            "Excited" to R.drawable.mood_excited,
            "Anxious" to R.drawable.mood_anxious,
            "Tired" to R.drawable.mood_tired,
            "Sad" to R.drawable.mood_sad,
        )

        moodMap.forEach { (mood, colorRes) ->
            // Container for button + label
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(8, 0, 8, 0)
            }

            // Mood button (slightly smaller + centered)
            val moodButton = com.google.android.material.button.MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(130, 130).apply {
                    setMargins(8, 0, 8, 0)
                }
                cornerRadius = 65
                backgroundTintList = ContextCompat.getColorStateList(context, colorRes)
                strokeWidth = 3
                strokeColor = ContextCompat.getColorStateList(context, android.R.color.black)
                icon = ContextCompat.getDrawable(context, moodIcons[mood] ?: 0)
                iconTint = null
                iconGravity =
                    com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
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

            legendLayout.addView(itemLayout)
        }
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
            set(Calendar.MILLISECOND, 999)
        }

        db.collection("diaryEntries")
            .whereEqualTo("userId", user.uid)
            .whereGreaterThanOrEqualTo("timestamp", startCal.timeInMillis)
            .whereLessThanOrEqualTo("timestamp", endCal.timeInMillis)
            .get()
            .addOnSuccessListener { documents ->
                calendarView.removeDecorators() // Clear previous decorations

                documents.forEach { doc ->
                    val mood = doc.getString("mood") ?: return@forEach
                    val timestamp = doc.getLong("timestamp") ?: return@forEach

                    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                    val day = CalendarDay.from(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )

                    val color = ContextCompat.getColor(
                        this,
                        moodColors[mood] ?: android.R.color.darker_gray
                    )
                    calendarView.addDecorator(MoodBackgroundDecorator(day, color))
                }
            }
            .addOnFailureListener { e ->
                Log.e("CalendarActivity", "Failed to load moods", e)
            }
    }

    /** Show mood for the selected day */
    private fun loadMoodForDay(date: CalendarDay) {
        val user = auth.currentUser ?: return
        val datePretty = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date.date)

        db.collection("diaryEntries")
            .whereEqualTo("userId", user.uid)
            .whereEqualTo("datePretty", datePretty)
            .get()
            .addOnSuccessListener { query ->
                val doc = query.documents.firstOrNull()
                doc?.let {
                    val mood = it.getString("mood") ?: "No mood"
                    val text = it.getString("text") ?: ""
                    Toast.makeText(this, "$mood: $text", Toast.LENGTH_SHORT).show()
                }
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