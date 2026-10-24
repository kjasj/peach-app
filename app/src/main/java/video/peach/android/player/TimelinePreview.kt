package video.peach.android.player

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import org.json.JSONObject
import video.peach.android.data.PeachClient
import video.peach.android.data.text
import video.peach.android.ui.LocalPeachImages

private class SpriteFrame(private val index: Int, private val columns: Int, private val rows: Int) : Transformation {
    override val cacheKey = "peach-timeline:$index:$columns:$rows"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = (input.width / columns).coerceAtLeast(1)
        val height = (input.height / rows).coerceAtLeast(1)
        val x = (index % columns * width).coerceAtMost(input.width - width)
        val y = (index / columns * height).coerceAtMost(input.height - height)
        return Bitmap.createBitmap(input, x, y, width, height)
    }
}

@Composable fun TimelinePreview(client: PeachClient?, item: JSONObject?, meta: JSONObject?, position: Long, duration: Long) {
    val loader = LocalPeachImages.current ?: return
    if (client == null || item == null) return
    val valid = (meta?.optInt("frames") ?: 0) > 0
    val columns = if (valid) meta!!.optInt("columns", 10).coerceAtLeast(1) else 3
    val rows = if (valid) meta!!.optInt("rows", 10).coerceAtLeast(1) else 3
    val perSheet = columns * rows
    val frame = if (valid) ((position / 1000) / meta!!.optInt("interval", 30).coerceAtLeast(1)).toInt().coerceIn(0, meta.optInt("frames") - 1)
        else (position.toDouble() / duration.coerceAtLeast(1) * 8).toInt().coerceIn(0, 8)
    val path = if (valid) "/timeline?id=${item.text("id")}&s=${frame / perSheet}" else "/thumb?id=${item.text("id") }"
    val context = LocalContext.current
    val request = remember(path, frame, client) {
        ImageRequest.Builder(context).data(client.resolve(path)).size(Size.ORIGINAL).apply {
            if (valid) transformations(SpriteFrame(frame % perSheet, columns, rows))
        }.build()
    }
    AsyncImage(request, if (valid) "时间轴预览" else "封面预览", imageLoader = loader, modifier = Modifier.width(160.dp).height(90.dp), contentScale = ContentScale.Fit)
}
