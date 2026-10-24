package video.peach.android.data

data class AppSection(val key: String, val title: String, val path: String, val query: Map<String, String> = emptyMap())

/** 对齐网页 DEFAULT_SIDEBAR_ORDER；索引接口 kind 使用复数，实体接口使用单数。 */
object AppNavigation {
    val primary = listOf(
        AppSection("home", "首页", "/api/items"),
        AppSection("follow", "关注", "/api/follow"),
        AppSection("jav", "JAV", "/api/items", mapOf("jav" to "1")),
        AppSection("performers", "艺人", "/api/index", mapOf("kind" to "performers")),
        AppSection("tags", "标签", "/api/index", mapOf("kind" to "tags")),
        AppSection("studios", "厂牌", "/api/index", mapOf("kind" to "studios")),
        AppSection("flagged", "已标记", "/api/items", mapOf("state" to "flagged")),
        AppSection("manage", "管理", "/api/tasks")
    )
    val extras = listOf(AppSection("playlists", "播放列表", "/api/playlists"), AppSection("immerse", "沉浸模式", "/api/items"))
    fun title(key: String) = (primary + extras).firstOrNull { it.key == key }?.title
    fun infer(path: String, query: Map<String, String>): String = when {
        path == "/api/index" -> query["kind"] ?: "home"
        path.startsWith("/api/follow") -> "follow"
        path.startsWith("/api/jav") || path.startsWith("/api/wants") || query["jav"] == "1" -> "jav"
        path == "/api/items" && query["state"] == "flagged" -> "flagged"
        path == "/api/items" -> "home"
        path.startsWith("/api/playlist") -> "playlists"
        else -> "manage"
    }
    fun entityKind(index: String) = mapOf("performers" to "performer", "tags" to "tag", "studios" to "studio", "creators" to "creator", "series" to "series", "agencies" to "agency")[index] ?: "performer"
}
