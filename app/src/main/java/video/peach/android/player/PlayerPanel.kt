package video.peach.android.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import video.peach.android.data.objects
import video.peach.android.data.text
import video.peach.android.data.displayName
import video.peach.android.data.readBounded
import video.peach.android.ui.*
import kotlin.math.abs

/** 触摸事件只由一个状态机消费；控制栏是独立的 Compose 命中区域。 */
class PlaybackGestureView(context: Context) : View(context) {
    var controller: PlaybackViewModel? = null
    var onHint: (String) -> Unit = {}
    private val handler = Handler(Looper.getMainLooper())
    private var startX = 0f
    private var startY = 0f
    private var startPosition = 0L
    private var previewPosition = 0L
    private var brightness = .5f
    private var volume = 0
    private var moved = false
    private var held = false
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val hold = Runnable { controller?.hold(); held = true; onHint("") }
    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean { if (!moved && !held) performClick(); return true }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            handler.removeCallbacks(hold)
            controller?.releaseGesture()
            val target = controller ?: return true
            val currentPos = target.player?.currentPosition ?: 0L
            if (e.x < width * 0.35f) {
                target.seek(currentPos - 10000)
                onHint("快退 10 秒 ⏪")
            } else if (e.x > width * 0.65f) {
                target.seek(currentPos + 10000)
                onHint("快进 10 秒 ⏩")
            } else {
                target.toggle()
            }
            return true
        }
    }).apply { setIsLongpressEnabled(false) }
    init { isClickable = true; contentDescription = "视频画面，激活显示或隐藏控制栏" }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val vm = controller ?: return false
        if (vm.state.value.locked) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x; startY = event.y; startPosition = vm.player?.currentPosition ?: 0
                previewPosition = startPosition; moved = false; held = false
                brightness = (context as? Activity)?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: .5f
                volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                vm.press(); handler.postDelayed(hold, 350); parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - startX; val dy = event.y - startY
                if (abs(dx) > slop || abs(dy) > slop) {
                    handler.removeCallbacks(hold)
                    if (vm.gestures.drag(abs(dx) >= abs(dy), startX < width / 2f)) {
                        moved = true
                        when (vm.gestures.mode) {
                            GestureMachine.Mode.SEEK -> {
                                val span = (vm.state.value.duration / 4).coerceIn(30000, 300000)
                                previewPosition = (startPosition + (dx / width.coerceAtLeast(1) * span).toLong()).coerceIn(0, vm.state.value.duration.coerceAtLeast(startPosition))
                                onHint("${clock(previewPosition)} / ${clock(vm.state.value.duration)}")
                            }
                            GestureMachine.Mode.BRIGHTNESS -> {
                                val value = (brightness - dy / height.coerceAtLeast(1)).coerceIn(.02f, 1f)
                                (context as? Activity)?.window?.let { window -> window.attributes = window.attributes.apply { screenBrightness = value } }
                                onHint("亮度 ${(value * 100).toInt()}%")
                            }
                            GestureMachine.Mode.VOLUME -> {
                                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val value = (volume - dy / height.coerceAtLeast(1) * max).toInt().coerceIn(0, max)
                                audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
                                onHint("音量 ${value * 100 / max.coerceAtLeast(1)}%")
                            }
                            else -> Unit
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(hold)
                if (event.actionMasked == MotionEvent.ACTION_UP && vm.gestures.mode == GestureMachine.Mode.SEEK) vm.seek(previewPosition)
                vm.releaseGesture(); onHint(""); parent.requestDisallowInterceptTouchEvent(false)
                if (moved || held) {
                    val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                    detector.onTouchEvent(cancel); cancel.recycle()
                }
            }
        }
        if (!moved && !held) detector.onTouchEvent(event)
        return true
    }
    override fun onDetachedFromWindow() { handler.removeCallbacksAndMessages(null); controller?.releaseGesture(); super.onDetachedFromWindow() }
    override fun performClick(): Boolean { super.performClick(); controller?.controls(); return true }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun PlayerPanel(vm: PlaybackViewModel, state: PlayingState, modifier: Modifier = Modifier, onPip: () -> Unit, onSwitch: (JSONObject) -> Unit) {
    val context = LocalContext.current
    val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
    var settings by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }
    var slider by remember { mutableStateOf<Float?>(null) }
    var menu by remember { mutableStateOf<String?>(null) }
    val importScope = rememberCoroutineScope()
    LaunchedEffect(state.playing, state.controls, state.locked, state.item) {
        if (state.playing && state.controls && !state.locked && !accessibility.isTouchExplorationEnabled) { delay(3500); vm.hideControls() }
    }
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            importScope.launch {
                runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { context.contentResolver.readBounded(it, 8 * 1024 * 1024) } }
                    .onSuccess { bytes -> vm.importSubtitle(context.contentResolver.displayName(it, "字幕.srt"), bytes) }
                    .onFailure { error -> hint = error.localizedMessage ?: "字幕导入失败" }
            }
        }
    }
    Box(modifier.background(Color.Black)) {
        AndroidView(factory = { activityContext -> PlayerView(activityContext).apply { useController = false; keepScreenOn = true; player = vm.player; resizeMode = state.resizeMode } }, update = { view ->
            view.player = vm.player
            view.resizeMode = state.resizeMode
            view.subtitleView?.apply {
                setApplyEmbeddedStyles(false); setApplyEmbeddedFontSizes(false); setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, state.subtitleSize)
                setStyle(CaptionStyleCompat(state.subtitleColor, state.subtitleBackground, android.graphics.Color.TRANSPARENT,
                    when (state.subtitleEdge) {
                        0 -> CaptionStyleCompat.EDGE_TYPE_NONE
                        2 -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                        3 -> CaptionStyleCompat.EDGE_TYPE_RAISED
                        4 -> CaptionStyleCompat.EDGE_TYPE_DEPRESSED
                        else -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                    }, android.graphics.Color.BLACK, Typeface.create(state.subtitleFont, Typeface.NORMAL)))
            }
        }, modifier = Modifier.fillMaxSize())
        if (!state.pip) AndroidView(factory = { ctx -> PlaybackGestureView(ctx).apply { controller = vm; onHint = { hint = it } } }, update = { it.controller = vm }, modifier = Modifier.fillMaxSize())
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        if (slider != null) Box(Modifier.align(Alignment.TopCenter).padding(top = 45.dp)) { TimelinePreview(vm.currentClient(), state.item, state.timeline, slider!!.toLong(), state.duration) }
        val badge = if (state.boost) "${state.boostSpeed}× 临时倍速" else hint
        if (badge.isNotEmpty()) Surface(color = Color.Black.copy(alpha = .7f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.Center)) { Text(badge, Modifier.padding(16.dp), color = Color.White) }
        if (state.locked && !state.pip) IconButton(onClick = vm::lock, modifier = Modifier.align(Alignment.CenterStart).padding(12.dp).background(Color.Black.copy(alpha = .5f))) { Icon(Icons.Rounded.Lock, "解锁播放器", tint = Color.White) }
        if (state.controls && !state.locked && !state.pip) {
            Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .25f)).padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (state.full) vm.full(false) else vm.close() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "关闭或返回播放器", tint = Color.White) }
                    Text(state.item?.let(::titleOf).orEmpty(), Modifier.weight(1f), color = Color.White, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { vm.mini(!state.mini) }) { Icon(if (state.mini) Icons.Rounded.OpenInFull else Icons.Rounded.PictureInPictureAlt, "应用内小窗", tint = Color.White) }
                    IconButton(onClick = { settings = true }) { Icon(Icons.Rounded.Settings, "播放设置", tint = Color.White) }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.seek(state.position - 10000) }) { Icon(Icons.Rounded.Replay10, "快退 10 秒", tint = Color.White) }
                    IconButton(onClick = vm::toggle, Modifier.size(56.dp)) { Icon(if (state.playing) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle, if (state.playing) "暂停" else "继续播放", Modifier.size(44.dp), tint = Color.White) }
                    IconButton(onClick = { vm.seek(state.position + 10000) }) { Icon(Icons.Rounded.Forward10, "快进 10 秒", tint = Color.White) }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(clock((slider?.toLong() ?: state.position)), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Slider(value = slider ?: state.position.toFloat().coerceAtMost(state.duration.toFloat().coerceAtLeast(1f)), onValueChange = { slider = it }, onValueChangeFinished = { slider?.let { vm.seek(it.toLong()) }; slider = null }, valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f), modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = .3f)))
                    Text(clock(state.duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                if (!state.mini) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::lock) { Icon(Icons.Rounded.LockOpen, "锁定手势", tint = Color.White) }
                    TextButton(onClick = { menu = "subtitles" }, Modifier.weight(1f)) { Text(state.subtitleLabel, color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    TextButton(onClick = { menu = "versions" }) { Text("选集", color = Color.White) }
                    IconButton(onClick = { hint = vm.cycleResizeMode() }) { Icon(Icons.Rounded.AspectRatio, "切换画面比例", tint = Color.White) }
                    IconButton(onClick = onPip) { Icon(Icons.Rounded.PictureInPicture, "系统画中画", tint = Color.White) }
                    IconButton(onClick = { vm.full(!state.full) }) { Icon(if (state.full) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, "切换全屏", tint = Color.White) }
                }
            }
        }
    }
    if (state.fatalError && !state.pip) AlertDialog(onDismissRequest = vm::close, title = { Text("无法播放") },
        text = { Text(state.error.orEmpty()) }, confirmButton = { TextButton(onClick = vm::retry) { Text("重新尝试") } },
        dismissButton = { TextButton(onClick = vm::close) { Text("关闭播放器") } })
    if (settings) ModalBottomSheet(onDismissRequest = { settings = false }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("播放设置", style = MaterialTheme.typography.titleLarge)
            Text("固定播放速度 · ${state.speed}×")
            FlowRow { listOf(.5f, 1f, 1.25f, 1.5f, 2f, 3f).forEach { value -> TextButton(onClick = { vm.speed(value) }) { Text("$value×") } } }
            Text("长按画面 350 毫秒：${state.boostSpeed}×，松手恢复原速度", style = MaterialTheme.typography.bodySmall)
            Text("快进跳转模式")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.fastSeek, onClick = { vm.setFastSeek(true) }, label = { Text("关键帧秒开 (推荐 4K)") })
                FilterChip(selected = !state.fastSeek, onClick = { vm.setFastSeek(false) }, label = { Text("逐帧精准定位") })
            }
            Text(if (state.fastSeek) "快进直接对齐最近关键帧，跳过中间帧解码，消除高码率卡顿。" else "精确定位到所选时间点，需要解码关键帧至目标点之间的全部帧。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("画面比例 · ${vm.resizeModeLabel(state.resizeMode)}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT, onClick = { vm.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT) }, label = { Text("适应屏幕 (Fit)") })
                FilterChip(selected = state.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM, onClick = { vm.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM) }, label = { Text("裁剪铺满 (Crop)") })
                FilterChip(selected = state.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL, onClick = { vm.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL) }, label = { Text("拉伸全屏 (Fill)") })
            }
            if (state.audioTracks.size > 1) {
                Text("音频音轨 · ${state.audioTracks.firstOrNull { it.selected }?.label ?: "默认"}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.audioTracks.forEach { track ->
                        FilterChip(
                            selected = track.selected,
                            onClick = { vm.selectAudioTrack(track) },
                            label = { Text(track.label) }
                        )
                    }
                }
            }
            if (state.item?.let { !vm.isFollow(it) } == true) {
                if (state.direct) {
                    OutlinedButton(onClick = { vm.compatibility(); settings = false }, Modifier.fillMaxWidth()) { Text("切换 HLS 转码兼容播放") }
                } else {
                    OutlinedButton(onClick = { vm.direct(); settings = false }, Modifier.fillMaxWidth()) { Text("切回直连原画（硬件解码·秒开）") }
                }
            }
            OutlinedButton(onClick = { settings = false; menu = "subtitles" }, Modifier.fillMaxWidth()) { Text("字幕 · ${state.subtitleLabel}") }
            OutlinedButton(onClick = { subtitlePicker.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text("从手机导入字幕") }
            Text("字幕字号 · ${state.subtitleSize.toInt()}")
            Slider(state.subtitleSize, { vm.subtitleStyle(size = it) }, valueRange = 12f..44f)
            FlowRow { TextButton(onClick = { vm.subtitleStyle(color = android.graphics.Color.WHITE) }) { Text("白色") }; TextButton(onClick = { vm.subtitleStyle(color = android.graphics.Color.YELLOW) }) { Text("黄色") }; TextButton(onClick = { vm.subtitleStyle(background = if (state.subtitleBackground == 0) 0x99000000.toInt() else 0) }) { Text("字幕背景") }; TextButton(onClick = { vm.subtitleStyle(edge = if (state.subtitleEdge == 0) 1 else 0) }) { Text("描边") } }
            FlowRow { TextButton(onClick = { vm.subtitleStyle(font = "sans-serif") }) { Text("默认字体") }; TextButton(onClick = { vm.subtitleStyle(font = "serif") }) { Text("衬线字体") }; TextButton(onClick = { vm.subtitleStyle(font = "monospace") }) { Text("等宽字体") } }
            if (state.qualities.isNotEmpty()) { Text("视频画质"); state.qualities.forEach { row -> TextButton(onClick = { vm.quality(row.optInt("height")); settings = false }) { Text(row.text("label")) } } }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (menu == "subtitles") SubtitleSheet(vm, state, { menu = null })
    if (menu == "versions") ModalBottomSheet(onDismissRequest = { menu = null }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("版本与分卷", style = MaterialTheme.typography.titleLarge)
            if (state.editions.isEmpty() && state.parts.isEmpty()) Text("当前作品没有其他版本或分卷。", Modifier.padding(vertical = 24.dp))
            (state.editions + state.parts).distinctBy { it.text("id") }.forEach { row -> TextButton(onClick = { menu = null; onSwitch(row) }, Modifier.fillMaxWidth()) { Text(titleOf(row)) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SubtitleSheet(vm: PlaybackViewModel, state: PlayingState, onClose: () -> Unit) {
    var query by remember { mutableStateOf(state.item?.text("code").orEmpty()) }
    var results by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var files by remember { mutableStateOf<Pair<String, List<JSONObject>>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("选择字幕", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { vm.chooseSubtitle(null); onClose() }) { Text("关闭字幕") }
            state.subtitles.forEach { row -> TextButton(onClick = { if (row.optBoolean("playable")) { vm.chooseSubtitle(row.getString("src"), row.text("label", "name")); onClose() } }, enabled = row.optBoolean("playable")) { Text(row.text("label", "name") + row.text("note").let { if (it.isBlank()) "" else " · $it" }) } }
            OutlinedTextField(query, { query = it }, label = { Text("在线字幕 · 番号或标题") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { loading = true; error = null; scope.launch { runCatching { vm.searchSubtitles(query) }.onSuccess { result -> results = (result.optJSONArray("items") ?: result.optJSONArray("results"))?.objects().orEmpty() }.onFailure { error = it.localizedMessage }; loading = false } }, enabled = !loading) { Text(if (loading) "正在搜索…" else "搜索简繁中文字幕") }
            results.forEach { row -> TextButton(onClick = { scope.launch { runCatching { vm.subtitleFiles(row.text("id")) }.onSuccess { result -> files = row.text("id") to (result.optJSONArray("files") ?: result.optJSONArray("items"))?.objects().orEmpty() }.onFailure { error = it.localizedMessage } } }) { Text(row.text("title", "name")) } }
            files?.let { (id, rows) -> rows.forEachIndexed { index, row -> TextButton(onClick = { vm.chooseSubtitle("/api/subtitles/subtitlecat/$id/${row.optInt("index", index)}", row.text("label", "name", "language")); onClose() }) { Text(row.text("label", "name", "language")) } } }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(20.dp))
        }
    }
}
