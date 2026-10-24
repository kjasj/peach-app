# 功能与协议对照表

源版本：`de22796c04919aaa057d77930c33c81313765569`。

此表是路由登记和有限证据索引，不是完整的页面/操作验收表。协议通过仅证明列出的合成用例；未登记的隐含前端状态、字段条件及外部来源仍需人工核对。

| 方法 | 接口 | 源处理器 | 安卓入口/权限 | 证据状态 |
|---|---|---|---|---|
| GET | `/api/media-processing` | `web_timeline_thumbnails.q_media_processing` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 media-processing |
| GET | `/api/folders` | `web_folders.q_folders` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 folders |
| GET | `/api/tasks` | `web_tasks.q_tasks` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 tasks |
| GET | `/api/downloads` | `web_downloads.q_downloads` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 downloads |
| GET | `/api/resources/search` | `web_resource_search.q_resource_search` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/jav/detail` | `web_jav_detail.q_jav_detail` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/jav/reviews` | `web_jav_detail.q_jav_reviews` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/jav/image` | `web_jav_detail.q_jav_image` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/jav/resources` | `web_jav_detail.q_jav_resources` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/jav/discoveries` | `web_jav_discovery.q_jav_discoveries` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/library-processing` | `web_library_processing.q_library_processing` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 library-processing |
| GET | `/api/library-processing/issues` | `web_library_processing.q_library_processing_issues` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：缺少任务参数时拒绝 |
| GET | `/api/thumbnail-jobs` | `web_timeline_thumbnails.q_thumbnail_jobs` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 thumbnail-jobs |
| GET | `/api/timeline` | `web_timeline_thumbnails.q_timeline` | 原生播放器 | 协议用例通过：读取 timeline |
| GET | `/api/scraping` | `web_scraping.q_scraping` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 scraping |
| GET | `/api/scraping/cover` | `web_router.lambda` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 scraping/cover |
| GET | `/api/scraping/amane-bridge` | `web_scraping.q_scraping_amane_bridge` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/settings` | `web_settings.q_settings` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 settings |
| GET | `/api/library-shortcuts` | `web_library_shortcuts.q_library_shortcuts` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 library-shortcuts |
| GET | `/api/feeds` | `web_feeds.q_feeds` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 feeds |
| GET | `/api/feeds/check` | `web_feeds.q_feed_check` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 feeds/check |
| GET | `/api/feeds/discoveries` | `web_feeds.q_feed_discoveries` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 feeds/discoveries |
| GET | `/api/feeds/lookup` | `web_feeds.q_feed_lookup` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/follow` | `web_follow.q_follow` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 follow |
| GET | `/api/follow/credentials` | `web_follow.q_follow_credentials` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 follow/credentials |
| GET | `/api/follow/tags` | `web_follow.q_follow_tags` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 follow/tags |
| GET | `/api/follow/authors` | `web_follow.q_follow_authors` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 follow/authors |
| GET | `/api/follow/schedule` | `web_follow.q_follow_schedule` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 follow/schedule |
| GET | `/api/follow/check` | `web_follow.q_follow_check` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 follow/check |
| GET | `/api/follow/suggest` | `web_router._get_follow_suggest` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/follow/resolve` | `web_router.lambda` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/taste/refresh` | `web_router.lambda` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 taste/refresh |
| GET | `/api/links/prune` | `web_router.lambda` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 links/prune |
| GET | `/api/resource-sync/apply` | `web_router.lambda` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 resource-sync/apply |
| GET | `/api/media-repair` | `web_media_repair.q_media_repair` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 media-repair |
| GET | `/api/orphan-records` | `web_orphan_records.q_orphan_records` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 orphan-records |
| GET | `/api/organize` | `web_organize.q_organize` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 organize |
| GET | `/api/items` | `web_catalog.q_items` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：馆藏列表；协议用例通过：分页；协议用例通过：搜索 |
| GET | `/api/item` | `web_router._get_item` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：作品详情 |
| GET | `/api/parts` | `web_catalog.q_parts` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 parts |
| GET | `/api/editions` | `web_catalog.q_editions` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：没有版本分组时给出原因 |
| GET | `/api/entity` | `web_entity.q_entity` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/entity/shapes` | `web_router._get_entity_shapes` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/links` | `web_links.w_links` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/photos` | `web_entity.q_entity_photos` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/photo-set` | `web_entity.q_photo_set` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/index` | `web_router._get_index` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/duplicates` | `web_batch.q_duplicates` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 duplicates |
| GET | `/api/quality-goals` | `web_stats.q_quality_goals` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 quality-goals |
| GET | `/api/wants` | `web_wants.q_wants` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 wants |
| GET | `/api/wants/magnets` | `web_wants.q_want_magnets` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/stats` | `web_router._get_stats` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 stats |
| GET | `/api/tops` | `web_router._get_tops` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/ads` | `web_router._get_ads` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 ads |
| GET | `/api/related` | `web_router._get_related` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 related |
| GET | `/api/custom-metadata` | `web_custom_metadata.q_custom_metadata` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 custom-metadata |
| GET | `/api/playlists` | `web_playlists.q_playlists` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 playlists |
| GET | `/api/playlist` | `web_playlists.q_playlist` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取列表 |
| GET | `/api/facets` | `web_router._get_facets` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 facets |
| GET | `/api/suggest` | `web_router._get_suggest` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/search-history` | `web_router._get_search_history` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 search-history |
| GET | `/api/taste` | `web_router._get_taste` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 taste |
| GET | `/api/review` | `web_router._get_review` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 review |
| GET | `/api/post-setup-tutorial` | `web_router._get_post_setup_tutorial` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：读取 post-setup-tutorial |
| POST | `/api/media-processing` | `web_timeline_thumbnails.w_media_processing` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/folders/preview` | `web_folders.w_folder_preview` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/folders/rule` | `web_folders.w_folder_rule` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/downloads` | `web_downloads.w_download_submit` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/downloads/cancel` | `web_downloads.w_download_cancel` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/downloads/postprocess` | `web_downloads.w_download_postprocess` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/library-processing` | `web_library_processing.w_library_processing` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/thumbnail-jobs` | `web_timeline_thumbnails.w_thumbnail_jobs` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/scraping/settings` | `web_scraping.w_scraping_settings` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/scraping/cover` | `web_scraping.w_scraping_cover` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/scraping/check` | `web_scraping.w_scraping_check` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/scraping/amane-bridge/check` | `web_scraping.w_scraping_amane_check` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/scraping/amane-bridge/rebuild` | `web_scraping.w_scraping_amane_rebuild` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/feeds/check` | `web_feeds.w_feed_check` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/feeds/source` | `web_feeds.w_feed_source` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/feeds/discovery` | `web_feeds.w_feed_discovery` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/check` | `web_follow.w_follow_check` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/schedule` | `web_follow.w_follow_schedule` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/source` | `web_follow.w_follow_source` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/author-alias` | `web_follow.w_follow_author_alias` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/creator` | `web_follow.w_follow_creator` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/resolve` | `web_follow.w_follow_resolve` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/credential` | `web_follow.w_follow_credential` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：来源凭据撤销空白测试 |
| POST | `/api/follow/status` | `web_follow.w_follow_status` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/media/hide` | `web_follow.w_follow_media_hide` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/image-dims` | `web_follow.w_follow_image_dims` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/save` | `web_follow.w_follow_save` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/follow/play` | `web_follow.w_follow_play` | 原生播放器 | 未验收 |
| POST | `/api/follow/activity` | `web_follow.w_follow_activity` | 原生播放器 | 未验收 |
| POST | `/api/activity` | `web_activity.w_activity` | 原生播放器 | 协议用例通过：观看累计 |
| POST | `/api/play` | `web_activity.w_play` | 原生播放器 | 协议用例通过：播放计数 |
| POST | `/api/feedback` | `web_activity.w_feedback` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：评分 |
| POST | `/api/watch-later` | `web_activity.w_watch_later` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：稍后看 |
| POST | `/api/custom-metadata` | `web_custom_metadata.w_custom_metadata` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/playlist` | `web_playlists.w_playlist` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：创建列表；协议用例通过：重命名列表；协议用例通过：删除测试列表 |
| POST | `/api/preference` | `web_activity.w_preference` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：喜欢 |
| POST | `/api/quality-goal` | `web_activity.w_quality_goal` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：画质目标 |
| POST | `/api/wants` | `web_wants.w_wants` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/item-tag` | `web_catalog.w_item_tag` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/batch` | `web_batch.w_batch` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/search-history` | `web_stats.w_search_history` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/taste/refresh` | `web_stats.w_taste_refresh` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/taste/source` | `web_stats.w_taste_source` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/trash/empty` | `web_router._post_empty_trash` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/organize/preview` | `web_organize.w_organize_preview` | 管理 → 文件整理 → 预览 | 原生合成预览和执行参数复用通过；执行与回滚另行验收 |
| POST | `/api/organize/apply` | `web_organize.w_organize_apply` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/organize/rollback` | `web_organize.w_organize_rollback` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/purge-missing` | `web_resource_sync.w_purge_missing` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/orphan-records/attach` | `web_orphan_records.w_orphan_attach` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/links/check` | `web_links.w_links_check` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/links/prune` | `web_links.w_links_prune` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/resource-sync/scan` | `web_resource_sync.w_resource_sync_scan` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/resource-sync/apply` | `web_resource_sync.w_resource_sync_apply` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/media-repair` | `web_media_repair.w_media_repair` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/review/decision` | `web_review.w_review_decision` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/review/genre` | `web_review.w_review_genre` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/settings` | `web_settings.w_settings` | 对应领域 → 全部功能 / 记录操作表单 | 协议用例通过：保存业务偏好 |
| POST | `/api/library-shortcuts` | `web_library_shortcuts.w_library_shortcuts` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/entity-name` | `web_entity.w_entity_name` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| POST | `/api/entity-alias` | `web_entity.w_entity_alias` | 对应领域 → 全部功能 / 记录操作表单 | 未验收 |
| GET | `/api/providers` | `routes_api.provider_health` | 专用路由：需人工核对入口和载荷 | 协议用例通过：读取 providers |
| GET | `/api/sources` | `routes_api.source_health` | 专用路由：需人工核对入口和载荷 | 协议用例通过：读取 sources |
| GET | `/api/libraries` | `routes_api.media_library_list` | 连接与认证 | 协议用例通过：读取 libraries |
| POST | `/api/reveal` | `routes_api.reveal` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/api/providers/opencode-go/models` | `routes_api.opencode_go_models` | 专用路由：需人工核对入口和载荷 | 未验收 |
| POST | `/api/taste/import` | `routes_api.taste_import` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/api/tasks/{run_id}` | `routes_api.task_run_detail` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/api/{route:path}` | `routes_api.api_get` | 专用路由：需人工核对入口和载荷 | 协议用例通过：馆藏列表；协议用例通过：分页；协议用例通过：搜索；协议用例通过：作品详情；协议用例通过：读取 libraries；协议用例通过：读取 providers；协议用例通过：读取 sources；协议用例通过：读取 facets；协议用例通过：读取 tasks；协议用例通过：读取 downloads；协议用例通过：读取 folders；协议用例通过：读取 playlists；协议用例通过：读取 feeds；协议用例通过：读取 follow；协议用例通过：读取 settings；协议用例通过：读取 library-shortcuts；协议用例通过：读取 stats；协议用例通过：读取 taste；协议用例通过：读取 library-processing；协议用例通过：读取 media-processing；协议用例通过：读取 thumbnail-jobs；协议用例通过：读取 media-repair；协议用例通过：读取 orphan-records；协议用例通过：读取 organize；协议用例通过：读取 duplicates；协议用例通过：读取 ads；协议用例通过：读取 quality-goals；协议用例通过：读取 wants；协议用例通过：读取 search-history；协议用例通过：读取 custom-metadata；协议用例通过：读取 scraping；协议用例通过：读取 review；协议用例通过：读取 post-setup-tutorial；协议用例通过：读取 related；协议用例通过：读取 parts；协议用例通过：读取 timeline；协议用例通过：没有版本分组时给出原因；协议用例通过：读取列表；协议用例通过：播放计划；协议用例通过：HLS 播放计划 |
| POST | `/api/{route:path}` | `routes_api.api_post` | 专用路由：需人工核对入口和载荷 | 协议用例通过：喜欢；协议用例通过：评分；协议用例通过：稍后看；协议用例通过：画质目标；协议用例通过：播放计数；协议用例通过：观看累计；协议用例通过：创建列表；协议用例通过：重命名列表；协议用例通过：删除测试列表；协议用例通过：保存业务偏好；协议用例通过：取消播放会话 |
| GET | `/login` | `routes_auth.login` | 连接与认证 | 未验收 |
| POST | `/login` | `routes_auth.login_submit` | 连接与认证 | 未验收 |
| GET | `/api/diagnostics` | `routes_configuration.system_diagnostics` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/diagnostics/bundle` | `routes_configuration.diagnostic_bundle` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/diagnostics/client-error` | `routes_configuration.client_error` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/configuration` | `routes_configuration.read_configuration` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/configuration/automatic-updates` | `routes_configuration.read_automatic_updates` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/remote-reporting` | `routes_configuration.save_remote_reporting` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/configuration/library-sharing/preview` | `routes_configuration.preview_library_sharing` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/library-sharing` | `routes_configuration.save_library_sharing` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/automatic-updates` | `routes_configuration.save_automatic_updates` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/peach-proxy` | `routes_configuration.save_peach_proxy` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/entry-links` | `routes_configuration.save_entry_links` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/push-discovery` | `routes_configuration.save_push_discovery` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/push-discovery/secret` | `routes_configuration.rotate_push_discovery_secret` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/configuration/indexers` | `routes_configuration.get_indexers` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/indexers` | `routes_configuration.save_indexers` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/downloads` | `routes_configuration.save_downloads` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/configuration/downloads/check` | `routes_configuration.check_saved_downloads` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/downloads/check` | `routes_configuration.check_downloads` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/downloads/folder` | `routes_configuration.create_download_folder` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/downloads/pikpak/login` | `routes_configuration.login_pikpak` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/downloads/pikpak/browser-login` | `routes_configuration.start_pikpak_browser_login` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/configuration/downloads/pikpak/browser-login` | `routes_configuration.pikpak_browser_login_status` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/downloads/pikpak/browser-login/cancel` | `routes_configuration.cancel_pikpak_browser_login` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/downloads/pikpak/logout` | `routes_configuration.logout_pikpak` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/startup` | `routes_configuration.save_startup` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/uninstall` | `routes_configuration.uninstall` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/configuration/updates` | `routes_configuration.check_updates` | 电脑端限制（不计安卓实现） | 未验收 |
| GET | `/api/configuration/update-status` | `routes_configuration.update_status` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/update` | `routes_configuration.download_update` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/update-restart` | `routes_configuration.restart_update` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/access` | `routes_configuration.save_access` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration/tunnel` | `routes_configuration.save_tunnel` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/pick-folder` | `routes_configuration.pick_folder` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/configuration` | `routes_configuration.save_configuration` | 电脑端限制（不计安卓实现） | 未验收 |
| POST | `/api/external-playback` | `routes_media.external_playback` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/external-stream/{id}` | `routes_media.external_stream` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/external-stream/{id}` | `routes_media.external_stream` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/external-playlist/{id}.m3u` | `routes_media.external_playlist` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/stream` | `routes_media.stream` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/stream` | `routes_media.stream` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/api/stream-plan` | `routes_media.stream_plan` | 原生播放器 | 协议用例通过：播放计划；协议用例通过：HLS 播放计划 |
| GET | `/strm-resource/{id}/{key}` | `routes_media.strm_resource` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/strm-resource/{id}/{key}` | `routes_media.strm_resource` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/stream/hls/{id}/index.m3u8` | `routes_media.hls_playlist` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/stream/hls/{id}/{index}.ts` | `routes_media.hls_segment` | 专用路由：需人工核对入口和载荷 | 未验收 |
| POST | `/api/stream-cancel` | `routes_media.stream_cancel` | 原生播放器 | 协议用例通过：取消播放会话 |
| GET | `/api/assets/{id}/subtitles` | `routes_media.asset_subtitles` | 专用路由：需人工核对入口和载荷 | 协议用例通过：本地字幕 |
| GET | `/api/assets/{id}/subtitles/{index}` | `routes_media.asset_subtitle_track` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/api/assets/{id}/subtitles/{index}` | `routes_media.asset_subtitle_track` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/thumb` | `routes_media.thumbnail` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/thumb` | `routes_media.thumbnail` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/photo` | `routes_media.photo` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/photo` | `routes_media.photo` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/photo-thumb` | `routes_media.photo_thumb` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/photo-thumb` | `routes_media.photo_thumb` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/sample-thumb` | `routes_media.sample_thumb` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/sample-thumb` | `routes_media.sample_thumb` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/sample-image` | `routes_media.sample_image` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/sample-image` | `routes_media.sample_image` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/poster` | `routes_media.poster` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/poster` | `routes_media.poster` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/timeline` | `routes_media.timeline` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/timeline` | `routes_media.timeline` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/cover` | `routes_media.cover` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/cover` | `routes_media.cover` | 专用路由：需人工核对入口和载荷 | 未验收 |
| POST | `/api/cover-crop` | `routes_media.cover_crop` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/endcard-frame` | `routes_media.endcard_frame` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/endcard-frame` | `routes_media.endcard_frame` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/avatar` | `routes_media.avatar` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/avatar` | `routes_media.avatar` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/follow-avatar` | `routes_media.follow_avatar` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/follow-avatar` | `routes_media.follow_avatar` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/source-icon` | `routes_media.source_icon` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/source-icon` | `routes_media.source_icon` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/work-icon` | `routes_media.work_icon` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/work-icon` | `routes_media.work_icon` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/follow-qualities` | `routes_media.follow_qualities` | 原生播放器 | 未验收 |
| GET | `/follow-stream` | `routes_media.follow_stream` | 原生播放器 | 未验收 |
| HEAD | `/follow-stream` | `routes_media.follow_stream` | 原生播放器 | 未验收 |
| GET | `/follow-image` | `routes_media.follow_image` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/follow-image` | `routes_media.follow_image` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/jav-preview/{code}/{ref}` | `routes_media.jav_preview` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/jav-preview/{code}/{ref}` | `routes_media.jav_preview` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/follow-cover` | `routes_media.follow_cover` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/follow-cover` | `routes_media.follow_cover` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/logo` | `routes_media.logo` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/logo` | `routes_media.logo` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/link-mark` | `routes_media.link_mark` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/link-mark` | `routes_media.link_mark` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/site-mark` | `routes_media.site_mark` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/site-mark` | `routes_media.site_mark` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/entity-image` | `routes_media.entity_image` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/entity-image` | `routes_media.entity_image` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/api/avatar-choices` | `routes_media.avatar_choices` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/avatar-choice` | `routes_media.avatar_choice` | 专用路由：需人工核对入口和载荷 | 未验收 |
| HEAD | `/avatar-choice` | `routes_media.avatar_choice` | 专用路由：需人工核对入口和载荷 | 未验收 |
| POST | `/api/avatar-frame` | `routes_media.avatar_frame` | 专用路由：需人工核对入口和载荷 | 未验收 |
| POST | `/api/avatar-pick` | `routes_media.avatar_pick` | 专用路由：需人工核对入口和载荷 | 未验收 |
| POST | `/api/avatar-code-cover` | `routes_media.avatar_code_cover` | 专用路由：需人工核对入口和载荷 | 未验收 |
| POST | `/api/subtitles/convert` | `routes_subtitles.convert_subtitle` | 原生播放器 | 未验收 |
| GET | `/api/subtitles/search` | `routes_subtitles.search_subtitles` | 原生播放器 | 未验收 |
| GET | `/api/subtitles/subtitlecat/{subtitle_id}` | `routes_subtitles.subtitle_files` | 专用路由：需人工核对入口和载荷 | 未验收 |
| GET | `/api/subtitles/subtitlecat/{subtitle_id}/{index}` | `routes_subtitles.online_subtitle` | 专用路由：需人工核对入口和载荷 | 未验收 |
