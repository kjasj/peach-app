package video.peach.android

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import video.peach.android.data.*

class PresentationTest {
    @Test fun artworkHeightUsesCropGeometryAndMediaDimensions() {
        val poster = JSONObject("""{"has_cover":true,"code":"TEST-001","poster_box":{"x0":1000,"x1":1700,"y0":0,"y1":1000}}""")
        assertEquals(.7f, Artwork.aspect(poster), .001f)
        assertEquals(.5f, Artwork.aspect(JSONObject("""{"width":500,"height":1000}""")), .001f)
        assertEquals(16f/9f, Artwork.aspect(JSONObject()), .001f)
    }
    @Test fun scrapedArtworkWinsOverVideoThumbnailAndUsesVersion() {
        val item = JSONObject("""{"id":1,"has_cover":true,"is_jav":true,"code":"TEST-001","cover_key":"TEST-001 蓝光","cover_version":"42","thumb_url":"/poster?id=1&c=4"}""")
        assertEquals("/cover?code=TEST-001%20%E8%93%9D%E5%85%89&thumb=1&v=42", Artwork.path(item))
        assertEquals(Artwork.path(item), video.peach.android.ui.imageOf(item))
    }
    @Test fun manualPosterBoxIsProjectedAndStaleGeometryIsIgnored() {
        val item = JSONObject("""{"poster_box":{"x0":1000,"y0":100,"x1":1900,"y1":1400,"px":[2000,1500],"method":"manual"}}""")
        val box = Artwork.window(item, 4f / 3f)!!
        assertEquals(.5f, box.left, .001f)
        assertEquals(.95f, box.right, .001f)
        assertEquals(100f / 1500f, box.top, .001f)
        assertNull(Artwork.window(item, 1f))
    }
    @Test fun wideSleeveFallsBackToRightHandFrontPanel() {
        val box = Artwork.window(JSONObject(), 1.5f)!!
        assertEquals(1f - .704f / 1.5f, box.left, .001f)
        assertNull(Artwork.window(JSONObject("""{"cover_frame":{"layout":"still"}}"""), 1.5f))
    }
    @Test fun machineStatesAreTranslatedButNamesStayIntact() {
        assertEquals("已完成", Presentation.value("status", "succeeded"))
        assertEquals("定时", Presentation.value("trigger", "scheduled"))
        assertEquals("running", Presentation.value("title", "running"))
        assertEquals("来源平台", Labels.of("provider_label"))
        assertEquals("添加或编辑来源", Labels.of("/api/follow/source"))
        assertFalse(Labels.of("/api/follow/media/hide").contains("api"))
        assertTrue("result_versions" in Presentation.hidden)
    }
    @Test fun unavailableIsReportedAsServerFileFailureRatherThanDecoderFailure() {
        try { PeachClient.decodeText(404, """{"error":"unavailable"}"""); fail() }
        catch (error: PeachError) {
            assertEquals(404, error.status)
            assertTrue(error.message!!.contains("无法读取"))
            assertFalse(error.message!!.contains("解码"))
            assertTrue(MediaErrors.http(error.status, error.message!!).contains("HTTP 404"))
        }
    }
}
