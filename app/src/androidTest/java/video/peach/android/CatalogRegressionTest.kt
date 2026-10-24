package video.peach.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import video.peach.android.data.ServerProfile

/** 只能连接 18902 上拒绝业务写入的合成 HTTP 夹具。 */
class CatalogRegressionTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: AppViewModel
    private fun capture(name: String) {
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Before fun connect() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("catalogFixture") == "true")
        ui.runOnUiThread { vm = ViewModelProvider(ui.activity)[AppViewModel::class.java] }
        ui.waitUntil(10000) { vm.state.value.ready }
        ui.runOnUiThread { vm.connect(ServerProfile("catalog-regression", "合成分页测试", "http://127.0.0.1:18902"), "fixture") }
        ui.waitUntil(15000) { vm.state.value.connected?.id == "catalog-regression" && vm.state.value.loadedCount == 60 && !vm.state.value.busy }
    }
    @Test fun scrollsAll435ItemsAndKeepsLoadedRowsWhenNextPageFails() {
        ui.onNodeWithTag("catalog-grid").performScrollToIndex(59)
        ui.waitUntil(15000) { vm.state.value.loadedCount == 120 && !vm.state.value.busy }
        ui.onNodeWithTag("catalog-grid").performScrollToIndex(119)
        ui.waitUntil(15000) { vm.state.value.moreError != null }
        assertEquals(120, vm.state.value.payload!!.getJSONArray("items").length())
        ui.onNodeWithTag("catalog-grid").performScrollToIndex(120)
        ui.onNodeWithText("重试加载").performClick()
        ui.waitUntil(15000) { vm.state.value.loadedCount >= 180 && !vm.state.value.busy }
        while (vm.state.value.nextOffset != null) {
            val loaded = vm.state.value.loadedCount
            ui.onNodeWithTag("catalog-grid").performScrollToIndex(loaded - 1)
            ui.waitUntil(15000) { vm.state.value.loadedCount > loaded && !vm.state.value.busy }
        }
        assertEquals(435, vm.state.value.loadedCount)
        ui.onNodeWithText("已显示 435 / 435").assertExists()
        ui.onNodeWithTag("catalog-grid").performScrollToIndex(434)
        ui.onNodeWithText("TEST-0435.mp4").assertIsDisplayed()
        capture("catalog-435")
        assertFalse(vm.state.value.query.containsKey("offset"))
        ui.runOnUiThread { vm.refresh() }
        ui.waitUntil(15000) { vm.state.value.loadedCount == 60 && !vm.state.value.busy }
        assertEquals(1, vm.state.value.payload!!.getJSONArray("items").getJSONObject(0).getInt("id"))
    }
    @Test fun menuUsesWebsiteEntriesAndArtistOpensMatchingWorks() {
        ui.onNodeWithContentDescription("导航菜单").performClick()
        capture("website-navigation")
        ui.onNodeWithTag("nav-performers").performScrollTo().performClick()
        ui.waitUntil(10000) { vm.state.value.page == "/api/index" && !vm.state.value.busy }
        assertEquals("performers", vm.state.value.query["kind"])
        capture("artist-index")
        ui.onNodeWithText("合成艺人").performClick()
        ui.waitUntil(10000) { vm.state.value.page == "/api/items" && !vm.state.value.busy }
        assertEquals("合成艺人", vm.state.value.query["performer"])
        assertEquals(3, vm.state.value.loadedCount)
        ui.runOnUiThread { vm.select("jav") }
        ui.waitUntil(10000) { vm.state.value.section == "jav" && !vm.state.value.busy }
        assertEquals("1", vm.state.value.query["jav"])
        ui.onNodeWithText("想要").assertExists()
        ui.runOnUiThread { vm.select("tags") }
        ui.waitUntil(10000) { vm.state.value.section == "tags" && !vm.state.value.busy }
        ui.onNodeWithText("合成标签").assertExists()
    }
}
