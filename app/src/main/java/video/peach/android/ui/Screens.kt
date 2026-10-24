package video.peach.android.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import video.peach.android.*
import video.peach.android.R
import video.peach.android.data.*
import video.peach.android.player.PlaybackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ConnectionScreen(vm: AppViewModel, state: ScreenState, close: (() -> Unit)? = null) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ca by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ServerProfile?>(null) }
    var inputError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runCatching { context.contentResolver.readBounded(it, 1024 * 1024).toString(Charsets.US_ASCII) }.onSuccess { ca = it }.onFailure { inputError = "无法读取 CA 证书" } }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.peach_logo), "Peach", Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp)); Text("Peach 蜜桃", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f)); close?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Close, "关闭连接设置") } }
        }
        Text("你的馆藏，随手可看", style = MaterialTheme.typography.headlineSmall)
        Text("连接电脑上的 Peach，浏览馆藏、追更和管理资源。手机与电脑需要能够互相访问。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.profiles.forEach { profile ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { vm.connect(profile, null); close?.invoke() }) { Text(profile.name, fontWeight = FontWeight.Medium); Text(profile.url, style = MaterialTheme.typography.bodySmall) }
                    IconButton(onClick = { editing = profile; name = profile.name; url = profile.url; ca = profile.caPem; password = "" }) { Icon(Icons.Rounded.Edit, "重新登录或编辑服务器") }
                    IconButton(onClick = { vm.forget(profile) }) { Icon(Icons.Rounded.DeleteOutline, "移除此服务器") }
                }
            }
        }
        HorizontalDivider()
        Text(if (editing == null) "添加服务器" else "编辑 ${editing!!.name}", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text("服务器名称") }, placeholder = { Text("家里的 Peach") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(url, { url = it; inputError = null }, label = { Text("服务器地址") }, placeholder = { Text("http://电脑的局域网地址:8900") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (url.startsWith("http://")) Notice("当前连接未加密，请在可信局域网内使用。")
        OutlinedTextField(password, { password = it }, label = { Text("访问口令") }, supportingText = { Text("口令仅用于登录，不会保存明文。") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedButton(onClick = { launcher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.VerifiedUser, null); Spacer(Modifier.width(8.dp)); Text(if (ca.isBlank()) "导入私有 CA 证书（可选）" else "已导入 CA 证书，点击替换") }
        if (ca.isNotEmpty()) TextButton(onClick = { ca = "" }) { Text("移除导入的 CA") }
        (inputError ?: state.error)?.let { Notice(it, error = true) }
        Button(onClick = {
            runCatching { vm.profile(name, url, ca, editing?.id) }.onSuccess { vm.connect(it, password) }.onFailure { inputError = it.localizedMessage ?: "请检查服务器地址" }
        }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !state.busy && url.isNotBlank()) {
            if (state.busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(if (state.busy) "正在验证连接…" else "连接 Peach")
        }
        Text("Mac 端需在首次设置中允许局域网访问。系统配置、自启和卸载等本机操作仍需在电脑完成。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable fun CatalogScreen(vm: AppViewModel, state: ScreenState, player: PlaybackViewModel) {
    val payload = state.payload
    var filter by remember { mutableStateOf(false) }
    var search by remember(state.page, state.section, state.query["q"]) { mutableStateOf(state.query["q"].orEmpty()) }
    var chosen by remember { mutableStateOf<Pair<Endpoint, JSONObject?>?>(null) }
    var selectedRecord by remember(state.page) { mutableStateOf<JSONObject?>(null) }
    var showSections by remember { mutableStateOf(false) }
    var gallery by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val endpoint = vm.registry.get(state.page)
    val actions = vm.registry.actions(state.page)
    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AppNavigation.primary, key = { it.key }, contentType = { "nav_chip" }) { entry ->
                FilterChip(selected = state.section == entry.key, onClick = { vm.select(entry.key) }, label = { Text(entry.title) })
            }
        }
        if (state.section == "jav") Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("馆藏" to "/api/items", "发现" to "/api/jav/discoveries", "想要" to "/api/wants").forEach { (label, path) ->
                FilterChip(selected = state.page == path, onClick = { vm.navigate(path, if (path == "/api/items") mapOf("jav" to "1") else emptyMap(), section = "jav") }, label = { Text(label) })
            }
        }
        if (state.section == "follow") LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                items = listOf("动态" to "/api/follow", "创作者" to "/api/follow/authors", "来源与分组" to "/api/follow?summary=1", "采集计划" to "/api/follow/schedule"),
                key = { it.second },
                contentType = { "follow_chip" }
            ) { (label, destination) ->
                val summary = destination.endsWith("?summary=1")
                val path = destination.substringBefore('?')
                FilterChip(selected = state.page == path && (state.query["summary"] == "1") == summary,
                    onClick = { vm.navigate(path, if (summary) mapOf("summary" to "1") else emptyMap(), section = "follow") }, label = { Text(label) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(search, { search = it }, placeholder = { Text("搜索${endpoint?.title ?: "馆藏"}") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), trailingIcon = {
                IconButton(onClick = { vm.refresh(state.query + ("q" to search) + ("offset" to "0")) }) { Icon(Icons.Rounded.Search, "执行搜索") }
            })
            IconButton(onClick = { filter = true }) { Icon(Icons.Rounded.Tune, "筛选与排序") }
            IconButton(onClick = { showSections = true }) { Icon(Icons.Rounded.GridView, "所有功能") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            val title = if (state.page in listOf("/api/index", "/api/items")) AppNavigation.title(state.section) ?: endpoint?.title else endpoint?.title
            Text(title ?: "馆藏", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            payload?.takeIf { (it.has("items") || it.has("groups") || it.has("rows")) && state.query["summary"] != "1" }?.let {
                val total = (it.opt("total") as? Number)?.toInt()
                Text(if (total != null) "已显示 ${state.loadedCount} / $total" else "已显示 ${state.loadedCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.page == "/api/items") IconButton(onClick = { vm.toggleWall() }) { Icon(if (state.wall) Icons.Rounded.ViewList else Icons.Rounded.ViewModule, "切换封面墙") }
            IconButton(onClick = { vm.refresh() }) { Icon(Icons.Rounded.Refresh, "刷新") }
        }
        if (state.error != null) Box(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) { Notice(state.error, true, "重试", { vm.refresh() }) }
        if (state.stale) Box(Modifier.padding(horizontal = 18.dp)) { Notice("当前显示上次缓存，操作前请恢复服务器连接。") }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (actions.isNotEmpty() && state.page != "/api/items") androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(actions, key = { it.path }, contentType = { "action_chip" }) { action -> AssistChip(onClick = { chosen = action to payload }, label = { Text(action.title) }) }
        }
        if (payload == null && !state.busy) { EmptyState(retry = { vm.refresh() }); return@Column }
        if (payload != null && state.page == "/api/tasks") { TasksContent(payload, vm, Modifier.weight(1f)); return@Column }
        if (payload != null && state.page == "/api/follow" && state.query["summary"] == "1") {
            FollowSources(payload, vm, Modifier.weight(1f)) { selectedRecord = it }; return@Column
        }
        val rows = remember(payload) { primaryRows(payload, state.page) }
        val isCatalog = state.page in listOf("/api/items", "/api/related", "/api/tops", "/api/parts", "/api/editions", "/api/jav/discoveries", "/api/follow", "/api/feeds/discoveries")
        val isIndex = state.page in listOf("/api/index", "/api/follow/authors")
        if (rows.isNotEmpty() && (isCatalog || isIndex)) {
            val grid = rememberLazyStaggeredGridState()
            val nearEnd by remember(grid, rows.size) { derivedStateOf { (grid.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1) >= rows.size - 8 } }
            LaunchedEffect(nearEnd, state.nextOffset, state.busy, state.moreError, state.stale) {
                if (nearEnd && !state.busy && state.moreError == null && !state.stale) vm.loadNext()
            }
            LazyVerticalStaggeredGrid(
                state = grid,
                columns = StaggeredGridCells.Adaptive(if (state.wall) 128.dp else 148.dp),
                contentPadding = PaddingValues(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier.weight(1f).testTag("catalog-grid")
            ) {
                items(
                    items = rows,
                    key = { row -> catalogItemKey(row, isIndex) },
                    contentType = { if (isIndex) "index_card" else "media_card" }
                ) { row ->
                    if (isIndex) IndexCard(row, if (state.page == "/api/follow/authors") "creators" else state.query["kind"].orEmpty(), vm)
                    else MediaCard(row, vm.client, onLongClick = { selectedRecord = row }, onClick = {
                        when {
                            state.page == "/api/follow" || row.optBoolean("_follow") -> selectedRecord = row.apply { put("_follow", true) }
                            row.has("location") && row.has("id") -> vm.openItem(row)
                            else -> selectedRecord = row
                        }
                    })
                }
                item(span = StaggeredGridItemSpan.FullLine, key = "catalog_paging_footer", contentType = "paging_footer") {
                    PagingFooter(state, vm)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.page == "/api/follow") item(key = "empty_follow", contentType = "empty_state") {
                    EmptyState("暂无关注动态", "可切换到“来源与分组”检查关注来源，或添加来源后检查更新。")
                } else if (payload != null) item(key = "record_view", contentType = "record_view") {
                    RecordView(payload, vm.client, onRecord = { selectedRecord = it })
                }
                if (payload != null && payload.length() <= 1) item(key = "empty_filter", contentType = "empty_state") {
                    EmptyState(message = "当前筛选没有匹配的内容。")
                }
                if (state.nextOffset != null) item(key = "paging_footer", contentType = "paging_footer") {
                    PagingFooter(state, vm)
                }
            }
        }
    }
    if (showSections) FeatureSheet(vm, state.group, { showSections = false }) { path -> showSections = false; vm.navigate(path) }
    if (filter && endpoint != null) FilterDialog(endpoint, state.query, { filter = false }) { filter = false; vm.refresh(it) }
    chosen?.let { (action, seed) ->
        val formSeed = remember(action, seed) { vm.actionSeed(action, seed) }
        ActionDialog(action, formSeed, state.writing, state.error, { chosen = null }, vm.registry.credentialGuides) { body -> vm.send(action, body) { chosen = null } }
    }
    selectedRecord?.let { record -> RecordSheet(record, vm.client, actions, { selectedRecord = null }, { action -> selectedRecord = null; chosen = action to record }, { selected ->
        selectedRecord = null
        if (selected.text("media_kind") == "image") gallery = galleryImages(selected)
        else if (selected.optBoolean("_follow")) vm.client?.let { player.open(it, selected, state.fixedSpeed, state.boost) } else vm.openItem(selected)
    }) }
    if (gallery.isNotEmpty()) Gallery(gallery, vm.client) { gallery = emptyList() }
}

@Composable private fun PagingFooter(state: ScreenState, vm: AppViewModel) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.moreError != null) Text(state.moreError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        when {
            state.busy -> CircularProgressIndicator(Modifier.size(24.dp))
            state.nextOffset != null -> OutlinedButton(onClick = vm::loadNext, enabled = !state.stale) { Text(if (state.moreError != null) "重试加载" else "加载更多") }
            else -> Text("已全部加载", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun IndexCard(row: JSONObject, index: String, vm: AppViewModel) {
    val kind = AppNavigation.entityKind(index)
    val name = row.text("k", "canonical_name", "name")
    OutlinedCard(onClick = {
        if (index == "creators") vm.navigate("/api/follow", mapOf("author" to row.text("key")), section = "follow")
        else vm.navigate("/api/items", mapOf(kind to name), section = index)
    }) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (index != "tags") {
                val image = row.text("entity_id").takeIf { row.flag("has_image") }?.let { "/entity-image?kind=$kind&id=$it" }
                    ?: row.text("rep").takeIf { it.isNotBlank() }?.let { "/thumb?id=$it" }
                if (image != null) PeachImage(vm.client, image, name, Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text("${row.optInt("n")} " + if (index == "creators") "项更新" else "部作品", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = {
                val query = mapOf("kind" to kind, "name" to name) + row.text("entity_id").takeIf { it.isNotBlank() }?.let { mapOf("id" to it) }.orEmpty()
                vm.navigate("/api/entity", query, section = index)
            }) { Text("查看资料") }
        }
    }
}

private fun catalogItemKey(row: JSONObject, isIndex: Boolean): String {
    val prefix = if (isIndex) "idx" else "media"
    val id = row.text("id", "entity_id", "release_key", "k", "canonical_name", "name", "code", "path")
    return if (id.isNotBlank()) "$prefix:$id" else "$prefix:${row.hashCode()}"
}

fun primaryRows(payload: JSONObject?, page: String): List<JSONObject> {
    if (payload == null) return emptyList()
    val primary = payload.optJSONArray("items")?.objects().orEmpty()
    val list = if (primary.isNotEmpty()) {
        if (page == "/api/follow") primary.map { JSONObject(it.toString()).put("_follow", true) } else primary
    } else if (page == "/api/follow") {
        payload.optJSONArray("groups")?.objects().orEmpty().flatMap { group ->
            val members = listOfNotNull(group.optJSONObject("primary")) +
                group.optJSONArray("variants")?.objects().orEmpty() + group.optJSONArray("duplicates")?.objects().orEmpty()
            members.distinctBy { it.text("id") }.map { JSONObject(it.toString()).put("_follow", true).put("_release_key", group.text("release_key")) }
        }
    } else {
        payload.optJSONArray("rows")?.objects().orEmpty()
    }
    return list.distinctBy { it.text("id", "entity_id", "release_key", "k", "canonical_name", "name", "code", "path").ifBlank { it.toString() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FeatureSheet(vm: AppViewModel, group: String, onClose: () -> Unit, onSelect: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("全部功能", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(query, { query = it }, placeholder = { Text("查找功能") }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), leadingIcon = { Icon(Icons.Rounded.Search, null) })
            LazyColumn(Modifier.fillMaxHeight(.8f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((key, title) in listOf("library" to "馆藏与资料", "discover" to "发现与口味", "follow" to "关注与订阅", "manage" to "管理与维护")) {
                    val features = vm.registry.endpoints.filter { it.method == "GET" && it.group == key && (query.isBlank() || it.title.contains(query)) }
                    if (features.isNotEmpty()) {
                        item(key = "header_$key", contentType = "group_header") { Text(title, Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleSmall) }
                        items(features, key = { "feature_${it.path}" }, contentType = { "feature_item" }) { feature -> ListItem(headlineContent = { Text(feature.title) }, supportingContent = if (feature.localOnly) ({ Text("此操作需在服务器本机完成") }) else null, leadingContent = { Icon(if (feature.localOnly) Icons.Rounded.Computer else Icons.Rounded.FolderOpen, null) }, modifier = Modifier.clickable { onSelect(feature.path) }) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RecordSheet(record: JSONObject, client: PeachClient?, actions: List<Endpoint>, onClose: () -> Unit, onAction: (Endpoint) -> Unit, onPlay: (JSONObject) -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(titleOf(record), style = MaterialTheme.typography.titleLarge)
            if (record.has("id") && (record.has("location") || record.optBoolean("_follow"))) Button(onClick = { onPlay(record) }, Modifier.fillMaxWidth()) { Icon(Icons.Rounded.PlayArrow, null); Text(if (record.text("media_kind") == "image") "查看图片集" else "播放") }
            val attachments = record.optJSONArray("media_items")?.objects().orEmpty()
            attachments.filter { it.text("media_kind") == "video" }.forEach { media ->
                OutlinedButton(onClick = { onPlay(JSONObject(record.toString()).put("_media_index", media.optInt("index")).put("media_kind", "video")) }, Modifier.fillMaxWidth()) {
                    Text("播放附件 ${media.optInt("index") + 1}" + media.text("label", "name").takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty())
                }
            }
            if (attachments.any { it.text("media_kind") == "image" } && record.text("media_kind") != "image") OutlinedButton(onClick = { onPlay(JSONObject(record.toString()).put("media_kind", "image")) }, Modifier.fillMaxWidth()) { Text("查看图片附件") }
            actions.forEach { action -> OutlinedButton(onClick = { onAction(action) }, Modifier.fillMaxWidth()) { Text(action.title) } }
            RecordView(record, client)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun DetailScreen(vm: AppViewModel, state: ScreenState, playback: PlaybackViewModel, onPlay: (JSONObject) -> Unit) {
    val item = state.item ?: return
    var action by remember(item) { mutableStateOf<Endpoint?>(null) }
    var gallery by remember(item) { mutableStateOf(false) }
    var crop by remember(item) { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var externalBusy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { playback.close(); vm.closeItem() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回馆藏") }; Text("作品详情", style = MaterialTheme.typography.titleMedium) }
        PeachImage(vm.client, imageOf(item), titleOf(item), Modifier.fillMaxWidth().aspectRatio(if (Artwork.official(item) && item.flag("is_jav")) .75f else 1.7f), scale = androidx.compose.ui.layout.ContentScale.Fit, cover = item.takeIf(Artwork::official))
        Text(item.text("code"), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
        Text(titleOf(item), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (state.error != null) Notice(state.error, true)
        Button(onClick = { onPlay(item) }, Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("开始播放") }
        if (item.optString("location") != "online") OutlinedButton(onClick = {
            val client = vm.client ?: return@OutlinedButton
            externalBusy = true
            scope.launch {
                runCatching {
                    val ticket = client.post("/api/external-playback?id=${item.text("id")}")
                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(client.resolve(ticket.getString("url"))), "video/*")
                    context.startActivity(Intent.createChooser(intent, "选择播放器"))
                }.onFailure { vm.error(it.localizedMessage ?: "没有可用的外部播放器") }
                externalBusy = false
            }
        }, enabled = !externalBusy, modifier = Modifier.fillMaxWidth()) { Text(if (externalBusy) "正在获取临时播放链接…" else "使用其他播放器") }
        if (galleryImages(item).isNotEmpty()) OutlinedButton(onClick = { gallery = true }, Modifier.fillMaxWidth()) { Text("查看图片集") }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { vm.registry.post("/api/watch-later")?.let { vm.send(it, JSONObject().put("id", item.getLong("id"))) { result -> item.put("watch_later", result.optBoolean("watch_later")); vm.openItem(item) } } }, label = { Text(if (item.flag("watch_later")) "移出稍后看" else "稍后看") }, leadingIcon = { Icon(Icons.Rounded.BookmarkBorder, null, Modifier.size(18.dp)) })
            AssistChip(onClick = { vm.registry.post("/api/preference")?.let { vm.send(it, JSONObject().put("id", item.getLong("id")).put("liked", !item.flag("liked"))) { vm.openItem(item) } } }, label = { Text(if (item.flag("liked")) "取消喜欢" else "喜欢") }, leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, null, Modifier.size(18.dp)) })
            AssistChip(onClick = { vm.navigate("/api/editions", mapOf("id" to item.text("id"))) }, label = { Text("版本") })
            AssistChip(onClick = { vm.navigate("/api/parts", mapOf("id" to item.text("id"))) }, label = { Text("分卷") })
            AssistChip(onClick = { vm.navigate("/api/related", mapOf("id" to item.text("id"))) }, label = { Text("相关推荐") })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.text("code").isNotBlank()) OutlinedButton(onClick = { crop = true }) { Text("封面取景") }
            vm.registry.actions("/api/item").forEach { endpoint -> OutlinedButton(onClick = { action = endpoint }) { Text(endpoint.title) } }
        }
        HorizontalDivider(); Text("作品资料", style = MaterialTheme.typography.titleMedium)
        RecordView(item, vm.client, onRecord = { record -> if (record.has("id") && record.has("location")) vm.openItem(record) })
        Spacer(Modifier.height(30.dp))
    }
    action?.let { endpoint -> ActionDialog(endpoint, item, state.writing, state.error, { action = null }, vm.registry.credentialGuides) { body -> vm.send(endpoint, body) { action = null; vm.openItem(item) } } }
    if (gallery) Gallery(galleryImages(item), vm.client) { gallery = false }
    if (crop && vm.client != null) CoverCrop(vm.client!!, item.text("code")) { crop = false }
}
