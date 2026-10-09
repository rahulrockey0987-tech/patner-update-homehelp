package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OrderAlarmManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var currentAlertOrderId: String? = null
    private var alarmJob: Job? = null
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Incoming Work Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud sound and vibration alerts for incoming customer orders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 200, 600, 200, 800)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startAlarm(orderId: String, customerName: String, rateText: String) {
        if (isPlaying && currentAlertOrderId == orderId) {
            return // Do not restart for same order
        }

        stopAlarm()
        currentAlertOrderId = orderId
        isPlaying = true

        // 1. Post Android Notification with Intent to app
        postOrderNotification(orderId, customerName, rateText)

        // 2. Start repeating pulse sound + vibration
        alarmJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                // Generate distinctive 2-tone alarm chime (880 Hz High & 587 Hz Mid chime)
                while (isActive && isPlaying) {
                    playChimePulse()
                    triggerVibrationPulse()
                    delay(1200) // Pulse repeat interval
                }
            } catch (e: Exception) {
                Log.e("OrderAlarmManager", "Error during alarm cycle", e)
            } finally {
                stopSoundInternal()
            }
        }
    }

    fun stopAlarm(orderId: String? = null) {
        if (orderId != null && currentAlertOrderId != orderId) {
            return
        }
        isPlaying = false
        currentAlertOrderId = null
        alarmJob?.cancel()
        alarmJob = null
        stopSoundInternal()
        stopVibration()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun playChimePulse() {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * 0.4).toInt() // 400ms chime
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Dual tone: 1046Hz (C6) decaying with 880Hz (A5)
                val freq = if (t < 0.2) 1046.5 else 1318.5
                val decay = 1.0 - (t / 0.4)
                val sample = (Math.sin(2.0 * Math.PI * freq * t) * decay * Short.MAX_VALUE * 0.7).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            audioTrack = track
        } catch (e: Exception) {
            Log.e("OrderAlarmManager", "AudioTrack playback error", e)
        }
    }

    private fun triggerVibrationPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 350, 150, 350),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 350, 150, 350), -1)
            }
        } catch (e: Exception) {
            Log.e("OrderAlarmManager", "Vibrator error", e)
        }
    }

    private fun stopSoundInternal() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    private fun postOrderNotification(orderId: String, customerName: String, rateText: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_ORDER_ID", orderId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚡ NEW WORK REQUEST: $customerName")
                .setContentText("General Helper • $rateText • Tap to accept or reject")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("OrderAlarmManager", "Notification post error", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "homehelp_incoming_orders"
        const val NOTIFICATION_ID = 9001
    }
}
