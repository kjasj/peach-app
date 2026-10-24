package video.peach.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import coil.size.Size
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import video.peach.android.data.ServerProfile
import video.peach.android.player.PlaybackViewModel
import video.peach.android.ui.CoverTransform

class ClientFixesTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: AppViewModel
    private lateinit var playback: PlaybackViewModel
    private fun pressBack() {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
        ui.waitForIdle()
    }
    @Before fun connect() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("catalogFixture") == "true")
        ui.runOnUiThread {
            vm = ViewModelProvider(ui.activity)[AppViewModel::class.java]
            playback = ViewModelProvider(ui.activity)[PlaybackViewModel::class.java]
        }
        ui.waitUntil(10000) { vm.state.value.ready }
        ui.runOnUiThread { vm.connect(ServerProfile("catalog-regression", "合成测试", "http://127.0.0.1:18902"), "fixture") }
        ui.waitUntil(15000) { vm.state.value.connected?.id == "catalog-regression" && vm.state.value.loadedCount == 60 && !vm.state.value.busy }
        ui.waitForIdle()
    }
    @After fun release() { if (::playback.isInitialized) ui.runOnUiThread { playback.close() } }
    private fun select(key: String) {
        ui.runOnUiThread { vm.select(key) }
        ui.waitUntil(10000) { vm.state.value.section == key && !vm.state.value.busy }
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        Thread.sleep(700)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(ui.activity.getExternalFilesDir(null), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun tagWorksAndStudioDetailsReturnToTheirParentWithoutFinishingActivity() {
        select("tags")
        ui.onNodeWithText("合成标签").performClick()
        ui.waitUntil(10000) { vm.state.value.page == "/api/items" && !vm.state.value.busy }
        assertEquals("合成标签", vm.state.value.query["tag"])
        ui.onNodeWithContentDescription("返回上一层").assertIsDisplayed()
        capture("before-system-back")
        pressBack()
        println("BACK result page=${vm.state.value.page} history=${vm.state.value.canGoBack} finishing=${ui.activity.isFinishing}")
        capture("after-system-back")
        ui.waitUntil(5000) { vm.state.value.page == "/api/index" }
        ui.onNodeWithText("合成标签").assertIsDisplayed()
        assertFalse(ui.activity.isFinishing)
        select("studios")
        ui.onNodeWithText("查看资料").performClick()
        ui.waitUntil(10000) { vm.state.value.page == "/api/entity" && !vm.state.value.busy }
        ui.onNodeWithContentDescription("返回上一层").assertExists()
        pressBack()
        ui.waitUntil(5000) { vm.state.value.page == "/api/index" }
        ui.onNodeWithText("合成厂牌").assertIsDisplayed()
        assertFalse(ui.activity.isFinishing)
    }
    @Test fun followAndManagementShowBusinessContentInsteadOfMachineMetadata() {
        select("follow")
        ui.onNodeWithText("暂无关注动态").assertExists()
        ui.onNodeWithText("result_versions").assertDoesNotExist()
        ui.onNodeWithText("来源与分组").performClick()
        ui.waitUntil(10000) { vm.state.value.query["summary"] == "1" && !vm.state.value.busy }
        ui.onNodeWithText("合成关注来源").assertIsDisplayed()
        ui.onNodeWithText("正在关注").assertIsDisplayed()
        capture("follow-native")
        select("manage")
        ui.onNodeWithText("追更检查").assertIsDisplayed()
        ui.onNodeWithText("运行中").assertIsDisplayed()
        ui.onNodeWithText("已处理 2 / 10").assertIsDisplayed()
        ui.onNodeWithText("statuses").assertDoesNotExist()
        ui.onNodeWithText("synthetic-machine-key").assertDoesNotExist()
        capture("manage-native")
    }
    @Test fun scrapedCoverCropUsesRightPanelAndJavShowsPortraitArtwork() = runBlocking {
        val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLUE)
        canvas.drawRect(300f, 0f, 600f, 400f, Paint().apply { color = Color.RED })
        val item = JSONObject("""{"poster_box":{"x0":320,"y0":0,"x1":600,"y1":400,"px":[600,400]}}""")
        val crop = CoverTransform(item).transform(bitmap, Size.ORIGINAL)
        assertEquals(280, crop.width)
        assertEquals(Color.RED, crop.getPixel(0, 10))
        crop.recycle(); bitmap.recycle()
        select("jav")
        ui.onNodeWithContentDescription("TEST-0001.mp4").assertExists()
        capture("jav-scraped-cover")
    }
    @Test fun masonryPacksMixedArtworkWithoutWaitingForTallerNeighbour() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("mixedCovers") == "true")
        select("jav")
        capture("masonry-covers")
        val first = ui.onNodeWithTag("media-card-1").fetchSemanticsNode().boundsInRoot
        val second = ui.onNodeWithTag("media-card-2").fetchSemanticsNode().boundsInRoot
        val third = ui.onNodeWithTag("media-card-3").fetchSemanticsNode().boundsInRoot
        assertTrue("竖封应高于横封", first.height > second.height * 1.3f)
        assertEquals("下一张应落到较短的一列", second.left, third.left, 1f)
        assertTrue("下一张不等待较高的卡片", third.top < first.bottom)
        val density = ui.activity.resources.displayMetrics.density
        assertTrue("同列间距应保持紧凑", third.top - second.bottom <= 14f * density)
        ui.onNodeWithTag("media-card-3").performClick()
        ui.waitUntil(10000) { vm.state.value.item?.optInt("id") == 3 && !vm.state.value.busy }
        ui.onNodeWithContentDescription("返回馆藏").performClick()
        ui.waitUntil(5000) { vm.state.value.item == null }
        ui.onNodeWithTag("catalog-grid").assertExists()
    }
    @Test fun deepCatalogPositionSurvivesDetailAndBack() {
        ui.runOnUiThread { vm.loadNext() }
        ui.waitUntil(10000) { vm.state.value.loadedCount == 120 && !vm.state.value.busy }
        ui.onNodeWithTag("catalog-grid").performScrollToIndex(100)
        ui.waitForIdle()
        val before = ui.onNodeWithTag("media-card-101").fetchSemanticsNode().boundsInRoot
        val loaded = vm.state.value.loadedCount
        capture("before-detail-scroll")
        ui.onNodeWithTag("media-card-101").performClick()
        ui.waitUntil(10000) { vm.state.value.item?.optInt("id") == 101 && !vm.state.value.busy }
        ui.onNodeWithContentDescription("返回馆藏").assertExists()
        pressBack()
        ui.waitUntil(5000) { vm.state.value.item == null }
        ui.waitForIdle()
        val after = ui.onNodeWithTag("media-card-101").fetchSemanticsNode().boundsInRoot
        assertEquals("返回详情前的列表高度位置", before.top, after.top, 2f)
        assertEquals(loaded, vm.state.value.loadedCount)
        ui.onNodeWithTag("media-card-1").assertDoesNotExist()
        capture("after-detail-scroll")
    }
    @Test fun unavailableReportsServerFileFailureAndCanBeClosed() {
        ui.runOnUiThread { playback.open(vm.client!!, JSONObject("""{"id":999,"name":"TEST-MISSING.mp4","location":"local"}"""), 1f, 2f, countPlay = false) }
        ui.waitUntil(15000) { playback.state.value.fatalError }
        ui.onNodeWithText("无法播放").assertIsDisplayed()
        assertTrue(playback.state.value.error!!.contains("无法读取"))
        assertTrue(playback.state.value.error!!.contains("HTTP 404"))
        capture("playback-unavailable")
        ui.onNodeWithText("关闭播放器").performClick()
        ui.waitUntil(5000) { playback.player == null }
        assertFalse(ui.activity.isFinishing)
    }
    @Test fun normalRangeVideoStillPlaysWithDecoderFallbackEnabled() {
        ui.runOnUiThread { playback.open(vm.client!!, JSONObject("""{"id":1,"name":"TEST-RANGE.mp4","location":"local"}"""), 1f, 2f, resumeOverride = 0, countPlay = false) }
        ui.waitUntil(25000) { playback.state.value.playing && playback.state.value.position >= 1000 }
        assertFalse(playback.state.value.fatalError)
        ui.runOnUiThread { playback.close() }
    }
}
