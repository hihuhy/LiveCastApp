package com.example.livecast.service

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.livecast.R
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.decoder.AudioDecoderInterface
import com.pedro.encoder.input.decoder.VideoDecoderInterface
import com.pedro.library.generic.GenericFromFile

/**
 * Service chạy nền (foreground) chịu trách nhiệm đẩy một video có sẵn trên máy
 * lên máy chủ RTMP (Facebook Live / YouTube Live / Twitch / server tùy chỉnh),
 * tương tự cách app GoStream gốc "livestream" video ghi sẵn.
 *
 * Dùng đúng class GenericFromFile của thư viện RootEncoder (mã nguồn mở), khởi tạo
 * ở "chế độ nền hoàn toàn" (không cần OpenGlView/Context cho GL) nên chạy được
 * trong Service không cần giao diện hiển thị.
 */
class StreamService : Service(), ConnectChecker, VideoDecoderInterface, AudioDecoderInterface {

    private lateinit var genericFromFile: GenericFromFile
    private var loop = true

    override fun onCreate() {
        super.onCreate()
        // Constructor không cần Context/OpenGlView -> chạy nền hoàn toàn, không cần giao diện
        genericFromFile = GenericFromFile(this, this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Đang chuẩn bị phát trực tiếp..."))

        when (intent?.action) {
            ACTION_STOP -> {
                stopStreaming()
                return START_NOT_STICKY
            }
            else -> {
                val videoUri: Uri = intent?.getParcelableExtra(EXTRA_VIDEO_URI) ?: return START_NOT_STICKY
                val rtmpUrl = intent.getStringExtra(EXTRA_RTMP_URL) ?: return START_NOT_STICKY
                loop = intent.getBooleanExtra(EXTRA_LOOP, true)
                startStreaming(videoUri, rtmpUrl)
            }
        }
        return START_STICKY
    }

    private fun startStreaming(videoUri: Uri, rtmpUrl: String) {
        try {
            genericFromFile.setLoopMode(loop)
            val videoOk = genericFromFile.prepareVideo(applicationContext, videoUri)
            val audioOk = genericFromFile.prepareAudio(applicationContext, videoUri)

            if (videoOk && audioOk) {
                genericFromFile.startStream(rtmpUrl)
                updateNotification("Đang phát trực tiếp...")
            } else {
                updateNotification("Lỗi: thiết bị không hỗ trợ giải mã video/audio này")
                stopSelf()
            }
        } catch (e: Exception) {
            updateNotification("Lỗi khi bắt đầu stream: ${e.message}")
            stopSelf()
        }
    }

    private fun stopStreaming() {
        if (::genericFromFile.isInitialized && genericFromFile.isStreaming) {
            genericFromFile.stopStream()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (::genericFromFile.isInitialized && genericFromFile.isStreaming) {
            genericFromFile.stopStream()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ----- VideoDecoderInterface / AudioDecoderInterface -----
    override fun onVideoDecoderFinished() {
        // Khi không loop và video phát hết, tự dừng stream
        if (!loop) stopStreaming()
    }

    override fun onAudioDecoderFinished() {}

    // ----- ConnectChecker: callback trạng thái kết nối RTMP -----
    override fun onConnectionStarted(url: String) {}
    override fun onConnectionSuccess() { updateNotification("Đã kết nối - đang LIVE") }
    override fun onConnectionFailed(reason: String) {
        updateNotification("Kết nối thất bại: $reason")
        stopStreaming()
    }
    override fun onNewBitrate(bitrate: Long) {}
    override fun onDisconnect() { updateNotification("Đã ngắt kết nối") }
    override fun onAuthError() { updateNotification("Lỗi xác thực với server RTMP") }
    override fun onAuthSuccess() {}

    // ----- Notification bắt buộc cho Foreground Service -----
    private fun buildNotification(text: String): Notification {
        val channelId = "livecast_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "LiveCast Streaming", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("LiveCast")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stream)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_RTMP_URL = "extra_rtmp_url"
        const val EXTRA_LOOP = "extra_loop"
        const val ACTION_STOP = "action_stop"
        private const val NOTIFICATION_ID = 1001
    }
}
