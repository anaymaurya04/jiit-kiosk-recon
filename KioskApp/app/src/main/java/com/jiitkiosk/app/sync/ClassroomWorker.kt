package com.jiitkiosk.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jiitkiosk.app.classroom.ClassroomClient
import com.jiitkiosk.app.classroom.ClassroomData
import com.jiitkiosk.app.classroom.ClassroomStore
import com.jiitkiosk.app.data.Cache
import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.reminders.ReminderSync
import com.jiitkiosk.app.widget.KioskWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * The background half of the Class tab: every 15 minutes (the platform's
 * floor) while there is a connection, refetch the coursework, write the same
 * cache key the screen reads, and re-lay the reminders — which is what makes a
 * submission cancel its own pings even if the app is never opened.
 */
class ClassroomWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = ClassroomStore(applicationContext)
        val token = store.token() ?: return Result.success()
        return try {
            val data = withContext(Dispatchers.IO) { ClassroomClient(token).fetch() }
            Cache(applicationContext.filesDir)
                .write(ClassroomData.CACHE_KEY, ClassroomData.serializer(), data)
            ReminderSync.refresh(applicationContext, data)
            KioskWidget.refresh(applicationContext)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // an expired authorization is the tab's business, not a retry loop's;
            // everything else (offline, Google down) keeps the cache and the plan
            if (e is HttpError && (e.code == 401 || e.code == 403)) Result.success() else Result.retry()
        }
    }

    companion object {
        const val NAME = "classroom-sync"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ClassroomWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                    )
                    .build(),
            )
        }
    }
}
