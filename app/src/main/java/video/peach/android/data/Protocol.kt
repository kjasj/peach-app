package video.peach.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONObject.values(): List<Pair<String, Any>> = keys().asSequence().map { it to get(it) }.toList()
fun JSONObject.text(vararg names: String): String = names.firstNotNullOfOrNull { name -> opt(name)?.takeIf { it != JSONObject.NULL && it !is JSONObject && it !is JSONArray && it.toString().isNotBlank() }?.toString() }.orEmpty()
fun JSONObject.flag(key: String): Boolean = when (val value = opt(key)) {
    is Boolean -> value
    is Number -> value.toInt() != 0
    is String -> value == "1" || value.equals("true", ignoreCase = true)
    else -> false
}

data class ProtocolField(val key: String, val type: String = "string", val default: Any? = null, val choices: List<String> = emptyList())
data class Endpoint(val path: String, val method: String, val title: String, val group: String, val fields: List<ProtocolField>, val localOnly: Boolean = false) {
    val destructive get() = listOf("delete", "trash/empty", "purge", "organize/apply", "resource-sync/apply", "links/prune").any { path.contains(it) }
}

/** UI 字段名集中在此，与 JSON 协议字段分别维护。 */
object Labels {
    private val fields = mapOf("loc" to "来源", "folder_recursive" to "包含子目录", "library" to "媒体库", "state" to "观看状态", "owner" to "归属", "len" to "片长", "dur_min" to "最短时长", "dur_max" to "最长时长", "orient" to "画面方向", "region" to "产地", "jav" to "内容分类", "thumb" to "预览图", "tag_match" to "标签匹配方式", "exclude_vertical" to "排除竖屏", "count" to "统计总数", "purpose" to "用途", "recursive" to "包含子目录", "future" to "新文件沿用规则", "expected_revision" to "当前规则版本", "plan_version" to "预览版本", "retry" to "重试失败项目", "speed_mode" to "处理速度", "metadata_only" to "只读取资料", "network" to "连接方式", "revoke" to "撤销凭据", "background" to "后台执行", "automatic" to "自动检查", "backfill_all" to "回抓全部历史", "rewind" to "重新开始", "older" to "更早的内容", "interval_minutes" to "间隔（分钟）", "defer_check" to "稍后检查", "lines" to "来源链接（每行一条）", "values" to "登录凭据", "to" to "设为状态", "content_types" to "内容类型", "code_format" to "番号格式", "endpoint" to "资料接口地址", "source_kind" to "列表类型", "source_seed_asset_id" to "推荐种子作品", "feed" to "新作编号", "disposal_reason" to "清理原因", "source_key" to "来源标识", "window" to "时间范围", "file_template" to "文件命名模板", "dir_template" to "目录命名模板", "confirm" to "我已确认预览与操作范围", "status_only" to "只查询状态", "check_id" to "检查批次", "restart" to "重新开始", "scan_id" to "扫描批次", "clean_cache" to "清理缓存", "stop" to "停止任务", "category" to "分类", "item_key" to "复核条目", "candidate_key" to "候选项目", "selected_ids" to "所选资料", "on" to "启用", "href" to "快捷入口地址", "saved" to "已保存", "ignored" to "已忽略", "coarse" to "每 30 秒", "precise" to "每 10 秒", "off" to "关闭", "peach" to "跟随 Peach 代理", "direct" to "直接连接", "peach_proxy" to "Peach 代理", "original" to "原始格式", "lowercase" to "小写", "compact" to "紧凑格式", "underscore" to "下划线分隔", "cookies_text" to "Cookie 文件内容", "avatar-choices" to "头像候选", "avatar-frame" to "提取头像画面", "avatar-pick" to "设置头像", "avatar-code-cover" to "使用作品封面头像", "crop" to "头像裁剪", "ref" to "图片引用", "version" to "版本", "canonical_name" to "规范名称")
    private val words = mapOf(
        "items" to "作品", "item" to "作品详情", "folders" to "媒体目录", "tasks" to "任务中心", "downloads" to "云下载",
        "resources/search" to "资源搜索", "jav/detail" to "作品资料", "jav/reviews" to "作品评价", "jav/image" to "资料图片", "jav/resources" to "作品资源", "jav/discoveries" to "作品发现",
        "media-processing" to "媒体处理", "library-processing" to "资料入库", "library-processing/issues" to "入库问题", "thumbnail-jobs" to "缩略图任务", "timeline" to "时间轴",
        "scraping" to "资料采集", "scraping/cover" to "封面补齐", "scraping/amane-bridge" to "采集桥接", "settings" to "业务偏好", "library-shortcuts" to "媒体库快捷方式",
        "feeds" to "新作订阅", "feeds/check" to "订阅更新检查", "feeds/discoveries" to "新作动态", "feeds/lookup" to "查找订阅人物",
        "follow" to "关注动态", "follow/credentials" to "来源凭据", "follow/tags" to "关注标签", "follow/authors" to "创作者", "follow/schedule" to "采集计划", "follow/check" to "更新检查", "follow/suggest" to "关注推荐", "follow/resolve" to "查找来源",
        "taste/refresh" to "口味分析任务", "links/prune" to "清理链接", "resource-sync/apply" to "同步结果", "media-repair" to "媒体修复", "orphan-records" to "失效记录", "organize" to "文件整理",
        "parts" to "分卷", "editions" to "版本", "entity" to "人物与厂牌", "entity/shapes" to "资料取景", "links" to "来源链接", "photos" to "人物图片", "photo-set" to "图片集", "index" to "资料索引",
        "duplicates" to "重复作品", "quality-goals" to "画质升级目标", "wants" to "想要清单", "wants/magnets" to "查找资源", "stats" to "馆藏统计", "tops" to "排行", "ads" to "垃圾队列", "related" to "相关作品",
        "custom-metadata" to "自定义资料来源", "playlists" to "播放列表", "playlist" to "播放列表管理", "facets" to "筛选分类", "suggest" to "搜索建议", "search-history" to "搜索历史", "taste" to "我的口味", "review" to "资料复核", "post-setup-tutorial" to "设置进度",
        "providers" to "服务状态", "sources" to "媒体来源", "libraries" to "媒体库", "configuration" to "服务器配置", "diagnostics" to "系统诊断",
        "id" to "编号", "ids" to "所选编号", "q" to "搜索", "query" to "搜索内容", "name" to "名称", "title" to "标题", "code" to "番号", "url" to "来源地址", "path" to "服务器路径", "root" to "根目录", "kind" to "类型", "type" to "类型", "action" to "操作", "limit" to "每页数量", "offset" to "起始位置", "sort" to "排序", "dir" to "排序方向", "status" to "状态", "source" to "来源", "source_id" to "来源编号", "asset_id" to "作品编号", "asset_ids" to "作品编号", "entity_id" to "资料编号", "entity_kind" to "资料类型", "provider" to "服务提供方", "enabled" to "启用", "liked" to "喜欢", "wanted" to "想要", "reason" to "备注", "rating" to "评分", "tags" to "标签", "tag" to "标签", "magnet" to "磁力链接", "target" to "目标目录", "origin" to "来源", "revision" to "数据版本", "mutation_revision" to "修改版本", "position" to "播放位置", "duration" to "时长", "delta" to "本次观看秒数", "ended" to "已播放结束", "seeks" to "跳转次数", "media" to "媒体序号", "mode" to "模式", "folder" to "目录", "operation" to "操作", "value" to "内容", "alias" to "别名", "aliases" to "别名", "canonical" to "规范名称", "canonical_name" to "规范名称", "author" to "创作者", "author_key" to "创作者", "credential" to "凭据", "token" to "访问令牌", "password" to "密码", "cookie" to "Cookie", "headers" to "请求头", "fields" to "字段", "preview" to "预览", "total" to "总数", "has_more" to "还有更多", "count" to "数量", "bytes" to "占用空间", "work_total" to "作品总数", "message" to "说明", "error" to "错误", "detail" to "详情", "created_at" to "创建时间", "updated_at" to "更新时间", "play_seconds" to "观看时长", "play_count" to "播放次数", "leave_ratio" to "离开位置", "creator" to "创作者", "studio" to "厂牌", "location" to "媒体来源", "release_date" to "发行日期", "width" to "宽度", "height" to "高度", "size" to "文件大小", "groups" to "分组", "sources" to "来源", "authors" to "创作者", "rows" to "记录", "records" to "记录", "results" to "结果", "data" to "内容", "summary" to "概览", "config" to "配置", "schedule" to "计划", "note" to "说明", "read_only" to "只读", "online" to "在线", "unread" to "未读", "errors" to "问题", "completed" to "已完成", "running" to "运行中", "failed" to "失败", "pending" to "等待中", "ok" to "成功",
        "create" to "创建", "add" to "添加", "remove" to "移除", "delete" to "删除", "rename" to "重命名", "reorder" to "调整顺序", "progress" to "进度", "reset" to "重置", "save" to "保存", "cancel" to "取消", "postprocess" to "重新后处理", "check" to "检查更新", "settings" to "设置", "source" to "来源", "discovery" to "动态状态", "author-alias" to "作者别名", "creator" to "创作者", "credential" to "登录凭据", "media/hide" to "隐藏媒体", "image-dims" to "图片尺寸", "play" to "播放记录", "activity" to "观看记录", "feedback" to "观看反馈", "watch-later" to "稍后看", "preference" to "喜欢与备注", "quality-goal" to "画质目标", "item-tag" to "作品标签", "batch" to "批量处理", "trash/empty" to "清空回收站", "organize/preview" to "预览整理方案", "organize/apply" to "执行整理", "organize/rollback" to "撤回上一批整理", "purge-missing" to "清理缺失资源", "orphan-records/attach" to "关联失效记录", "links/check" to "检查链接", "resource-sync/scan" to "扫描同步差异", "review/decision" to "复核决定", "review/genre" to "题材复核", "entity-name" to "修改资料名称", "entity-alias" to "编辑资料别名", "folders/preview" to "目录规则预览", "folders/rule" to "保存目录规则",
        "follow-name" to "按名字订阅", "follow" to "关注", "seen" to "标记看过", "dislike" to "不喜欢", "dispose" to "移入回收站", "restore" to "还原", "like" to "喜欢", "later" to "稍后看", "apply" to "执行", "rollback" to "撤回", "asc" to "升序", "desc" to "降序", "new" to "最新", "manual" to "手动", "video" to "视频", "image" to "图片", "performer" to "人物", "series" to "系列", "genre" to "题材", "label" to "标签", "template" to "命名模板", "plan" to "整理方案", "plan_id" to "方案编号", "batch_id" to "批次编号", "job_id" to "任务编号", "source_ids" to "来源编号", "provider_id" to "服务编号", "searchHistoryLimit" to "搜索历史条数", "followInitialDays" to "首次采集天数", "postSetupTutorialDone" to "已完成设置引导"
    )
    fun known(key: String) = key in fields || key in words || key in Presentation.labels
    fun of(key: String): String {
        val name = key.removePrefix("/api/")
        return Presentation.labels[name] ?: fields[name] ?: words[name] ?: name.split('/').filter(String::isNotBlank).joinToString(" · ") { Presentation.labels[it] ?: fields[it] ?: words[it] ?: it }
    }
}

class ProtocolRegistry(context: Context) {
    val endpoints: List<Endpoint>
    val commit: String
    val credentialGuides: Map<String, List<String>>
    init {
        val root = JSONObject(context.assets.open("protocol.json").bufferedReader().use { it.readText() })
        commit = root.getString("source_commit")
        credentialGuides = root.optJSONObject("credential_guides")?.values()?.associate { (key, value) ->
            val fields = (value as? JSONObject)?.optJSONArray("fields")
            key to (fields?.let { (0 until it.length()).map(it::getString) }.orEmpty())
        }.orEmpty()
        val dedicated = setOf("/api/avatar-choices", "/api/avatar-frame", "/api/avatar-pick", "/api/avatar-code-cover", "/api/providers/opencode-go/models")
        endpoints = root.getJSONArray("routes").objects().filter { it.optBoolean("dynamic") || it.optString("path") in dedicated }.map { row ->
            val path = row.getString("path")
            val fields = row.optJSONArray("fields")?.objects().orEmpty().map { field ->
                val choices = field.optJSONArray("choices")
                ProtocolField(field.getString("key"), field.optString("type", "string"), field.opt("default")?.takeUnless { it == JSONObject.NULL }, choices?.let { (0 until it.length()).map(it::getString) }.orEmpty())
            }
            Endpoint(path, row.getString("method"), Labels.of(path), group(path), fields, row.optBoolean("local_only"))
        } + listOf("providers", "sources", "libraries", "configuration", "diagnostics").map { Endpoint("/api/$it", "GET", Labels.of(it), "manage", emptyList(), it in listOf("configuration", "diagnostics")) }
    }
    fun get(path: String) = endpoints.firstOrNull { it.path == path && it.method == "GET" }
    fun post(path: String) = endpoints.firstOrNull { it.path == path && it.method == "POST" }
    fun actions(page: String): List<Endpoint> {
        val key = page.removePrefix("/api/")
        val family = when (key) { "items", "item", "duplicates", "ads", "quality-goals" -> "catalog"; "playlists", "playlist" -> "playlist"; "feed*" -> "feeds"; else -> key.substringBefore('/') }
        return endpoints.filter { it.method == "POST" && when (family) {
            "catalog" -> it.path.removePrefix("/api/") in listOf("feedback", "watch-later", "preference", "quality-goal", "item-tag", "batch", "trash/empty")
            "playlist" -> it.path == "/api/playlist"
            "follow" -> it.path.startsWith("/api/follow/") && !it.path.endsWith("activity") && !it.path.endsWith("image-dims") && !it.path.endsWith("play")
            "entity", "index", "photos", "photo-set", "avatar-choices" -> it.path in setOf("/api/entity-name", "/api/entity-alias", "/api/library-shortcuts", "/api/avatar-frame", "/api/avatar-pick", "/api/avatar-code-cover")
            "orphan-records" -> it.path in setOf("/api/orphan-records/attach", "/api/purge-missing")
            else -> it.path.removePrefix("/api/").substringBefore('/') == family
        } }
    }
    companion object {
        fun group(path: String): String = when {
            listOf("/api/follow", "/api/feeds").any(path::startsWith) -> "follow"
            listOf("/api/jav", "/api/wants", "/api/resources", "/api/taste", "/api/tops").any(path::startsWith) -> "discover"
            path.removePrefix("/api/") in listOf("items", "item", "folders", "parts", "editions", "entity", "photos", "photo-set", "index", "related", "playlists", "playlist", "facets", "suggest", "search-history") -> "library"
            else -> "manage"
        }
    }
}
