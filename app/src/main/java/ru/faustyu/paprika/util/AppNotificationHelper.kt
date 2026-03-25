package ru.faustyu.paprika.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationManagerCompat

object AppNotificationHelper {

    private const val CHANNEL_ID = "paprika_messages"
    private const val CHANNEL_NAME = "Messages"

    fun init(context: Context) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(soundUri, audioAttr)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 150, 100, 150)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    fun playMessageSound(context: Context, soundEnabled: Boolean) {
        // We only vibrate lightly inside the app to avoid loud cringe system ringtones
        if (soundEnabled) {
            vibrate(context)
        }
    }

    private fun vibrate(context: Context) {
        val pattern = longArrayOf(0, 150, 100, 150)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    fun updateBadge(context: Context, count: Int) {
        try {
            val nm = NotificationManagerCompat.from(context)
            // Use notification with number for badge support
            val builder = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Paprika")
                .setContentText("$count unread messages")
                .setNumber(count)
                .setSilent(true)
                .setOngoing(true)
                .setAutoCancel(false)
            if (count > 0) {
                nm.notify(1001, builder.build())
            } else {
                nm.cancel(1001)
            }
        } catch (_: Exception) {}
    }
}
