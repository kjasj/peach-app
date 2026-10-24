package video.peach.android.ui

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation
import org.json.JSONObject
import video.peach.android.data.Artwork

/** 框按原图比例投影到缩略图，避免把封底当作正封。 */
class CoverTransform(private val item: JSONObject) : Transformation {
    override val cacheKey = "peach-cover-v1:${item.optJSONObject("poster_box")}:${item.optJSONObject("cover_frame")}:${item.optString("title")}" 
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val box = Artwork.window(item, input.width.toFloat() / input.height) ?: return input
        val left = (box.left * input.width).toInt().coerceIn(0, input.width - 1)
        val top = (box.top * input.height).toInt().coerceIn(0, input.height - 1)
        val right = (box.right * input.width).toInt().coerceIn(left + 1, input.width)
        val bottom = (box.bottom * input.height).toInt().coerceIn(top + 1, input.height)
        return Bitmap.createBitmap(input, left, top, right - left, bottom - top)
    }
}
