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

    // Notification Channels (IDs remain constants)
    private const val CHANNEL_DIARY_REMINDERS = "diary_reminders"
    private const val CHANNEL_STREAK_UPDATES = "streak_updates"
    private const val CHANNEL_CRISIS_SUPPORT = "crisis_support"

    // Notification IDs
    const val NOTIFICATION_ID_DIARY_REMINDER = 1001
    const val NOTIFICATION_ID_STREAK = 1002
    const val NOTIFICATION_ID_CRISIS = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Diary reminders channel
            val diaryChannel = NotificationChannel(
                CHANNEL_DIARY_REMINDERS,
                context.getString(R.string.channel_diary_title),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_diary_description)
            }

            // Streak updates channel
            val streakChannel = NotificationChannel(
                CHANNEL_STREAK_UPDATES,
                context.getString(R.string.channel_streak_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_streak_description)
            }

            // Crisis support channel
            val crisisChannel = NotificationChannel(
                CHANNEL_CRISIS_SUPPORT,
                context.getString(R.string.channel_crisis_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_crisis_description)
            }

            notificationManager.createNotificationChannel(diaryChannel)
            notificationManager.createNotificationChannel(streakChannel)
            notificationManager.createNotificationChannel(crisisChannel)
        }
    }

    // Daily diary notification
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
            .setContentTitle(context.getString(R.string.diary_notif_title))
            .setContentText(context.getString(R.string.diary_notif_text))
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

        val (title, message) = getStreakMessage(context, streakDays)

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

        val message = context.getString(R.string.crisis_message_detail)

        val notification = NotificationCompat.Builder(context, CHANNEL_CRISIS_SUPPORT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.crisis_title))
            .setContentText(context.getString(R.string.crisis_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CRISIS, notification)
    }

    private fun getStreakMessage(context: Context, streakDays: Int): Pair<String, String> {
        // Use string resources for each special case, and format for dynamic cases
        return when {
            streakDays == 1 -> Pair(
                context.getString(R.string.streak_title_1),
                context.getString(R.string.streak_msg_1)
            )
            streakDays == 3 -> Pair(
                context.getString(R.string.streak_title_3),
                context.getString(R.string.streak_msg_3)
            )
            streakDays == 7 -> Pair(
                context.getString(R.string.streak_title_7),
                context.getString(R.string.streak_msg_7)
            )
            streakDays == 14 -> Pair(
                context.getString(R.string.streak_title_14),
                context.getString(R.string.streak_msg_14)
            )
            streakDays == 30 -> Pair(
                context.getString(R.string.streak_title_30),
                context.getString(R.string.streak_msg_30)
            )
            streakDays == 60 -> Pair(
                context.getString(R.string.streak_title_60),
                context.getString(R.string.streak_msg_60)
            )
            streakDays == 90 -> Pair(
                context.getString(R.string.streak_title_90),
                context.getString(R.string.streak_msg_90)
            )
            streakDays == 365 -> Pair(
                context.getString(R.string.streak_title_365),
                context.getString(R.string.streak_msg_365)
            )
            streakDays % 10 == 0 && streakDays > 0 -> Pair(
                context.getString(R.string.streak_title_multiple_of_10, streakDays),
                context.getString(R.string.streak_msg_multiple_of_10, streakDays)
            )
            else -> Pair(
                context.getString(R.string.streak_title_default, streakDays),
                context.getString(R.string.streak_msg_default)
            )
        }
    }
}


