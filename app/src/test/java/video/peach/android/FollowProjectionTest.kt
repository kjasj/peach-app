package video.peach.android

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import video.peach.android.ui.primaryRows

class FollowProjectionTest {
    @Test fun projectsPrimaryVariantsAndDuplicatesAsPlayableItems() {
        val payload = JSONObject("""{"groups":[{"release_key":"synthetic","primary":{"id":1,"title":"测试"},"variants":[{"id":2}],"duplicates":[{"id":3}]}]}""")
        val rows = primaryRows(payload, "/api/follow")
        assertEquals(listOf(1, 2, 3), rows.map { it.getInt("id") })
        assertTrue(rows.all { it.optBoolean("_follow") })
        assertFalse(payload.getJSONArray("groups").getJSONObject(0).getJSONObject("primary").has("_follow"))
    }
    @Test fun directItemsAreMarkedOnlyInFollowContext() {
        val payload = JSONObject("""{"items":[{"id":1}]}""")
        assertTrue(primaryRows(payload, "/api/follow").single().optBoolean("_follow"))
        assertFalse(primaryRows(payload, "/api/items").single().has("_follow"))
    }
}
