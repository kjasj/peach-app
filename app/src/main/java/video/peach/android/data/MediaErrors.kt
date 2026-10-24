package video.peach.android.data

object MediaErrors {
    fun reason(raw: String): String = when (raw.trim().lowercase()) {
        "unavailable" -> "Peach 服务无法读取这部影片的文件。请在电脑端核对该作品的文件位置、挂载和读取权限。"
        "no such id" -> "这部影片的记录已不存在，请刷新馆藏。"
        "offline" -> "媒体来源已离线，请在电脑端检查磁盘或网络挂载。"
        "transcode unavailable", "ffmpeg unavailable" -> "服务器转码工具不可用，请在电脑端检查 FFmpeg。"
        "hls unavailable", "segment unavailable" -> "服务器未能提供播放分片，可重试或改用原始播放。"
        "stream cancelled" -> "本次播放会话已取消，请重新打开影片。"
        "follow media unavailable" -> "关注来源未能提供视频，请检查来源登录状态或稍后重试。"
        else -> raw
    }
    fun http(status: Int, raw: String = ""): String {
        val detail = if (raw.isNotBlank()) reason(raw) else when (status) {
            401 -> "登录已过期，请重新连接服务器。"
            403 -> "服务器拒绝读取媒体，请检查访问权限。"
            404 -> "服务器没有找到可读取的媒体资源，请在电脑端检查文件位置。"
            429 -> "来源请求过于频繁，请稍后重试。"
            503 -> "媒体来源离线或服务器暂不可用。"
            else -> "读取媒体失败，请检查网络或服务器。"
        }
        return "$detail（HTTP $status）"
    }
}
