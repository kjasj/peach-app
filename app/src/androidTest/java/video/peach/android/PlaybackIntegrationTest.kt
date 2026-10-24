package video.peach.android

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import video.peach.android.data.*
import video.peach.android.player.PlaybackViewModel

/** 必须显式传 peachFixture=true，且只允许回环地址上的合成服务。 */
@androidx.annotation.OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class PlaybackIntegrationTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var client: PeachClient
    private lateinit var playback: PlaybackViewModel
    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ui.mainClock.advanceTimeByFrame()
        instrumentation.waitForIdleSync()
        Thread.sleep(800) // 等待系统画中画转场和 Surface 提交，避免只截到前一帧。
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val file = java.io.File(ui.activity.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Before fun fixture() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("仅在隔离测试服务运行", args.getString("peachFixture") == "true")
        val base = args.getString("peachBase") ?: "http://127.0.0.1:18901"
        require(android.net.Uri.parse(base).host in listOf("127.0.0.1", "localhost"))
        val app = ui.activity.application as PeachApplication
        client = PeachClient(ServerProfile("instrumentation-fixture", "合成测试", base), app.secrets)
        client.login("android-fixture", 0)
        ui.runOnUiThread { playback = ViewModelProvider(ui.activity)[PlaybackViewModel::class.java] }
        ui.waitUntil(15000) { ViewModelProvider(ui.activity)[AppViewModel::class.java].state.value.connected != null }
        ui.waitForIdle()
        val connected = ViewModelProvider(ui.activity)[AppViewModel::class.java].client!!
        require(connected.base.toString() == PeachClient.normalizeAddress(base)) { "拒绝在非指定合成服务上运行写入测试" }
        client = connected
        val catalog = client.get("/api/items", mapOf("limit" to "200"))
        val items = catalog.getJSONArray("items").objects()
        require(items.isNotEmpty() && items.all { it.text("name").startsWith("TEST-") } && catalog.optInt("total", items.size) <= 20) {
            "当前服务包含非合成馆藏，拒绝运行写入测试"
        }
    }
    private fun open(name: String) = runBlocking {
        val item = client.get("/api/items", mapOf("q" to name)).getJSONArray("items").objects().first { it.optString("name") == name }
        ui.runOnUiThread { playback.open(client, item, 1.25f, 2f, resumeOverride = 0); playback.full(true) }
        ui.waitUntil(30000) { playback.state.value.playing && playback.state.value.position > 0 }
        item
    }
    @Test fun realTouchHoldRestoresSpeedAndDoesNotReplacePlayer() {
        open("TEST-001.mp4")
        ui.waitForIdle()
        Thread.sleep(1000)
        ui.onRoot().performTouchInput { down(Offset(width * .3f, height * .28f)) }
        Thread.sleep(650)
        ui.waitUntil(3000) { playback.state.value.boost }
        ui.onNodeWithText("2.0× 临时倍速").assertIsDisplayed()
        screenshot("hold-boost")
        ui.runOnUiThread { assertEquals(2f, playback.player!!.playbackParameters.speed, .001f) }
        ui.onRoot().performTouchInput { up() }
        ui.waitUntil(3000) { !playback.state.value.boost }
        ui.runOnUiThread {
            assertEquals(1.25f, playback.player!!.playbackParameters.speed, .001f)
            val original = playback.player
            playback.full(false); playback.mini(true)
            assertSame(original, playback.player)
            playback.mini(false); playback.full(true)
            assertSame(original, playback.player)
            playback.close()
        }
    }
    @Test fun actualHlsPlaysAndCancellationIsAccepted() = runBlocking {
        val item = client.get("/api/items", mapOf("q" to "TEST-003.mkv")).getJSONArray("items").objects().first()
        val session = java.util.UUID.randomUUID().toString()
        val plan = client.get("/api/stream-plan", mapOf("id" to item.text("id"), "session" to session, "mode" to "hls"))
        assertEquals("hls", plan.getString("protocol"))
        ui.runOnUiThread { playback.open(client, item, 1f, 2f, "hls", resumeOverride = 0); playback.full(true) }
        ui.waitUntil(30000) { playback.state.value.playing && playback.state.value.position >= 1000 }
        screenshot("hls-player")
        ui.runOnUiThread { playback.close() }
        val result = client.post("/api/stream-cancel?session=$session")
        assertTrue(result.optBoolean("ok"))
    }
    @Test fun localSubtitleTrackCanBeSelectedAndStyled() {
        open("TEST-001.mp4")
        ui.waitUntil(10000) { playback.state.value.subtitles.isNotEmpty() }
        val track = playback.state.value.subtitles.first { it.optBoolean("playable") }
        ui.runOnUiThread {
            playback.chooseSubtitle(track.getString("src"), track.optString("label"))
            playback.subtitleStyle(size = 28f, font = "monospace")
            playback.seek(0)
        }
        ui.waitUntil(15000) { playback.state.value.position > 1000 && playback.state.value.playing }
        ui.runOnUiThread {
            assertTrue(playback.player!!.currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT })
            assertEquals(28f, playback.state.value.subtitleSize, .001f)
            playback.press(); playback.hold(); playback.background()
            assertFalse(playback.state.value.boost)
            assertEquals(1.25f, playback.player!!.playbackParameters.speed, .001f)
            playback.close()
        }
    }
    @Test fun systemPictureInPictureKeepsPlaybackSession() {
        assumeTrue(ui.activity.packageManager.hasSystemFeature("android.software.picture_in_picture"))
        open("TEST-001.mp4")
        var original: androidx.media3.exoplayer.ExoPlayer? = null
        ui.runOnUiThread { original = playback.player; ui.activity.enterPip() }
        ui.waitUntil(10000) { ui.activity.isInPictureInPictureMode && playback.state.value.pip }
        screenshot("system-pip")
        ui.runOnUiThread { assertSame(original, playback.player); assertTrue(playback.player!!.playWhenReady) }
        ui.runOnUiThread {
            ui.activity.startActivity(android.content.Intent(ui.activity, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        ui.waitUntil(10000) { !ui.activity.isInPictureInPictureMode }
        ui.runOnUiThread { assertSame(original, playback.player); playback.close() }
    }
    @Test fun organizePreviewIsReviewableAndSeedsConfirmationForm() {
        lateinit var appViewModel: AppViewModel
        ui.runOnUiThread {
            appViewModel = ViewModelProvider(ui.activity)[AppViewModel::class.java]
            appViewModel.navigate("/api/organize")
        }
        ui.waitUntil(15000) { !appViewModel.state.value.busy && appViewModel.state.value.payload != null }
        val body = JSONObject().put("location", "local").put("file_template", "{number}[ CD{cd}]").put("dir_template", "")
        ui.runOnUiThread { appViewModel.send(appViewModel.registry.post("/api/organize/preview")!!, body) }
        ui.waitUntil(15000) { appViewModel.state.value.actionResult != null }
        ui.onNodeWithText("操作结果与预览").assertIsDisplayed()
        assertTrue(appViewModel.state.value.actionResult!!.has("rows"))
        screenshot("organize-preview")
        val seed = appViewModel.actionSeed(appViewModel.registry.post("/api/organize/apply")!!, null)!!
        assertEquals("{number}[ CD{cd}]", seed.getString("file_template"))
        assertFalse(seed.getBoolean("confirm"))
        ui.onNodeWithText("已查看，返回").performScrollTo().performClick()
        ui.waitUntil(5000) { appViewModel.state.value.actionResult == null }
    }
}
