package com.tylor.repwise.health

import android.content.Context
import androidx.work.*
import com.tylor.repwise.RepWiseApp
import com.tylor.repwise.data.SyncStatus

/** Pushes any unsynced workouts and food entries to Health Connect; retries on failure. */
class HealthSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RepWiseApp
        val hc = app.healthConnect
        val db = app.database
        if (!hc.hasAllPermissions()) return Result.success() // nothing to do until the user grants access

        val bodyWeight = db.profiles().get()?.weightKg ?: 80.0
        var failed = false
        for (workout in db.workouts().unsynced()) {
            runCatching { hc.writeWorkout(workout, db.workouts().sets(workout.id), bodyWeight) }
                .onSuccess { db.workouts().update(workout.copy(syncStatus = SyncStatus.SYNCED)) }
                .onFailure { failed = true; db.workouts().update(workout.copy(syncStatus = SyncStatus.FAILED)) }
        }
        for (entry in db.food().unsynced()) {
            runCatching { hc.writeFood(entry) }
                .onSuccess { db.food().update(entry.copy(syncStatus = SyncStatus.SYNCED)) }
                .onFailure { failed = true; db.food().update(entry.copy(syncStatus = SyncStatus.FAILED)) }
        }
        for (w in db.weighIns().unsynced()) {
            runCatching { hc.writeWeight(w) }
                .onSuccess { db.weighIns().update(w.copy(syncStatus = SyncStatus.SYNCED)) }
                .onFailure { failed = true; db.weighIns().update(w.copy(syncStatus = SyncStatus.FAILED)) }
        }
        return if (failed && runAttemptCount < 5) Result.retry() else Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<HealthSyncWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("hc-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
