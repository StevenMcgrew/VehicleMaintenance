package com.example.vehiclemaintenance.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.example.vehiclemaintenance.MainActivity
import com.example.vehiclemaintenance.VEHICLE_DEEP_LINK_PREFIX
import com.example.vehiclemaintenance.R

private const val CHANNEL_ID = "service_reminders"

/**
 * Posts and clears the per-vehicle reminder. Without the notification permission every call is a
 * no-op, which is why the on-screen due and overdue status has to stand on its own.
 */
class ReminderNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun post(reminder: VehicleReminder) {
        val vehicle = reminder.vehicle
        val title = context.getString(
            R.string.vehicle_summary,
            vehicle.year,
            vehicle.make,
            vehicle.model,
        )
        val summary = context.resources.getQuantityString(
            R.plurals.notification_due_count,
            reminder.dueItemNames.size,
            reminder.dueItemNames.size,
        )
        val names = reminder.dueItemNames.joinToString(
            context.getString(R.string.notification_name_separator),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(summary)
            // No big content title: the expanded form keeps the vehicle as its title, which is
            // the only thing that says which vehicle these services belong to.
            .setStyle(NotificationCompat.BigTextStyle().bigText(names))
            .setContentIntent(openVehicleIntent(vehicle.id))
            .setAutoCancel(true)
            .build()
        // Permission is checked by the platform; a refusal drops the post instead of throwing.
        if (manager.areNotificationsEnabled()) {
            manager.notify(notificationId(vehicle.id), notification)
        }
    }

    fun cancel(vehicleId: String) = manager.cancel(notificationId(vehicleId))

    private fun openVehicleIntent(vehicleId: String): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            "$VEHICLE_DEEP_LINK_PREFIX/$vehicleId".toUri(),
            context,
            MainActivity::class.java,
        )
        return PendingIntent.getActivity(
            context,
            notificationId(vehicleId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/** Stable per vehicle, so a repeat replaces its own notification instead of stacking a new one. */
private fun notificationId(vehicleId: String): Int = vehicleId.hashCode()
