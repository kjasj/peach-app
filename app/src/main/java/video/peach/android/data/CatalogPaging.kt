package video.peach.android.data

import org.json.JSONArray
import org.json.JSONObject

/** Peach 在 count=1 时返回 has_more:null，必须用 total 推导下一页。 */
object CatalogPaging {
    private val collections = listOf("items", "groups", "rows")
    fun count(payload: JSONObject): Int = collections.firstNotNullOfOrNull { payload.optJSONArray(it)?.length() } ?: 0

    fun next(payload: JSONObject, offset: Int, limit: Int): Int? {
        val received = count(payload)
        if (received == 0) return null
        val end = offset + received
        val hasMore = payload.opt("has_more") as? Boolean
        if (hasMore != null) return if (hasMore) end else null
        val total = (payload.opt("total") as? Number)?.toInt()
        return if (total != null) end.takeIf { it < total } else end.takeIf { received >= limit }
    }

    fun append(previous: JSONObject, incoming: JSONObject): JSONObject {
        val merged = JSONObject(incoming.toString())
        for (key in collections) {
            val before = previous.optJSONArray(key) ?: continue
            val after = incoming.optJSONArray(key) ?: continue
            val seen = mutableSetOf<String>()
            val values = JSONArray()
            for (array in listOf(before, after)) for (index in 0 until array.length()) {
                val row = array.get(index)
                val identity = if (row is JSONObject) row.text("id", "entity_id", "release_key", "k").ifBlank { row.toString() } else row.toString()
                if (seen.add(identity)) values.put(row)
            }
            merged.put(key, values)
        }
        return merged
    }
}
