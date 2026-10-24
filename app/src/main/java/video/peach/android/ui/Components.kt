package video.peach.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import video.peach.android.data.*
import android.content.Intent
import android.net.Uri

val LocalPeachImages = staticCompositionLocalOf<ImageLoader?> { null }

@Composable fun Notice(text: String, error: Boolean = false, action: String? = null, onClick: () -> Unit = {}) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (error) Icons.Rounded.Info else Icons.Rounded.CloudOff, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp)); Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            if (action != null) TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable fun EmptyState(title: String = "这里还没有内容", message: String = "添加媒体或调整筛选后，内容就会出现在这里。", retry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        retry?.let { OutlinedButton(onClick = it) { Text("重新加载") } }
    }
}

@Composable fun PeachImage(client: PeachClient?, path: String, description: String?, modifier: Modifier, scale: ContentScale = ContentScale.Crop, cover: JSONObject? = null, onAspect: ((Float) -> Unit)? = null) {
    val context = LocalContext.current
    val shared = LocalPeachImages.current
    val loader = shared ?: remember(client) { ImageLoader.Builder(context).apply { client?.let { okHttpClient(it.http) } }.build() }
    val uri = remember(client, path) { runCatching { client?.resolve(path) }.getOrNull() }
    val request = remember(context, uri, cover?.toString()) {
        ImageRequest.Builder(context).data(uri).apply {
            if (cover != null) { allowHardware(false); transformations(CoverTransform(cover)) }
        }.crossfade(true).build()
    }
    if (shared == null) DisposableEffect(loader) { onDispose { loader.shutdown() } }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        Icon(Icons.Rounded.Movie, null, Modifier.align(Alignment.Center).size(30.dp), tint = MaterialTheme.colorScheme.outline)
        if (uri != null) AsyncImage(model = request, imageLoader = loader, contentDescription = description, modifier = Modifier.fillMaxSize(), contentScale = scale,
            onSuccess = { result ->
                val drawable = result.result.drawable
                val aspect = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                if (aspect.isFinite() && aspect > 0) onAspect?.invoke(aspect)
            })
    }
}

fun titleOf(item: JSONObject) = item.text("catalog_title", "title", "name", "canonical_name", "author", "code", "label").ifBlank { "未命名" }
fun imageOf(item: JSONObject): String {
    if (Artwork.official(item)) return Artwork.path(item)
    for (key in listOf("thumb", "thumbnail", "cover", "poster", "image", "avatar", "thumb_url", "cover_url")) {
        val value = item.opt(key)
        if (value is String && (value.startsWith("/") || value.startsWith("http"))) return value
        if (value is JSONObject) value.text("src", "url").takeIf { it.isNotBlank() }?.let { return it }
    }
    return Artwork.path(item)
}

@Composable fun MediaCard(item: JSONObject, client: PeachClient?, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val path = imageOf(item)
    val official = Artwork.official(item)
    var aspect by rememberSaveable(client?.profile?.id, path, item.optJSONObject("poster_box")?.toString()) { mutableFloatStateOf(Artwork.aspect(item)) }
    Column(modifier.testTag("media-card-${item.text("id")}").clip(RoundedCornerShape(14.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box {
            PeachImage(client, path, titleOf(item), Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(12.dp)), scale = ContentScale.Fit, cover = item.takeIf { official }, onAspect = { aspect = it })
            if (item.optDouble("duration", 0.0) > 0) Surface(color = androidx.compose.ui.graphics.Color.Black.copy(alpha = .7f), shape = RoundedCornerShape(5.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp)) {
                Text(clock((item.optDouble("duration") * 1000).toLong()), Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(titleOf(item), Modifier.padding(top = 9.dp, start = 2.dp, end = 2.dp), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val subtitle = listOf(item.text("code"), item.text("creator", "author", "studio")).filter { it.isNotBlank() }.joinToString(" · ")
        if (subtitle.isNotBlank()) Text(subtitle, Modifier.padding(top = 4.dp, bottom = 10.dp, start = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

fun clock(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
}

/** JSON 契约转换成原生信息行，敏感字段不在只读概览中回显。 */
@Composable fun RecordView(record: JSONObject, client: PeachClient?, depth: Int = 0, onRecord: ((JSONObject) -> Unit)? = null) {
    val context = LocalContext.current
    fun open(value: String) {
        val uri = Uri.parse(value)
        if (uri.scheme in listOf("http", "https")) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
    val hidden = setOf("token", "cookie", "password", "authorization", "secret", "credentials", "field_owners", "metadata_json", "mutation_revision")
    val entries = record.values().filter { !it.first.startsWith("_") && it.first !in hidden && it.first !in Presentation.hidden && Labels.known(it.first) && it.second != JSONObject.NULL }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { (key, value) ->
            when (value) {
                is JSONObject -> {
                    var expanded by remember(record, key) { mutableStateOf(depth == 0 && key in listOf("summary", "counts", "stats")) }
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                                Text(Labels.of(key), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, "展开${Labels.of(key)}")
                            }
                            if (expanded && depth < 6) { Spacer(Modifier.height(8.dp)); RecordView(value, client, depth + 1, onRecord) }
                        }
                    }
                }
                is JSONArray -> {
                    if (value.length() > 0) {
                        var expanded by remember(record, key) { mutableStateOf(depth < 1) }
                        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 6.dp)) {
                            Text("${Labels.of(key)} · ${value.length()}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, "展开${Labels.of(key)}")
                        }
                        if (expanded) for (index in 0 until value.length()) {
                            val child = value.get(index)
                            if (child is JSONObject) RecordCard(child, client, onRecord)
                            else if (child.toString().startsWith("https://") || child.toString().startsWith("http://")) TextButton(onClick = { open(child.toString()) }) { Text("打开来源链接 ${index + 1}") }
                            else Text(child.toString(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                else -> if (key in setOf("url", "href", "where") && (value.toString().startsWith("http://") || value.toString().startsWith("https://"))) {
                    TextButton(onClick = { open(value.toString()) }) { Text("打开${Labels.of(key)}") }
                } else if (key !in setOf("ok", "has_more") && value.toString().isNotBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(Labels.of(key), Modifier.widthIn(min = 64.dp, max = 116.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(Presentation.value(key, value), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable fun RecordCard(record: JSONObject, client: PeachClient?, onClick: ((JSONObject) -> Unit)? = null) {
    var expanded by remember(record) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth().clickable { if (onClick != null && record.text("id").isNotBlank()) onClick(record) else expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.text("task_label").ifBlank { titleOf(record).takeUnless { it == "未命名" } ?: record.text("message").ifBlank { "记录" } }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val subtitle = listOf(Presentation.value("status", record.text("status")), record.text("provider_label", "message", "note")).filter { it.isNotBlank() }.joinToString(" · ")
                    if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, "详细信息") }
            }
            if (expanded) { Spacer(Modifier.height(12.dp)); RecordView(record, client, 2, onClick) }
        }
    }
}
