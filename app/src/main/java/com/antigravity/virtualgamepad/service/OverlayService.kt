package com.antigravity.virtualgamepad.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antigravity.virtualgamepad.MainActivity
import com.antigravity.virtualgamepad.R
import com.antigravity.virtualgamepad.input.InputRouter
import com.antigravity.virtualgamepad.overlay.OverlayLayoutManager
import com.antigravity.virtualgamepad.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OverlayService : Service() {

    private val tag = "OverlayService"
    var overlayLayoutManager: OverlayLayoutManager? = null
        private set
    private var inputRouter: InputRouter? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()

        inputRouter = InputRouter(this)
        overlayLayoutManager = OverlayLayoutManager(
            context = this,
            inputRouter = inputRouter!!,
            onCloseRequested = {
                stop(this)
            }
        )
        overlayLayoutManager?.init()

        instance = this
        _isRunningFlow.value = true
        Log.i(tag, "OverlayService created and armed for target games")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayLayoutManager?.onConfigurationChanged(newConfig)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                PreferencesRepository.getInstance(this).setServiceArmed(false)
                overlayLayoutManager?.removeAll()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                PreferencesRepository.getInstance(this).setServiceArmed(true)
                // Arm service in background; keep overlay hidden until target game is active
                overlayLayoutManager?.setOverlayVisible(false)
            }
            ACTION_FORCE_SHOW -> {
                PreferencesRepository.getInstance(this).setServiceArmed(true)
                overlayLayoutManager?.setOverlayVisible(true)
            }
            ACTION_FORCE_HIDE -> {
                overlayLayoutManager?.setOverlayVisible(false)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferencesRepository.getInstance(this).setServiceArmed(false)
        overlayLayoutManager?.removeAll()
        overlayLayoutManager = null
        inputRouter?.release()
        inputRouter = null

        instance = null
        _isRunningFlow.value = false
        Log.i(tag, "OverlayService completely stopped and removed from screen")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Virtual Gamepad Service Armed")
            .setContentText("Monitoring for target games (KOTOR).")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(0, "Turn Off", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Virtual Gamepad Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "virtual_gamepad_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.antigravity.virtualgamepad.ACTION_START"
        const val ACTION_STOP = "com.antigravity.virtualgamepad.ACTION_STOP"
        const val ACTION_FORCE_SHOW = "com.antigravity.virtualgamepad.ACTION_FORCE_SHOW"
        const val ACTION_FORCE_HIDE = "com.antigravity.virtualgamepad.ACTION_FORCE_HIDE"

        @Volatile
        var instance: OverlayService? = null
            private set

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        fun isRunning(): Boolean = _isRunningFlow.value

        fun start(context: Context) {
            PreferencesRepository.getInstance(context).setServiceArmed(true)
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun show(context: Context) {
            val prefs = PreferencesRepository.getInstance(context)
            if (!prefs.isServiceArmed()) return

            if (!isRunning()) {
                val intent = Intent(context, OverlayService::class.java).apply {
                    action = ACTION_FORCE_SHOW
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                instance?.overlayLayoutManager?.setOverlayVisible(true)
            }
        }

        fun hide() {
            instance?.overlayLayoutManager?.setOverlayVisible(false)
        }

        /**
         * Ultimate authority stop method: completely removes overlay windows and terminates service.
         */
        fun stop(context: Context) {
            PreferencesRepository.getInstance(context).setServiceArmed(false)
            instance?.overlayLayoutManager?.removeAll()
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
