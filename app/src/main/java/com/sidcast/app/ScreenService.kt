package com.sidcast.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.NoFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.GifObjectFilterRender
import com.pedro.library.rtmp.RtmpDisplay

class ScreenService : Service(), ConnectChecker {

    companion object {
        var rtmpDisplay: RtmpDisplay? = null
        var gifFilter: GifObjectFilterRender? = null
        var isGifVisible = false
        var endpointUrl = ""
        var resultCode = 0
        var resultData: Intent? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "sidcast_channel")
            .setContentTitle("sidcast")
            .setContentText("Transmissão ao vivo ativa")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(101, notification)

        if (rtmpDisplay == null) {
            rtmpDisplay = RtmpDisplay(baseContext, true, this)
        }
        resultData?.let { data ->
            rtmpDisplay?.setIntentResult(resultCode, data)
            if (rtmpDisplay?.prepareAudio() == true && rtmpDisplay?.prepareVideo(720, 1280, 30, 2500 * 1024, 0, 320) == true) {
                rtmpDisplay?.startStream(endpointUrl)
            }
        }
        return START_NOT_STICKY
    }

    fun setGifOverlay(context: Context, uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            val filter = GifObjectFilterRender()
            filter.setGif(inputStream)
            filter.setScale(35f, 35f)
            filter.setPosition(5f, 5f)
            gifFilter = filter
            rtmpDisplay?.glInterface?.setFilter(filter)
            isGifVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleGif() {
        if (isGifVisible) {
            rtmpDisplay?.glInterface?.setFilter(NoFilterRender())
            isGifVisible = false
        } else {
            gifFilter?.let {
                rtmpDisplay?.glInterface?.setFilter(it)
                isGifVisible = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rtmpDisplay?.stopStream()
        rtmpDisplay = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("sidcast_channel", "sidcast Live", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onConnectionStarted(url: String) {}
    override fun onConnectionSuccess() {}
    override fun onConnectionFailed(reason: String) {
        rtmpDisplay?.stopStream()
    }
    override fun onNewBitrate(bitrate: Long) {}
    override fun onDisconnect() {}
    override fun onAuthError() {}
    override fun onAuthSuccess() {}
}
