package video.peach.android.data

/** 用户界面使用业务名称；接口状态和内部运行标识分别处理。 */
object Presentation {
    val labels = mapOf(
        "counts" to "数量统计", "stats" to "统计", "finished" to "最近完成", "skipped" to "未执行", "runs" to "任务记录",
        "run" to "任务详情", "task_key" to "任务类型", "task_label" to "任务名称", "trigger" to "启动方式",
        "started_at" to "开始时间", "finished_at" to "结束时间", "elapsed_seconds" to "用时（秒）",
        "progress_current" to "已处理", "progress_total" to "待处理总数", "progress_label" to "当前进度",
        "result_summary" to "处理结果", "followups" to "后续任务", "parent" to "上级任务", "followup_counts" to "后续任务统计",
        "available" to "可用", "active" to "进行中", "succeeded" to "已完成", "cancelled" to "已取消", "interrupted" to "已中断",
        "source_label" to "关注来源", "provider_label" to "来源平台", "published_at" to "发布时间", "checked_at" to "最近检查",
        "last_checked_at" to "最近检查", "last_success_at" to "最近成功", "last_error" to "最近错误", "next_check_at" to "下次检查",
        "media_kind" to "内容类型", "media_items" to "媒体附件", "variant_label" to "版本说明", "variant_kind" to "版本类型",
        "playable" to "可播放", "has_media" to "有媒体附件", "media_error" to "媒体读取问题", "media_needs_credential" to "需要来源登录",
        "resource_urls" to "资源链接", "resource_provider" to "资源平台", "detail_tags" to "全部标签", "credit" to "发布署名",
        "audio" to "音轨", "description" to "简介", "display_title" to "显示标题", "catalog_title" to "作品标题",
        "is_jav" to "番号作品", "has_cover" to "已取得封面", "has_thumb" to "已取得预览图", "watch_later" to "稍后看",
        "performers" to "艺人", "studios" to "厂牌", "biography" to "人物简介", "birth_date" to "出生日期",
        "nationality" to "国籍", "debut_date" to "出道日期", "website" to "官方网站", "links" to "相关链接", "images" to "图片",
        "n" to "作品数量", "k" to "名称", "items_count" to "内容数量", "unseen" to "未看", "saved" to "已收藏",
        "interval_seconds" to "检查间隔（秒）", "last_run" to "最近执行", "next_run" to "下次执行", "initial_days" to "首次采集天数",
        "all" to "全部", "before_finished_at" to "更早任务的结束时间", "before_id" to "更早任务的编号", "box" to "取景区域",
        "c" to "预览画面", "clear_token" to "清除访问口令", "clouddrive_address" to "云盘服务地址", "confirm_disable" to "确认停用",
        "confirmation" to "确认内容", "current_password" to "当前口令", "delete_data" to "删除数据", "desktop" to "桌面集成",
        "direction" to "排序方向", "domain" to "域名", "download" to "下载", "entries" to "条目", "goal" to "画质目标",
        "hidden" to "隐藏", "hostname" to "主机名", "initial" to "首次采集", "key" to "标识", "max_size" to "最大文件大小",
        "media_dirs" to "媒体目录", "media_sources" to "媒体来源", "metadata_cancel" to "取消资料任务", "metadata_cursor" to "资料任务位置",
        "metadata_job_id" to "资料任务编号", "metadata_refresh" to "刷新资料", "min_size" to "最小文件大小", "next" to "下一页",
        "organizeTemplates" to "整理模板", "page" to "页码", "pikpak_account" to "PikPak 账号", "pikpak_root" to "PikPak 目录",
        "port" to "端口", "proxy" to "代理", "quality" to "清晰度", "refresh" to "刷新", "remember" to "记住连接",
        "route" to "路由", "run_id" to "任务编号", "s" to "查询内容", "scan_now" to "立即扫描", "scope" to "处理范围",
        "scope_kind" to "范围类型", "scope_name" to "范围名称", "seed" to "随机排序种子", "service" to "服务",
        "session" to "播放会话", "sidebarOrder" to "菜单顺序", "silent" to "静默执行", "sites" to "来源站点", "stage" to "当前阶段",
        "subtitle_id" to "字幕编号", "ticket" to "临时访问票据", "tunnel_notice" to "远程访问提示", "types" to "内容类型",
        "username" to "用户名", "variant" to "版本", "work" to "作品", "follow-check" to "追更检查", "follow-resolve" to "关注来源查找",
        "library-processing" to "扫描与采集", "scraping-cover" to "封面采集", "resource-scan" to "资源同步检查",
        "follow/source" to "添加或编辑来源", "follow/author-alias" to "创作者别名", "follow/creator" to "关联创作者",
        "follow/credential" to "配置来源登录", "follow/settings" to "关注偏好", "follow/discovery" to "标记动态状态",
        "amane-bridge" to "采集桥接", "hide" to "隐藏", "rebuild" to "重新生成"
    )
    val hidden = setOf("token", "cookie", "password", "authorization", "secret", "credentials", "field_owners", "metadata_json", "mutation_revision",
        "statuses", "result_versions", "mutex_key", "pid", "host", "heartbeat_at", "root_run_id", "parent_run_id", "followup_key", "followup_depth",
        "cover_key", "cover_version", "cover_frame", "poster_box", "has_local_poster", "external_id", "published_precision", "media_type", "tag_types",
        "alias_suggestions", "author_facets", "facets", "providers", "seed", "filtered", "finished_has_more")
    private val enumKeys = setOf("status", "last_status", "state", "trigger", "media_kind", "medium", "variant_kind", "location", "sort", "dir", "stage", "task_key")
    private val statuses = mapOf("succeeded" to "已完成", "running" to "运行中", "pending" to "等待中", "queued" to "排队中", "failed" to "失败",
        "cancelled" to "已取消", "interrupted" to "已中断", "idle" to "空闲", "ready" to "就绪", "done" to "已完成", "partial" to "部分完成",
        "unavailable" to "暂不可用", "missing" to "未取得", "offline" to "离线", "local" to "本地磁盘", "online" to "在线来源",
        "checking" to "检查中", "fetching" to "读取中", "downloading" to "下载中", "paused" to "已暂停", "completed" to "已完成", "success" to "成功", "not_modified" to "暂无更新",
        "manual" to "手动", "scheduled" to "定时", "startup" to "启动时", "cli" to "命令行", "new" to "最新", "unseen" to "未看",
        "seen" to "已看", "ignored" to "已忽略", "saved" to "已收藏", "downloaded" to "已下载", "original" to "原版", "external" to "外部资源")
    fun value(key: String, raw: Any): String = if (raw is Boolean) if (raw) "是" else "否"
        else if (key in enumKeys) statuses[raw.toString()] ?: labels[raw.toString()] ?: Labels.of(raw.toString()) else raw.toString()
}
