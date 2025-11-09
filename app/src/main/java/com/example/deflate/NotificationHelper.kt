package com.example.deflate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


object NotificationHelper {
    
    // Notification Channels
    private const val CHANNEL_DIARY_REMINDERS = "diary_reminders"
    private const val CHANNEL_STREAK_UPDATES = "streak_updates"
    private const val CHANNEL_CRISIS_SUPPORT = "crisis_support"
    
    // Notification IDs
    const val NOTIFICATION_ID_DIARY_REMINDER = 1001
    const val NOTIFICATION_ID_STREAK = 1002
    const val NOTIFICATION_ID_CRISIS = 1003
    

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Diary reminders channel
            val diaryChannel = NotificationChannel(
                CHANNEL_DIARY_REMINDERS,
                "Diary Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to log your feelings"
            }
            
            // Streak updates channel
            val streakChannel = NotificationChannel(
                CHANNEL_STREAK_UPDATES,
                "Streak Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about your progress and milestones"
            }
            
            // Crisis support channel
            val crisisChannel = NotificationChannel(
                CHANNEL_CRISIS_SUPPORT,
                "Crisis Support",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important support and helpline information"
            }
            
            notificationManager.createNotificationChannel(diaryChannel)
            notificationManager.createNotificationChannel(streakChannel)
            notificationManager.createNotificationChannel(crisisChannel)
        }
    }
    
  // Daily diary notif
    fun showDiaryReminderNotification(context: Context) {
        val intent = Intent(context, DiaryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_DIARY_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to Check In")
            .setContentText("How are you feeling today? Take a moment to write about your feelings.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DIARY_REMINDER, notification)
    }
    
    
     // Show streak milestone notification    
    fun showStreakNotification(context: Context, streakDays: Int) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val (title, message) = getStreakMessage(streakDays)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_STREAK_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_STREAK, notification)
    }
    

    fun showCrisisNotification(context: Context) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val message = """
            We've noticed you've been feeling down lately. You're not alone, and help is available.
            
            Crisis Helplines:
            🇺🇸 National Suicide Prevention Lifeline: 988
            🇬🇧 Samaritans: 116 123
            🇿🇦 SADAG: 0800 567 567
            🌍 International: findahelpline.com
            
            Please reach out if you need support.
        """.trimIndent()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_CRISIS_SUPPORT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Support is Available")
            .setContentText("We've noticed you've been feeling down. Help is here for you.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CRISIS, notification)
    }
    

    private fun getStreakMessage(streakDays: Int): Pair<String, String> {
        return when {
            streakDays == 1 -> Pair(
                "Great Start! 🌱",
                "You've started your journey! Keep logging your feelings daily to build your streak."
            )
            streakDays == 3 -> Pair(
                "3 Day Streak! 🔥",
                "Amazing! You're building a healthy habit. Three days of consistent self-reflection!"
            )
            streakDays == 7 -> Pair(
                "Week Streak! 🎉",
                "Congratulations! You've been checking in with yourself for a whole week. That's dedication!"
            )
            streakDays == 14 -> Pair(
                "2 Week Streak! ⭐",
                "Incredible! Two weeks of consistent self-care. You're doing great!"
            )
            streakDays == 30 -> Pair(
                "30 Day Milestone! 🏆",
                "Outstanding achievement! A full month of reflection and growth. You should be proud!"
            )
            streakDays == 60 -> Pair(
                "60 Day Milestone! 💎",
                "Two months of dedication! You've made self-reflection a true habit."
            )
            streakDays == 90 -> Pair(
                "90 Day Milestone! 👑",
                "Three months strong! You're a self-care champion. Keep up the amazing work!"
            )
            streakDays == 365 -> Pair(
                "1 YEAR STREAK! 🎊",
                "LEGENDARY! One full year of daily reflection. You're an inspiration!"
            )
            streakDays % 10 == 0 && streakDays > 0 -> Pair(
                "$streakDays Day Streak! 🔥",
                "You're on fire! $streakDays days of consistent self-care. Keep it going!"
            )
            else -> Pair(
                "$streakDays Day Streak! ✨",
                "Keep up the great work! You're building a powerful habit."
            )
        }
    }
}

