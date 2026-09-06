package com.example.vehiclemaintenance.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

const val DAILY_DUE_CHECK_WORK = "daily-due-check"

/**
 * WorkManager keeps its own queue across reboots and app updates, so the daily check needs no boot
 * receiver. KEEP means restarting the app never resets the period back to a full day.
 */
fun scheduleDailyDueCheck(context: Context) {
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        DAILY_DUE_CHECK_WORK,
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS).build(),
    )
}
