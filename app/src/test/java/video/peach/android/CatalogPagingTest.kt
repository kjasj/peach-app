package video.peach.android

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import video.peach.android.data.*

class CatalogPagingTest {
    private fun page(start: Int, size: Int, total: Int) = JSONObject().put("total", total).put("has_more", JSONObject.NULL)
        .put("items", JSONArray((start until start + size).map { JSONObject().put("id", it) }))

    @Test fun loadsAll430ItemsWhenBackendHasMoreIsNull() {
        var result = page(0, 60, 430)
        var next = CatalogPaging.next(result, 0, 60)
        var requests = 1
        while (next != null) {
            val offset = next
            val incoming = page(offset, minOf(60, 430 - offset), 430)
            result = CatalogPaging.append(result, incoming)
            next = CatalogPaging.next(incoming, offset, 60)
            requests++
        }
        assertEquals(8, requests)
        assertEquals(430, result.getJSONArray("items").length())
        assertEquals((0 until 430).toList(), result.getJSONArray("items").objects().map { it.getInt("id") })
    }
    @Test fun appendDoesNotMutatePreviousAndDeduplicatesOverlaps() {
        val first = page(0, 60, 100)
        val merged = CatalogPaging.append(first, page(50, 50, 100))
        assertEquals(60, first.getJSONArray("items").length())
        assertEquals(100, merged.getJSONArray("items").length())
    }
    @Test fun supportsExplicitBooleanAndStopsOnEmptyPages() {
        assertEquals(60, CatalogPaging.next(page(0, 60, 430).put("has_more", true), 0, 60))
        assertNull(CatalogPaging.next(page(0, 60, 430).put("has_more", false), 0, 60))
        assertNull(CatalogPaging.next(page(60, 0, 430).put("has_more", true), 60, 60))
    }
    @Test fun navigationUsesWebsiteRoutesAndPluralIndexKinds() {
        assertEquals(listOf("首页", "关注", "JAV", "艺人", "标签", "厂牌", "已标记", "管理"), AppNavigation.primary.map { it.title })
        assertEquals("1", AppNavigation.primary.first { it.key == "jav" }.query["jav"])
        assertEquals("performers", AppNavigation.primary.first { it.key == "performers" }.query["kind"])
        assertEquals("tag", AppNavigation.entityKind("tags"))
        assertEquals("flagged", AppNavigation.primary.first { it.key == "flagged" }.query["state"])
    }
}
