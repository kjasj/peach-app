package video.peach.android.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream

fun ContentResolver.displayName(uri: Uri, fallback: String): String =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }?.takeIf { it.isNotBlank() } ?: fallback

/** 不相信 provider 的 available()/文件大小，边读取边限制内存占用。 */
fun ContentResolver.readBounded(uri: Uri, maximum: Int): ByteArray =
    openInputStream(uri)?.use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= maximum) { "文件过大，最大支持 ${maximum / 1024 / 1024} MB" }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: error("无法读取所选文件")
