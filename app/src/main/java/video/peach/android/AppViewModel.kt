package video.peach.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import video.peach.android.data.*
import java.util.UUID

data class ScreenState(
    val ready: Boolean = false, val profiles: List<ServerProfile> = emptyList(), val connected: ServerProfile? = null,
    val group: String = "library", val page: String = "/api/items", val query: Map<String, String> = emptyMap(),
    val payload: JSONObject? = null, val busy: Boolean = false, val writing: Boolean = false,
    val error: String? = null, val notice: String? = null, val theme: String = "system",
    val boost: Float = 2f, val fixedSpeed: Float = 1f, val stale: Boolean = false,
    val item: JSONObject? = null, val actionResult: JSONObject? = null, val wall: Boolean = false,
    val section: String = "home", val loadedCount: Int = 0, val nextOffset: Int? = null,
    val moreError: String? = null, val listGeneration: Int = 0, val canGoBack: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PeachApplication
    val registry = ProtocolRegistry(app)
    private val mutable = MutableStateFlow(ScreenState())
    val state = mutable.asStateFlow()
    var client: PeachClient? = null
        private set
    private var loading: Job? = null
    private var polling: Job? = null
    private var detailLoading: Job? = null
    private var organizePreview: JSONObject? = null
    private val history = ArrayDeque<ScreenState>()
    init {
        viewModelScope.launch {
            val profiles = app.preferences.servers()
            mutable.update { it.copy(ready = true, profiles = profiles, theme = app.preferences.get("theme", "system"), boost = app.preferences.get("boost", "2").toFloatOrNull() ?: 2f, fixedSpeed = app.preferences.get("fixedSpeed", "1").toFloatOrNull() ?: 1f) }
            val active = app.preferences.get("active")
            profiles.firstOrNull { it.id == active }?.let { connect(it, null) }
        }
    }
    fun connect(profile: ServerProfile, password: String?) {
        if (mutable.value.writing) { error("正在提交服务器操作，请完成后切换连接"); return }
        loading?.cancel(); polling?.cancel(); detailLoading?.cancel()
        mutable.update { it.copy(busy = true, error = null) }
        loading = viewModelScope.launch {
            try {
                val candidate = PeachClient(profile, app.secrets)
                candidate.get("/healthz")
                if (password != null) candidate.login(password) else candidate.get("/api/libraries")
                val profiles = mutable.value.profiles.filterNot { it.id == profile.id } + profile
                app.preferences.saveServers(profiles); app.preferences.put("active", profile.id)
                client = candidate
                history.clear()
                organizePreview = null
                mutable.update { it.copy(connected = profile, profiles = profiles, busy = false, error = null, item = null, actionResult = null, query = emptyMap(), page = "/api/items", group = "library", section = "home", payload = null, loadedCount = 0, nextOffset = null, canGoBack = false) }
                refresh()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(busy = false, error = error.localizedMessage ?: "无法连接服务器") }
            }
        }
    }
    fun profile(name: String, address: String, ca: String, id: String? = null): ServerProfile {
        val normalized = PeachClient.normalizeAddress(address)
        val identity = id?.takeIf { existing -> mutable.value.profiles.any { it.id == existing && it.url == normalized } } ?: UUID.randomUUID().toString()
        return ServerProfile(identity, name.ifBlank { "我的 Peach" }, normalized, ca)
    }
    fun forget(profile: ServerProfile) {
        if (mutable.value.writing) { error("正在提交服务器操作，请完成后移除连接"); return }
        viewModelScope.launch {
        if (mutable.value.connected?.id == profile.id) disconnect()
        app.secrets.write("cookies:${profile.id}", "")
        app.database.dao().clear(profile.id)
        val profiles = mutable.value.profiles.filterNot { it.id == profile.id }
        app.preferences.saveServers(profiles); mutable.update { it.copy(profiles = profiles) }
        }
    }
    fun disconnect() {
        if (mutable.value.writing) { error("正在提交服务器操作，请完成后退出连接"); return }
        loading?.cancel(); polling?.cancel(); detailLoading?.cancel(); client = null; organizePreview = null
        history.clear()
        mutable.update { it.copy(connected = null, payload = null, item = null, actionResult = null, busy = false, error = null, canGoBack = false) }
        viewModelScope.launch { app.preferences.put("active", "") }
    }
    fun logout() {
        if (mutable.value.writing) { error("正在提交服务器操作，请完成后退出登录"); return }
        client?.logout(); disconnect()
    }
    fun select(key: String) {
        val section = (AppNavigation.primary + AppNavigation.extras).firstOrNull { it.key == key } ?: AppNavigation.primary.first()
        history.clear()
        navigate(section.path, section.query, section = section.key, remember = false)
        if (key == "immerse") mutable.update { it.copy(wall = true) }
    }
    fun navigate(path: String, query: Map<String, String> = emptyMap(), group: String = registry.get(path)?.group ?: mutable.value.group, section: String = AppNavigation.infer(path, query), remember: Boolean = true) {
        detailLoading?.cancel()
        if (remember && (mutable.value.page != path || mutable.value.query != query || mutable.value.item != null)) {
            if (history.size >= 40) history.removeFirst()
            history.addLast(mutable.value.copy(busy = false, error = null, actionResult = null))
        }
        mutable.update { it.copy(page = path, query = query, group = group, section = section, item = null, payload = null, error = null, actionResult = null, wall = false, loadedCount = 0, nextOffset = null, moreError = null, canGoBack = history.isNotEmpty()) }
        refresh()
    }
    fun back() {
        if (mutable.value.item != null) { closeItem(); return }
        val previous = history.removeLastOrNull() ?: return
        loading?.cancel(); polling?.cancel(); detailLoading?.cancel()
        val current = mutable.value
        mutable.value = previous.copy(canGoBack = history.isNotEmpty(), listGeneration = current.listGeneration + 1,
            theme = current.theme, boost = current.boost, fixedSpeed = current.fixedSpeed, writing = current.writing)
        if (previous.payload == null) refresh()
        else if (previous.page == "/api/tasks") client?.let { startPolling(it, previous.page, previous.query) }
    }
    fun loadNext() {
        val current = mutable.value
        val offset = current.nextOffset ?: return
        if (current.busy || current.stale) return
        refresh(current.query + ("offset" to offset.toString()), append = true)
    }
    fun refresh(query: Map<String, String> = mutable.value.query, append: Boolean = false) {
        val api = client ?: return
        loading?.cancel(); polling?.cancel()
        val page = mutable.value.page
        val previous = mutable.value.payload
        val requestQuery = if (registry.get(page)?.fields?.any { it.key == "limit" } == true) mapOf("limit" to "60") + query else query
        mutable.update { it.copy(busy = true, error = null, moreError = null, query = if (append) it.query else requestQuery - "offset", stale = false, listGeneration = it.listGeneration + if (append) 0 else 1) }
        loading = viewModelScope.launch {
            try {
                val resolvedQuery = if (page == "/api/library-processing/issues" && query["job_id"].isNullOrBlank()) {
                    val task = api.get("/api/library-processing")
                    val id = task.text("job_id")
                    if (id.isBlank()) {
                        mutable.update { it.copy(payload = JSONObject().put("message", "当前没有可查询问题的入库任务"), busy = false) }
                        return@launch
                    }
                    query + ("job_id" to id)
                } else requestQuery
                val incoming = api.get(page, resolvedQuery)
                val payload = incoming
                if (page in listOf("/api/parts", "/api/editions") && payload.has("error")) {
                    payload.remove("error")
                    payload.put("message", if (page.endsWith("parts")) "当前作品没有其他分卷" else "当前作品没有其他版本")
                }
                if (client !== api || mutable.value.page != page) return@launch
                val offset = resolvedQuery["offset"]?.toIntOrNull() ?: 0
                val limit = resolvedQuery["limit"]?.toIntOrNull() ?: 60
                val next = CatalogPaging.next(incoming, offset, limit)
                val merged = if (append && previous != null) CatalogPaging.append(previous, incoming) else incoming
                mutable.update { it.copy(payload = merged, busy = false, loadedCount = offset + CatalogPaging.count(incoming), nextOffset = next) }
                if (!append) app.database.dao().cache(CacheRecord(api.profile.id, page + mutable.value.query.toSortedMap(), merged.toString()))
                if (page in listOf("/api/tasks", "/api/downloads", "/api/follow/check", "/api/library-processing", "/api/thumbnail-jobs", "/api/scraping/cover", "/api/follow/resolve", "/api/links/prune", "/api/taste/refresh", "/api/resource-sync/apply", "/api/media-repair", "/api/organize", "/api/media-processing")) startPolling(api, page, query)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (client !== api || mutable.value.page != page) return@launch
                if (append) {
                    mutable.update { it.copy(busy = false, moreError = error.localizedMessage ?: "下一页加载失败") }
                    return@launch
                }
                val cache = app.database.dao().cache(api.profile.id, page + mutable.value.query.toSortedMap())
                val isAuth = error is PeachError && error.status in listOf(401, 403)
                val cached = if (isAuth) null else cache?.let { JSONObject(it.payload) }
                mutable.update { it.copy(busy = false, payload = cached, loadedCount = cached?.let(CatalogPaging::count) ?: 0, nextOffset = cached?.let { data -> CatalogPaging.next(data, 0, requestQuery["limit"]?.toIntOrNull() ?: 60) }, stale = cached != null, error = error.localizedMessage ?: "加载失败") }
            }
        }
    }
    private fun startPolling(api: PeachClient, page: String, query: Map<String, String>) {
        polling = viewModelScope.launch {
            while (client === api && mutable.value.page == page) {
                delay(5000)
                try { val result = api.get(page, query); mutable.update { it.copy(payload = result, error = null) } }
                catch (error: Exception) { if (error is CancellationException) throw error; mutable.update { it.copy(error = error.localizedMessage) }; break }
            }
        }
    }
    fun openItem(item: JSONObject) {
        val api = client ?: return
        detailLoading?.cancel()
        mutable.update { it.copy(item = item, busy = true, error = null) }
        detailLoading = viewModelScope.launch {
            try {
                val result = api.get("/api/item", mapOf("id" to item.text("id")))
                if (result.has("error")) throw PeachError(404, result.optString("error"))
                if (client !== api) return@launch
                mutable.update { it.copy(item = result.optJSONObject("item") ?: result, busy = false) }
            } catch (error: Exception) { if (error is CancellationException) throw error; mutable.update { it.copy(busy = false, error = error.localizedMessage) } }
        }
    }
    fun closeItem() { detailLoading?.cancel(); mutable.update { it.copy(item = null, busy = false) } }
    fun send(endpoint: Endpoint, body: JSONObject, after: (JSONObject) -> Unit = {}) {
        val api = client ?: return
        if (mutable.value.writing) return
        mutable.update { it.copy(writing = true, error = null, actionResult = null) }
        viewModelScope.launch {
            try {
                val result = api.post(endpoint.path, body)
                if (result.has("error") && !result.isNull("error")) throw PeachError(400, result.optString("error"))
                if (endpoint.path == "/api/organize/preview") organizePreview = JSONObject(body.toString())
                val review = endpoint.path.endsWith("/preview") || result.has("rows") || result.has("plan") || result.has("changes")
                mutable.update { it.copy(writing = false, actionResult = if (review) result else null, notice = "服务器已接受操作；后台任务进度可在任务中心查看") }
                after(result)
                if (mutable.value.item == null) refresh()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(writing = false, error = error.localizedMessage ?: "操作失败") }
            }
        }
    }
    fun dismissNotice() { mutable.update { it.copy(notice = null) } }
    fun dismissResult() { mutable.update { it.copy(actionResult = null) } }
    fun actionSeed(endpoint: Endpoint, seed: JSONObject?): JSONObject? =
        if (endpoint.path == "/api/organize/apply") organizePreview?.let { JSONObject(it.toString()).put("confirm", false) } ?: seed else seed
    fun background() { polling?.cancel() }
    fun foreground() { if (client != null && mutable.value.payload != null) refresh() }
    fun error(text: String) { mutable.update { it.copy(error = text) } }
    fun toggleWall() { mutable.update { it.copy(wall = !it.wall) } }
    fun preferences(theme: String = mutable.value.theme, boost: Float = mutable.value.boost, speed: Float = mutable.value.fixedSpeed) {
        mutable.update { it.copy(theme = theme, boost = boost, fixedSpeed = speed) }
        viewModelScope.launch { app.preferences.put("theme", theme); app.preferences.put("boost", boost.toString()); app.preferences.put("fixedSpeed", speed.toString()) }
    }
}
