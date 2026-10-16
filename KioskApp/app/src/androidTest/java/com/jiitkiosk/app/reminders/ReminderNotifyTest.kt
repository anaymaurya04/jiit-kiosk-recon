package com.jiitkiosk.app.reminders

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The path an alarm fires into: the receiver must turn its extras into exactly
 * the notification board 21 promises. The alarm timing itself belongs to the
 * system; everything after the broadcast is covered here.
 */
@RunWith(AndroidJUnit4::class)
class ReminderNotifyTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun receiverPostsTheNotification() {
        assumeTrue("needs the runtime notification grant", Notifications.allowed(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()

        context.sendBroadcast(
            Intent(context, ReminderReceiver::class.java)
                .putExtra(ReminderReceiver.EXTRA_ID, "test-work")
                .putExtra(ReminderReceiver.EXTRA_LEAD, 6)
                .putExtra(ReminderReceiver.EXTRA_TITLE, "Lab report")
                .putExtra(ReminderReceiver.EXTRA_COURSE, "Software Engineering"),
        )

        // the receiver runs on the main thread; give it a beat
        val id = "reminder/test-work/6".hashCode()
        val deadline = System.currentTimeMillis() + 5_000
        var found = false
        while (!found && System.currentTimeMillis() < deadline) {
            found = manager.activeNotifications.any { it.id == id }
            if (!found) Thread.sleep(100)
        }

        assertTrue("the reminder notification never arrived", found)
        val posted = manager.activeNotifications.first { it.id == id }
        assertEquals("Lab report is due in 6h", posted.notification.extras.getString("android.title"))
        assertEquals("SOFTWARE ENGINEERING · NOT SUBMITTED", posted.notification.extras.getString("android.text"))
        assertEquals(Notifications.CHANNEL, posted.notification.channelId)
        manager.cancelAll()
    }
}
