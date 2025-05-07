package com.kevmo314.kineticstreamer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService

class StreamingService : LifecycleService() {
    private var activeCameraId: String? = null
    private var isStreaming: Boolean = false

    override fun onCreate() {
        super.onCreate()
        
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kinetic Streamer",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Streaming service notification channel"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kinetic Streamer")
                .setContentText("Streaming service is running")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    private val binder = object : IStreamingService.Stub() {
        @Throws(RemoteException::class)
        override fun setPreviewSurface(surface: Surface) {
            Log.d(TAG, "Setting preview surface")
            // TODO: Implement preview surface handling
        }

        @Throws(RemoteException::class)
        override fun startStreaming(config: StreamingConfiguration) {
            if (this@StreamingService.isStreaming) {
                return
            }
            this@StreamingService.isStreaming = true
            Log.d(TAG, "Starting stream with config: $config")
            // TODO: Implement streaming logic
        }

        @Throws(RemoteException::class)
        override fun stopStreaming() {
            if (!this@StreamingService.isStreaming) {
                return
            }
            this@StreamingService.isStreaming = false
            Log.d(TAG, "Stopping stream")
            // TODO: Implement stop streaming logic
        }

        @Throws(RemoteException::class)
        override fun isStreaming(): Boolean = this@StreamingService.isStreaming

        @Throws(RemoteException::class)
        override fun getActiveCameraId(): String = activeCameraId ?: ""

        @Throws(RemoteException::class)
        override fun setActiveCameraId(cameraId: String) {
            activeCameraId = cameraId
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    companion object {
        private const val TAG = "StreamingService"
        private const val CHANNEL_ID = "KINETIC_STREAMER"
        private const val NOTIFICATION_ID = 68448
    }
}