package com.jiitkiosk.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jiitkiosk.app.MainActivity
import com.jiitkiosk.app.R

/**
 * The reminder channel and the one notification shape the app posts. The text
 * is the pair from board 21: "[Title] is due in 6h" / "[COURSE] · NOT SUBMITTED".
 */
object Notifications {

    const val CHANNEL = "reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Deadline reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders for unsubmitted Classroom coursework"
                },
            )
        }
    }

    /** on 13+ this needs the runtime grant; below it is a yes */
    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun post(context: Context, workId: String, lead: Int, title: String, course: String, link: String?) {
        if (!allowed(context)) return
        ensureChannel(context)
        // tapping opens the assignment itself when Classroom gave a link
        val target = if (!link.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val tap = PendingIntent.getActivity(
            context,
            workId.hashCode(),
            target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("$title is due in ${lead}h")
            .setContentText("${course.uppercase()} · NOT SUBMITTED")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context)
                .notify("reminder/$workId/$lead".hashCode(), notification)
        }
    }
}
