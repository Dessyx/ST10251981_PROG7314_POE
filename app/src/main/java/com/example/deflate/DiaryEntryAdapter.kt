package com.example.deflate

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Date
import java.util.Locale

class DiaryEntryAdapter(
    private var entries: List<DiaryEntry>,
    private val onEntryClick: (DiaryEntry) -> Unit = {}
) : RecyclerView.Adapter<DiaryEntryAdapter.DiaryEntryViewHolder>() {

    inner class DiaryEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val moodIcon: ImageView = itemView.findViewById(R.id.moodIcon)
        val moodIconBackground: View = itemView.findViewById(R.id.moodIconBackground)
        val entryType: TextView = itemView.findViewById(R.id.entryType)
        val moodText: TextView = itemView.findViewById(R.id.moodText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_entry, parent, false)
        return DiaryEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryEntryViewHolder, position: Int) {
        val entry = entries[position]

        // Localized "Diary Entry" title
        val ctx = holder.itemView.context
        holder.entryType.text = ctx.getString(R.string.diary_entry)

        // Localized mood label for display (maps internal mood id -> localized string)
        val moodLabel = getLocalizedMoodLabel(ctx, entry.mood)
        holder.moodText.text = moodLabel

        // Date: use entry.datePretty if present, otherwise format a Date using Locale
        holder.dateText.text = if (!entry.datePretty.isNullOrBlank()) {
            entry.datePretty
        } else {
            // if entry has a timestamp (millis) field, use it; otherwise fallback to current date
            val dateMillis = entry.timestamp ?: System.currentTimeMillis()
            android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEE, MMM d")
            val formatted = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
                .format(Date(dateMillis))
            formatted
        }

        // Set mood icon and background color based on mood
        val (_, colorRes) = getMoodResources(entry.mood)
        val moodColor = ContextCompat.getColor(ctx, colorRes)

        // Create circular background with mood color
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(moodColor)
        holder.moodIconBackground.background = drawable

        // Keep the note icon (or mood icon) and add accessibility contentDescription
        holder.moodIcon.setImageResource(R.drawable.ic_note)
        holder.moodIcon.clearColorFilter()
        // Accessibility: set content description to the localized mood label
        holder.moodIcon.contentDescription = moodLabel

        // Click handling
        holder.itemView.setOnClickListener { onEntryClick(entry) }
        holder.itemView.isClickable = true
        holder.itemView.isFocusable = true
    }


    override fun getItemCount(): Int = entries.size

    fun updateEntries(newEntries: List<DiaryEntry>) {
        entries = newEntries
        notifyDataSetChanged()
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
    private fun getLocalizedMoodLabel(ctx: Context, mood: String?): String {
        if (mood.isNullOrBlank()) return ctx.getString(R.string.mood_unknown_label)

        return when (mood.lowercase(Locale.getDefault())) {
            "happy" -> ctx.getString(R.string.mood_happy_label)
            "sad" -> ctx.getString(R.string.mood_sad_label)
            "anxious" -> ctx.getString(R.string.mood_anxious_label)
            "tired" -> ctx.getString(R.string.mood_tired_label)
            "excited" -> ctx.getString(R.string.mood_excited_label)
            "content" -> ctx.getString(R.string.mood_content_label)
            else -> mood
        }
    }

}

