package video.peach.android.data

import org.json.JSONObject
import java.net.URLEncoder

/** 与网页 coverUrl 一致，使用刮削封面及服务端内容版本。 */
object Artwork {
    fun official(item: JSONObject) = item.flag("has_cover") && item.text("cover_key", "code").isNotBlank()
    private fun encoded(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    fun path(item: JSONObject): String {
        if (official(item)) {
            val version = item.text("cover_version").takeIf(String::isNotBlank)?.let { "&v=${encoded(it)}" }.orEmpty()
            return "/cover?code=${encoded(item.text("cover_key", "code"))}&thumb=1$version"
        }
        item.text("follow_thumb_url", "thumb_url").takeIf { it.startsWith("/") || it.startsWith("https://") || it.startsWith("http://") }?.let { return it }
        return if (item.flag("_follow")) "/follow-cover?id=${item.text("id")}" else "/poster?id=${item.text("id")}&c=4"
    }
    data class Window(val left: Float, val top: Float, val right: Float, val bottom: Float)
    fun aspect(item: JSONObject): Float {
        if (official(item)) {
            val box = item.optJSONObject("poster_box")
            if (box != null) {
                val width = box.optDouble("x1") - box.optDouble("x0")
                val height = box.optDouble("y1") - box.optDouble("y0")
                if (width > 0 && height > 0 && (width / height).isFinite()) return (width / height).toFloat()
            }
            return if (item.flag("is_jav")) .704f else 16f / 9f
        }
        val width = item.optDouble("width", 0.0)
        val height = item.optDouble("height", 0.0)
        return if (width > 0 && height > 0 && (width / height).isFinite()) (width / height).toFloat() else 16f / 9f
    }
    fun window(item: JSONObject, aspect: Float): Window? {
        val box = item.optJSONObject("poster_box")
        val dimensions = box?.optJSONArray("px")
        val width = dimensions?.optDouble(0) ?: 0.0
        val height = dimensions?.optDouble(1) ?: 0.0
        if (box != null && width > 0 && height > 0 && kotlin.math.abs(width / height - aspect) < .04) {
            val left = (box.optDouble("x0", 0.0) / width).toFloat().coerceIn(0f, 1f)
            val top = (box.optDouble("y0", 0.0) / height).toFloat().coerceIn(0f, 1f)
            val right = (box.optDouble("x1", width) / width).toFloat().coerceIn(left, 1f)
            val bottom = (box.optDouble("y1", height) / height).toFloat().coerceIn(top, 1f)
            if (right > left && bottom > top) return Window(left, top, right, bottom)
        }
        val still = item.optJSONObject("cover_frame")?.text("layout") == "still"
        if (still || aspect <= 1.2f) return null
        val wide = item.text("title", "catalog_title", "name").contains(Regex("blu[ -]?ray|ブルーレイ", RegexOption.IGNORE_CASE)) && aspect in 1.75f..1.85f
        val panel = if (wide) .85f else .704f
        return Window((1 - panel / aspect).coerceIn(0f, 1f), 0f, 1f, 1f)
    }
}
