package com.example.deflate

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

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
        
        holder.entryType.text = "Diary Entry"
        holder.moodText.text = entry.mood
        holder.dateText.text = entry.datePretty
        
        // Set mood icon and background color based on mood
        val (_, colorRes) = getMoodResources(entry.mood)
        val moodColor = ContextCompat.getColor(holder.itemView.context, colorRes)
        
        // Create circular background with mood color
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(moodColor)
        holder.moodIconBackground.background = drawable
        
        // Keep the note icon black
        holder.moodIcon.setImageResource(R.drawable.ic_note)
        holder.moodIcon.clearColorFilter()
        
        // Set click listener on the entire item
        holder.itemView.setOnClickListener {
            onEntryClick(entry)
        }
        
        // Make item clickable
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
}

