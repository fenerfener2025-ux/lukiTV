package com.example.service

import android.content.Intent
import android.util.Log
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.AuroraApplication

/**
 * CarMediaPlaybackService allows Android Auto, Android Automotive OS (AAOS),
 * and vehicle Bluetooth/dashboard controllers to discover, bind to, and control
 * Pinpirik TV / AuroraTV streams directly from car dashboards and steering wheel buttons.
 */
class CarMediaPlaybackService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return try {
            val session = AuroraApplication.instance.playerEngineManager.getMediaSession()
            if (session != null) {
                Log.d("CarMediaService", "Delivering MediaSession to car controller: ${controllerInfo.packageName}")
            }
            session
        } catch (e: Exception) {
            Log.e("CarMediaService", "Failed to obtain MediaSession for car: ${e.message}")
            null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep media playback active in the background when user switches to GPS/Navigation
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CarMediaService", "CarMediaPlaybackService destroyed")
    }
}
