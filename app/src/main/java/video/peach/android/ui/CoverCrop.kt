package video.peach.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.json.JSONObject
import video.peach.android.data.PeachClient
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable fun CoverCrop(client: PeachClient, code: String, onClose: () -> Unit) {
    var width by remember { mutableIntStateOf(1) }
    var height by remember { mutableIntStateOf(1) }
    var start by remember { mutableStateOf<Offset?>(null) }
    var end by remember { mutableStateOf<Offset?>(null) }
    var canvas by remember { mutableStateOf(Size.Zero) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val loader = LocalPeachImages.current ?: return
    fun save(reset: Boolean) {
        if (busy) return
        val body = JSONObject().put("code", code)
        if (reset) body.put("box", JSONObject.NULL) else {
            val a = start ?: return
            val b = end ?: return
            val scale = min(canvas.width / width, canvas.height / height)
            val left = (canvas.width - width * scale) / 2
            val top = (canvas.height - height * scale) / 2
            fun x(value: Float) = ((value - left) / scale).toInt().coerceIn(0, width)
            fun y(value: Float) = ((value - top) / scale).toInt().coerceIn(0, height)
            body.put("box", JSONObject().put("x0", minOf(x(a.x), x(b.x))).put("x1", maxOf(x(a.x), x(b.x))).put("y0", minOf(y(a.y), y(b.y))).put("y1", maxOf(y(a.y), y(b.y))))
        }
        busy = true
        scope.launch { runCatching { client.post("/api/cover-crop", body) }.onSuccess { onClose() }.onFailure { error = it.localizedMessage }; busy = false }
    }
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text("封面取景 · $code") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("在封面上拖动框选正封区域。原始图片保持不变。", style = MaterialTheme.typography.bodySmall)
            Box(Modifier.fillMaxWidth().height(280.dp)) {
                AsyncImage(model = client.resolve("/cover?code=" + java.net.URLEncoder.encode(code, "UTF-8")), imageLoader = loader, contentDescription = "原始封面", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(), onSuccess = { result -> width = result.result.drawable.intrinsicWidth.coerceAtLeast(1); height = result.result.drawable.intrinsicHeight.coerceAtLeast(1) })
                Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectDragGestures(onDragStart = { start = it; end = it }, onDrag = { change, _ -> end = change.position; change.consume() }) }) {
                    canvas = size
                    val a = start; val b = end
                    if (a != null && b != null) drawRect(Color(0xFFF2557B), Offset(minOf(a.x, b.x), minOf(a.y, b.y)), Size(kotlin.math.abs(a.x-b.x), kotlin.math.abs(a.y-b.y)), style = Stroke(3.dp.toPx()))
                }
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { Button(onClick = { save(false) }, enabled = !busy && start != null && end != null) { Text(if (busy) "保存中…" else "保存取景") } }, dismissButton = { Row { TextButton(onClick = { save(true) }, enabled = !busy) { Text("恢复默认") }; TextButton(onClick = onClose, enabled = !busy) { Text("取消") } } })
}
