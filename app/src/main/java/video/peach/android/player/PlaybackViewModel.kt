package video.peach.android.player

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import video.peach.android.PeachApplication
import video.peach.android.data.*
import java.io.File
import java.util.UUID

data class PlayingState(
    val item: JSONObject? = null, val loading: Boolean = false, val error: String? = null, val fatalError: Boolean = false,
    val position: Long = 0, val duration: Long = 0, val buffered: Long = 0, val playing: Boolean = false,
    val full: Boolean = false, val mini: Boolean = false, val pip: Boolean = false,
    val locked: Boolean = false, val controls: Boolean = true, val boost: Boolean = false,
    val speed: Float = 1f, val boostSpeed: Float = 2f, val subtitleSize: Float = 22f,
    val subtitleColor: Int = android.graphics.Color.WHITE, val subtitleBackground: Int = 0x99000000.toInt(),
    val subtitleEdge: Int = 1, val subtitleFont: String = "sans-serif",
    val subtitles: List<JSONObject> = emptyList(), val qualities: List<JSONObject> = emptyList(),
    val editions: List<JSONObject> = emptyList(), val parts: List<JSONObject> = emptyList(), val subtitleLabel: String = "关闭字幕", val timeline: JSONObject? = null,
    val fastSeek: Boolean = true, val direct: Boolean = true
)

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PeachApplication
    private val mutable = MutableStateFlow(PlayingState())
    val state = mutable.asStateFlow()
    var player: ExoPlayer? = null
        private set
    private var mediaSession: MediaSession? = null
    private var api: PeachClient? = null
    private var session = ""
    private var generation = 0
    private var mediaUri: String = ""
    private var mime: String? = null
    private var ticker: Job? = null
    private var load: Job? = null
    private var reporting: Job? = null
    private val reportLock = Mutex()
    private var lastTick = 0L
    private var elapsed = 0.0
    private var seeks = 0
    private var loaded = false
    private var hadPlayed = false
    private var closing = false
    private var endedSession = false
    private var requestedMode = "auto"
    private var reportPlayOnStart = true
    val gestures = GestureMachine()

    fun open(client: PeachClient, item: JSONObject, fixed: Float, boost: Float, mode: String = "auto", resumeOverride: Long? = null, countPlay: Boolean = true) {
        close()
        val currentGeneration = ++generation
        api = client; session = UUID.randomUUID().toString()
        requestedMode = mode; reportPlayOnStart = countPlay
        elapsed = 0.0; seeks = 0; loaded = false; hadPlayed = false; closing = false; endedSession = false
        mutable.value = PlayingState(item = item, loading = true, speed = fixed, boostSpeed = boost)
        val sourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(app, OkHttpDataSource.Factory(client.readHttp)))
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(10_000, true)
            .build()
        val exo = ExoPlayer.Builder(app, DefaultRenderersFactory(app).setEnableDecoderFallback(true))
            .setLoadControl(loadControl)
            .setMediaSourceFactory(sourceFactory)
            .build()
        exo.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        exo.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        exo.setHandleAudioBecomingNoisy(true)
        player = exo
        mediaSession = MediaSession.Builder(app, exo).build()
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                mutable.update { it.copy(playing = isPlaying) }
                if (!isPlaying && loaded && hadPlayed && !closing) flush(false)
                if (isPlaying && !hadPlayed) {
                    hadPlayed = true
                    if (countPlay) viewModelScope.launch {
                        val follow = isFollow(item)
                        runCatching { client.post(if (follow) "/api/follow/play" else "/api/play", JSONObject().put(if (follow) "item" else "id", identity(item).toLong())) }
                            .onFailure { error -> mutable.update { it.copy(error = "播放次数未保存：${error.localizedMessage}") } }
                    }
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                mutable.update { it.copy(loading = playbackState == Player.STATE_BUFFERING) }
                if (playbackState == Player.STATE_READY) loaded = true
                if (playbackState == Player.STATE_ENDED) { endedSession = true; flush(true) }
            }
            override fun onPlayerError(error: PlaybackException) {
                val causes = generateSequence<Throwable>(error) { it.cause }.take(12).toList()
                val http = causes.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
                val message = if (http != null) {
                    val body = runCatching { JSONObject(http.responseBody.toString(Charsets.UTF_8)).text("error", "message") }.getOrDefault("")
                    MediaErrors.http(http.responseCode, body)
                } else when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED, PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> "当前设备无法解码该视频，可尝试 HLS 播放或在详情页选择其他播放器。"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "媒体连接中断或超时，请检查网络后重试。"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "无法解析视频容器，可尝试 HLS 播放或其他播放器。"
                    else -> "播放器未能读取视频，请重试或尝试 HLS 播放。"
                }
                mutable.update { it.copy(loading = false, fatalError = true, error = "$message\n诊断编号：${error.errorCode}") }
                releaseGesture()
            }
        })
        load = viewModelScope.launch {
            try {
                val isFastSeek = app.preferences.get("fastSeek", "true") == "true"
                exo.setSeekParameters(if (isFastSeek) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
                mutable.update { it.copy(fastSeek = isFastSeek,
                    subtitleSize = app.preferences.get("subtitleSize", "22").toFloatOrNull() ?: 22f,
                    subtitleColor = app.preferences.get("subtitleColor", "-1").toIntOrNull() ?: -1,
                    subtitleBackground = app.preferences.get("subtitleBackground", "-1728053248").toIntOrNull() ?: 0x99000000.toInt(),
                    subtitleEdge = app.preferences.get("subtitleEdge", "1").toIntOrNull() ?: 1,
                    subtitleFont = app.preferences.get("subtitleFont", "sans-serif")) }
                val follow = isFollow(item)
                val id = identity(item)
                var isDirect = false
                if (follow) {
                    mediaUri = client.resolve("/follow-stream?id=$id${mediaParameter(item)}")
                    mime = null
                } else if (mode == "hls") {
                    val plan = client.get("/api/stream-plan", mapOf("id" to id, "session" to session, "mode" to "hls"))
                    mediaUri = client.resolve(plan.getString("src"))
                    mime = MimeTypes.APPLICATION_M3U8
                } else {
                    val directUrl = runCatching {
                        val ticket = client.post("/api/external-playback?id=$id")
                        client.resolve(ticket.getString("url"))
                    }.getOrNull()

                    if (directUrl != null) {
                        mediaUri = directUrl
                        mime = null
                        isDirect = true
                    } else {
                        val plan = client.get("/api/stream-plan", mapOf("id" to id, "session" to session, "mode" to mode))
                        mediaUri = client.resolve(plan.getString("src"))
                        mime = if (plan.optString("protocol") == "hls") MimeTypes.APPLICATION_M3U8
                            else plan.optString("mime_type").ifBlank { null }
                    }
                }
                if (currentGeneration != generation) return@launch
                mutable.update { it.copy(direct = isDirect) }
                if (currentGeneration != generation) return@launch
                val saved = app.database.dao().position(client.profile.id, if (follow) "follow" else "item", id, edition(item))
                val resume = resumeOverride ?: saved?.milliseconds ?: (item.optDouble("leave_ratio", 0.0) * item.optDouble("duration", 0.0) * 1000).toLong()
                exo.setMediaItem(buildMediaItem(item, emptyList()))
                exo.setPlaybackSpeed(fixed)
                exo.prepare(); if (resume > 0) exo.seekTo(resume)
                exo.playWhenReady = true
                mutable.update { it.copy(loading = false) }
                ticker = viewModelScope.launch {
                    lastTick = SystemClock.elapsedRealtime()
                    var nextReport = lastTick + 10000
                    var lastMediaPosition = exo.currentPosition
                    while (isActive && player === exo) {
                        delay(500)
                        val now = SystemClock.elapsedRealtime()
                        val mediaPosition = exo.currentPosition
                        val deltaPosition = mediaPosition - lastMediaPosition
                        if (exo.isPlaying && deltaPosition in 1..2500) elapsed += deltaPosition / 1000.0
                        lastMediaPosition = mediaPosition
                        lastTick = now
                        mutable.update { it.copy(position = exo.currentPosition.coerceAtLeast(0), duration = exo.duration.takeIf { value -> value > 0 } ?: (item.optDouble("duration", 0.0) * 1000).toLong(), buffered = exo.bufferedPosition.coerceAtLeast(0)) }
                        if (now >= nextReport) { flush(false); nextReport = now + 10000 }
                    }
                }
                if (!follow) {
                    launch { runCatching { client.get("/api/timeline", mapOf("id" to id)) }.onSuccess { meta -> if (currentGeneration == generation) mutable.update { it.copy(timeline = meta) } } }
                    launch { runCatching { client.get("/api/assets/$id/subtitles").optJSONArray("subtitles")?.objects().orEmpty() }.onSuccess { rows -> if (currentGeneration == generation) mutable.update { it.copy(subtitles = rows) } } }
                    for ((path, parts) in listOf("/api/editions" to false, "/api/parts" to true)) {
                        launch { runCatching { client.get(path, mapOf("id" to id)).optJSONArray("items")?.objects().orEmpty() }.onSuccess { rows -> if (currentGeneration == generation) mutable.update { if (parts) it.copy(parts = rows) else it.copy(editions = rows) } } }
                    }
                } else launch { runCatching { client.get("/follow-qualities", mapOf("id" to id)).optJSONArray("qualities")?.objects().orEmpty() }.onSuccess { rows -> if (currentGeneration == generation) mutable.update { it.copy(qualities = rows) } } }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                val message = if (error is PeachError) MediaErrors.http(error.status, error.message.orEmpty()) else "无法连接播放来源，请检查网络后重试。"
                if (currentGeneration == generation) mutable.update { it.copy(loading = false, fatalError = true, error = "获取播放地址失败：$message") }
            }
        }
    }
    private fun buildMediaItem(item: JSONObject, subtitles: List<MediaItem.SubtitleConfiguration>): MediaItem = MediaItem.Builder().setUri(mediaUri)
        .setMimeType(mime).setMediaId(identity(item)).setSubtitleConfigurations(subtitles)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(item.text("catalog_title", "title", "name", "code")).build()).build()
    private fun edition(item: JSONObject) = item.text("version_id", "edition_id", "id") + mediaParameter(item)
    private fun mediaParameter(item: JSONObject) = if (item.has("_media_index")) "&media=${item.optInt("_media_index")}" else ""
    fun isFollow(item: JSONObject) = item.optBoolean("_follow") || item.text("follow_item_id").isNotBlank() || item.optString("location") == "online"
    fun identity(item: JSONObject) = if (isFollow(item)) item.text("follow_item_id", "id") else item.text("id")
    fun chooseSubtitle(path: String?, label: String = "关闭字幕") {
        val exo = player ?: return
        val item = mutable.value.item ?: return
        val position = exo.currentPosition; val playing = exo.playWhenReady
        val tracks = path?.let { listOf(MediaItem.SubtitleConfiguration.Builder(Uri.parse(if (it.startsWith("file:")) it else api!!.resolve(it)))
            .setMimeType(MimeTypes.TEXT_VTT).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).setLabel(label).build()) }.orEmpty()
        exo.setMediaItem(buildMediaItem(item, tracks), position)
        exo.prepare(); exo.playWhenReady = playing
        mutable.update { it.copy(subtitleLabel = label) }
    }
    fun importSubtitle(name: String, bytes: ByteArray) {
        val client = api ?: return
        viewModelScope.launch {
            try {
                require(bytes.size <= 4 * 1024 * 1024) { "字幕文件不能超过 4 MiB" }
                val text = withContext(Dispatchers.IO) {
                    val url = client.base.resolve("/api/subtitles/convert")!!.newBuilder().addQueryParameter("name", name).build()
                    val body = bytes.toRequestBody("application/octet-stream".toMediaType())
                    client.http.newCall(okhttp3.Request.Builder().url(url).post(body).build()).execute().use { response ->
                        val value = response.body?.string().orEmpty()
                        if (!response.isSuccessful) throw PeachError(response.code, "字幕转换失败")
                        value
                    }
                }
                val file = File(app.cacheDir, "subtitle-${UUID.randomUUID()}.vtt").apply { writeText(text) }
                chooseSubtitle(Uri.fromFile(file).toString(), name)
            } catch (error: Exception) { if (error is CancellationException) throw error; mutable.update { it.copy(error = error.localizedMessage) } }
        }
    }
    suspend fun searchSubtitles(query: String) = api?.get("/api/subtitles/search", mapOf("q" to query)) ?: JSONObject()
    suspend fun subtitleFiles(id: String) = api?.get("/api/subtitles/subtitlecat/$id") ?: JSONObject()
    fun quality(height: Int) {
        val item = mutable.value.item ?: return
        if (!isFollow(item)) return
        val client = api ?: return
        val exo = player ?: return
        flush(false)
        mediaUri = client.resolve("/follow-stream?id=${identity(item)}&quality=$height${mediaParameter(item)}")
        val position = exo.currentPosition
        exo.setMediaItem(buildMediaItem(item, emptyList()), position); exo.prepare(); exo.play()
    }
    fun compatibility() {
        val client = api ?: return
        val item = mutable.value.item ?: return
        val position = player?.currentPosition
        open(client, item, mutable.value.speed, mutable.value.boostSpeed, "hls", position, countPlay = false)
    }
    fun direct() {
        val client = api ?: return
        val item = mutable.value.item ?: return
        val position = player?.currentPosition
        open(client, item, mutable.value.speed, mutable.value.boostSpeed, "auto", position, countPlay = false)
    }
    fun currentClient(): PeachClient? = api
    fun retry() {
        val client = api ?: return
        val item = mutable.value.item ?: return
        val position = player?.currentPosition
        val count = reportPlayOnStart && !hadPlayed
        open(client, item, mutable.value.speed, mutable.value.boostSpeed, requestedMode, resumeOverride = position, countPlay = count)
    }
    fun speed(value: Float) {
        releaseGesture()
        player?.setPlaybackSpeed(value)
        mutable.update { it.copy(speed = value) }
        viewModelScope.launch { app.preferences.put("fixedSpeed", value.toString()) }
    }
    fun setFastSeek(enabled: Boolean) {
        player?.setSeekParameters(if (enabled) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
        mutable.update { it.copy(fastSeek = enabled) }
        viewModelScope.launch { app.preferences.put("fastSeek", enabled.toString()) }
    }
    fun toggle() { player?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seek(value: Long) { releaseGesture(); val total = mutable.value.duration; player?.seekTo(if (total > 0) value.coerceIn(0, total) else value.coerceAtLeast(0)); seeks++ }
    fun press() { gestures.down(player?.playbackParameters?.speed ?: mutable.value.speed) }
    fun hold() { if (gestures.hold()) { player?.setPlaybackSpeed(mutable.value.boostSpeed); mutable.update { it.copy(boost = true) } } }
    fun releaseGesture() { gestures.release()?.let { player?.setPlaybackSpeed(it) }; mutable.update { it.copy(boost = false) } }
    fun controls() { mutable.update { it.copy(controls = !it.controls) } }
    fun hideControls() { if (!mutable.value.locked) mutable.update { it.copy(controls = false) } }
    fun lock() { releaseGesture(); mutable.update { it.copy(locked = !it.locked, controls = false) } }
    fun full(value: Boolean) { releaseGesture(); mutable.update { it.copy(full = value, mini = false, controls = true) } }
    fun mini(value: Boolean) { releaseGesture(); mutable.update { it.copy(mini = value, full = false) } }
    fun pip(value: Boolean) { releaseGesture(); mutable.update { it.copy(pip = value) } }
    fun background() { releaseGesture(); if (!mutable.value.pip) player?.pause() }
    fun subtitleStyle(size: Float = mutable.value.subtitleSize, color: Int = mutable.value.subtitleColor, background: Int = mutable.value.subtitleBackground, edge: Int = mutable.value.subtitleEdge, font: String = mutable.value.subtitleFont) {
        mutable.update { it.copy(subtitleSize = size, subtitleColor = color, subtitleBackground = background, subtitleEdge = edge, subtitleFont = font) }
        viewModelScope.launch { app.preferences.put("subtitleSize", size.toString()); app.preferences.put("subtitleColor", color.toString()); app.preferences.put("subtitleBackground", background.toString()); app.preferences.put("subtitleEdge", edge.toString()); app.preferences.put("subtitleFont", font) }
    }
    private fun flush(ended: Boolean) {
        val item = mutable.value.item ?: return
        val exo = player ?: return
        if (!loaded) return
        val client = api ?: return
        val position = exo.currentPosition.coerceAtLeast(0)
        val duration = exo.duration.takeIf { it > 0 } ?: mutable.value.duration
        val delta = elapsed; elapsed = 0.0
        val seekCount = seeks; seeks = 0
        val follow = isFollow(item)
        val id = identity(item)
        val completed = ended || endedSession
        val payload = JSONObject().put(if (follow) "item" else "id", id.toLong()).put("position", position / 1000.0).put("duration", duration / 1000.0).put("delta", delta).put("ended", completed).put("seeks", seekCount)
        val reportGeneration = generation
        reporting = app.outgoing.launch {
            try {
                reportLock.withLock {
                    app.database.dao().position(PlaybackPosition(client.profile.id, if (follow) "follow" else "item", id, edition(item), if (completed) 0 else position))
                    withTimeout(20000) { client.post(if (follow) "/api/follow/activity" else "/api/activity", payload) }
                }
            } catch (error: Exception) {
                if (reportGeneration == generation && mutable.value.item != null)
                    mutable.update { it.copy(error = "观看记录未保存：${error.localizedMessage}。为避免重复累计，未自动重试。") }
            }
        }
    }
    fun close() {
        closing = true
        generation++; releaseGesture(); load?.cancel(); ticker?.cancel()
        flush(false)
        val client = api; val previousSession = session
        player?.pause(); mediaSession?.release(); mediaSession = null; player?.release(); player = null
        if (client != null && previousSession.isNotEmpty()) app.outgoing.launch {
            runCatching { withTimeout(20000) { client.post("/api/stream-cancel?session=$previousSession") } }
        }
        api = null; session = ""; loaded = false
        mutable.value = PlayingState()
    }
    override fun onCleared() { close(); super.onCleared() }
}
