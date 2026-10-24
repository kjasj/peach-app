package video.peach.android

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import video.peach.android.player.*
import video.peach.android.ui.*
import video.peach.android.data.displayName
import video.peach.android.data.readBounded
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels()
    private val playback: PlaybackViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by app.state.collectAsStateWithLifecycle()
            val playing by playback.state.collectAsStateWithLifecycle()
            val images = remember(state.connected?.id) {
                coil.ImageLoader.Builder(this)
                    .apply { app.client?.let { okHttpClient(it.readHttp) } }
                    .diskCache { coil.disk.DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(250L * 1024 * 1024).build() }
                    .memoryCache { coil.memory.MemoryCache.Builder(this).maxSizePercent(0.25).build() }
                    .respectCacheHeaders(false)
                    .build()
            }
            DisposableEffect(images) { onDispose { images.shutdown() } }
            CompositionLocalProvider(LocalPeachImages provides images) { PeachTheme(state.theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PeachApp(app, state, playback, playing, ::enterPip)
                }
            } }
            LaunchedEffect(playing.full) {
                requestedOrientation = if (playing.full) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = if (playing.full)
                            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        else
                            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    }
                }
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (playing.full) { controller.hide(WindowInsetsCompat.Type.systemBars()); controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
                else controller.show(WindowInsetsCompat.Type.systemBars())
            }
            LaunchedEffect(playing.item == null) { if (playing.item == null) window.attributes = window.attributes.apply { screenBrightness = -1f } }
        }
    }
    internal fun enterPip() {
        if (playback.player == null || !packageManager.hasSystemFeature("android.software.picture_in_picture")) return
        playback.pip(true)
        if (!enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())) playback.pip(false)
    }
    override fun onUserLeaveHint() { super.onUserLeaveHint(); if (playback.state.value.playing) enterPip() }
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) { super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig); playback.pip(isInPictureInPictureMode) }
    override fun onPause() { playback.releaseGesture(); super.onPause() }
    override fun onStart() { super.onStart(); app.foreground() }
    override fun onStop() { app.background(); if (!isInPictureInPictureMode) playback.background(); super.onStop() }
}

private data class Destination(val key: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val destinations = listOf(Destination("home", "首页", Icons.Rounded.VideoLibrary), Destination("follow", "关注", Icons.Rounded.Subscriptions), Destination("jav", "JAV", Icons.Rounded.Movie), Destination("manage", "管理", Icons.Rounded.Dashboard))

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PeachApp(vm: AppViewModel, state: ScreenState, playback: PlaybackViewModel, playing: PlayingState, onPip: () -> Unit) {
    var connection by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var features by remember { mutableStateOf(false) }
    var navigation by remember { mutableStateOf(false) }
    val catalogMemory = rememberSaveableStateHolder()
    val catalogKey = "${state.connected?.id}:${state.listGeneration}"
    DisposableEffect(catalogKey) {
        onDispose { catalogMemory.removeState(catalogKey) }
    }
    val snackbar = remember { SnackbarHostState() }
    val start: (JSONObject) -> Unit = { row -> vm.client?.let { playback.open(it, row, state.fixedSpeed, state.boost) } }
    LaunchedEffect(playing.speed) { if (playing.item != null && playing.speed != state.fixedSpeed) vm.preferences(speed = playing.speed) }
    BackHandler(playing.item != null || state.item != null || state.canGoBack) {
        when {
            playing.full -> playback.full(false)
            playing.item != null -> {
                playback.close()
                if (state.item != null) vm.closeItem()
            }
            state.item != null -> vm.closeItem()
            state.canGoBack -> vm.back()
            else -> playback.close()
        }
    }
    LaunchedEffect(state.connected?.id) { if (playing.item != null) playback.close() }
    LaunchedEffect(state.notice) { state.notice?.let { snackbar.showSnackbar(it); vm.dismissNotice() } }
    if (!state.ready) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    if (playing.item != null && (playing.full || playing.pip)) { PlayerPanel(playback, playing, Modifier.fillMaxSize(), onPip, start); return }
    if (state.connected == null) { ConnectionScreen(vm, state); return }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
            TopAppBar(title = { Column { Text("Peach", style = MaterialTheme.typography.titleLarge); Text(state.connected.name + if (state.connected.url.startsWith("http://")) " · 未加密" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, navigationIcon = {
                if (state.canGoBack) IconButton(onClick = vm::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回上一层") }
                else IconButton(onClick = { navigation = true }) { Icon(Icons.Rounded.Menu, "导航菜单") }
            }, actions = {
                IconButton(onClick = { connection = true }) { Icon(Icons.Rounded.Dns, "切换服务器") }
                IconButton(onClick = { features = true }) { Icon(Icons.Rounded.Apps, "全部功能") }
                IconButton(onClick = { settings = true }) { Icon(Icons.Rounded.Settings, "客户端设置") }
            })
        }, bottomBar = {
            if (!wide) NavigationBar {
                destinations.forEach { target -> NavigationBarItem(selected = state.section == target.key, onClick = { vm.select(target.key) }, icon = { Icon(target.icon, target.title) }, label = { Text(target.title) }) }
            }
        }) { padding ->
            Row(Modifier.padding(padding)) {
                if (wide) NavigationRail {
                    destinations.forEach { target -> NavigationRailItem(selected = state.section == target.key, onClick = { vm.select(target.key) }, icon = { Icon(target.icon, target.title) }, label = { Text(target.title) }) }
                }
                if (wide && state.item != null) Box(Modifier.weight(1f)) {
                    catalogMemory.SaveableStateProvider(catalogKey) { CatalogScreen(vm, state, playback) }
                }
                Column(Modifier.weight(if (wide && state.item != null) 1.1f else 1f)) {
                    if (playing.item != null && !playing.mini) {
                        PlayerPanel(playback, playing, Modifier.fillMaxWidth().aspectRatio(16f / 9f), onPip, start)
                        playing.error?.let { Notice(it, true) }
                    }
                    if (state.item != null) DetailScreen(vm, state, playback, start)
                    else catalogMemory.SaveableStateProvider(catalogKey) { CatalogScreen(vm, state, playback) }
                }
            }
        }
        if (playing.item != null && playing.mini) Column(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = if (wide) 16.dp else 96.dp).width(276.dp).background(MaterialTheme.colorScheme.surface)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("正在播放", Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.labelMedium); IconButton(onClick = { playback.mini(false) }) { Icon(Icons.Rounded.OpenInFull, "展开播放器") }; IconButton(onClick = playback::close) { Icon(Icons.Rounded.Close, "关闭小窗") } }
            PlayerPanel(playback, playing, Modifier.fillMaxWidth().aspectRatio(16f / 9f), onPip, start)
        }
    }
    if (connection) ModalBottomSheet(onDismissRequest = { connection = false }) { ConnectionScreen(vm, state, { connection = false }) }
    if (features) FeatureSheet(vm, state.group, { features = false }) { path -> features = false; vm.navigate(path) }
    if (settings) ClientSettings(vm, state, { settings = false })
    if (navigation) NavigationMenu(state.section, { navigation = false }, vm::select, { features = true })
    state.actionResult?.let { result ->
        ModalBottomSheet(onDismissRequest = vm::dismissResult) {
            Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("操作结果与预览", style = MaterialTheme.typography.headlineSmall)
                Text("执行前请核对变化范围；服务器路径是电脑上的文件位置。", style = MaterialTheme.typography.bodySmall)
                RecordView(result, vm.client)
                Button(onClick = vm::dismissResult, Modifier.fillMaxWidth()) { Text("已查看，返回") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable private fun ClientSettings(vm: AppViewModel, state: ScreenState, close: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch {
            importing = true
            runCatching {
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { context.contentResolver.readBounded(it, 64 * 1024 * 1024) }
                (vm.client ?: error("请先连接服务器")).importHistory(context.contentResolver.displayName(it, "bookmarks.html"), bytes)
            }.onSuccess { vm.navigate("/api/taste"); close() }.onFailure { vm.error(it.localizedMessage ?: "导入失败") }
            importing = false
        } }
    }
    ModalBottomSheet(onDismissRequest = close) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("客户端设置", style = MaterialTheme.typography.headlineSmall)
            Text("外观", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (value, title) -> FilterChip(selected = state.theme == value, onClick = { vm.preferences(theme = value) }, label = { Text(title) }) } }
            Text("长按临时倍速", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(1.5f, 2f, 3f).forEach { value -> FilterChip(selected = state.boost == value, onClick = { vm.preferences(boost = value) }, label = { Text("$value×") }) } }
            Text("长按 350 毫秒加速，松手恢复。左右滑动快进；左侧调亮度，右侧调音量。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !importing, modifier = Modifier.fillMaxWidth()) { Text(if (importing) "正在导入…" else "导入浏览器书签或历史导出文件") }
            Notice("手机不会读取电脑上的浏览器历史。可导入导出文件；系统设置、自启、更新及卸载需在运行 Peach 的电脑上完成。")
            Text("Peach Android ${BuildConfig.VERSION_NAME} · 服务端基线 ${vm.registry.commit.take(8)}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { vm.logout(); close() }) { Text("退出当前服务器登录") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
