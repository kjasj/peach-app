package video.peach.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import video.peach.android.data.*

fun galleryImages(record: JSONObject): List<Pair<String, String>> {
    val images = mutableListOf<Pair<String, String>>()
    val media = record.optJSONArray("media_items") ?: record.optJSONArray("media")
    media?.objects()?.forEachIndexed { index, item ->
        if (item.text("media_kind", "kind", "type").contains("image")) images.add((item.text("src", "url").takeIf { it.startsWith("/") } ?: "/follow-image?id=${record.text("id")}&media=${item.optInt("index", index)}") to item.text("name", "label"))
    }
    for (key in listOf("sample_images", "images", "photos")) {
        val array = record.optJSONArray(key) ?: continue
        for (index in 0 until array.length()) {
            val item = array.get(index)
            val path = when (item) { is JSONObject -> item.text("src", "url", "image", "thumb"); is String -> item; else -> "" }
            if (path.startsWith("/") || path.startsWith("http")) images.add(path to "图片 ${index + 1}")
        }
    }
    if (images.isEmpty() && record.text("media_kind") == "image") images.add("/follow-image?id=${record.text("id")}" to titleOf(record))
    return images.distinctBy { it.first }
}

@Composable fun Gallery(images: List<Pair<String, String>>, client: PeachClient?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val pager = rememberPagerState(pageCount = { images.size })
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { index ->
                var zoom by remember(index) { mutableFloatStateOf(1f) }
                var dx by remember(index) { mutableFloatStateOf(0f) }
                var dy by remember(index) { mutableFloatStateOf(0f) }
                PeachImage(client, images[index].first, images[index].second, Modifier.fillMaxSize().pointerInput(index) {
                    detectTransformGestures { _, pan, scale, _ ->
                        zoom = (zoom * scale).coerceIn(1f, 5f)
                        dx = if (zoom > 1f) (dx + pan.x).coerceIn(-size.width.toFloat(), size.width.toFloat()) else 0f
                        dy = if (zoom > 1f) (dy + pan.y).coerceIn(-size.height.toFloat(), size.height.toFloat()) else 0f
                    }
                }.graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = dx, translationY = dy), ContentScale.Fit)
            }
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${pager.currentPage + 1} / ${images.size}", Modifier.weight(1f), color = Color.White)
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "关闭图片集", tint = Color.White) }
            }
        }
    }
}
