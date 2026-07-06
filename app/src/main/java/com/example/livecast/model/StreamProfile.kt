package com.example.livecast.model

/**
 * Đại diện cho một "kênh" phát trực tiếp mà người dùng đã cấu hình.
 * platform: Facebook / Youtube / Twitch / Tùy chỉnh — chỉ để hiển thị icon/label,
 * việc đẩy luồng thực tế luôn dùng chuẩn RTMP nên hoạt động với mọi nền tảng
 * miễn là có rtmpUrl (server) + streamKey do nền tảng đó cấp.
 */
data class StreamProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var platform: Platform,
    var rtmpUrl: String,      // vd: rtmp://live-api-s.facebook.com:80/rtmp/
    var streamKey: String,    // khóa luồng riêng tư do Facebook/Youtube cấp cho từng buổi live
    var loopVideo: Boolean = true
) {
    /** Ghép URL server + stream key thành 1 endpoint RTMP hoàn chỉnh để đẩy luồng */
    fun fullRtmpEndpoint(): String {
        val base = rtmpUrl.trimEnd('/')
        return if (streamKey.isBlank()) base else "$base/$streamKey"
    }

    enum class Platform(val label: String, val defaultServer: String) {
        FACEBOOK("Facebook Live", "rtmp://live-api-s.facebook.com:80/rtmp/"),
        YOUTUBE("YouTube Live", "rtmp://a.rtmp.youtube.com/live2"),
        TWITCH("Twitch", "rtmp://live.twitch.tv/app"),
        CUSTOM("Tùy chỉnh (RTMP khác)", "")
    }
}
