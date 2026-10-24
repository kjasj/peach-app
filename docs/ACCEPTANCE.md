# 业务验收用例与剩余工作

这是当前可复现的验收入口，不代表已经穷尽原网页操作。完整逐按钮对照与外部来源全流程仍未完成，不能据此宣布全部功能迁移通过。每项在合成、可回滚的测试环境操作。

| 功能 | 安卓入口 | 实际协议 | 验收用例 | 当前证据 |
|---|---|---|---|---|
| 连接认证 | 首次连接 / 切换服务器 | `/healthz`, `/login`, `/api/libraries` | 错误口令失败；正确口令 303 后能读列表；会话失效提示重新登录；切换后数据隔离 | 自动认证测试、合成原生流程、真实只读读取通过；多服务器完整 UI 交互待验收 |
| 馆藏、搜索、分页 | 馆藏 → 搜索 / 筛选 | `/api/items`, `/api/facets`, `/api/folders` | 筛选返回正确记录；下一页追加且不跳过；空结果可恢复 | 合成搜索与分页协议通过；全部筛选组合待验收 |
| 详情与关联 | 点按封面 → 作品详情 | `/api/item`, `/api/related`, `/api/editions`, `/api/parts` | 展示详情；切换分卷；无版本分组给出空状态 | 原生详情及分卷协议通过；多版本正向场景待验收 |
| 喜欢、评分、稍后看 | 详情按钮 / 长按卡片 → 操作 | `/api/preference`, `/api/feedback`, `/api/watch-later`, `/api/quality-goal` | 写入后重新读取能看到结果；失败不显示成功 | 合成写入协议通过 |
| 播放列表 | 全部功能 → 播放列表 | `/api/playlists`, `/api/playlist` | 创建、加作品、重命名、重排、删除 | 创建/读取/重命名/删除协议通过；全部原生交互待验收 |
| 实体、别名与图片 | 全部功能 → 资料索引 / 人物与厂牌 | `/api/index`, `/api/entity`, `/api/entity-name`, `/api/entity-alias`, `/api/photos`, `/api/photo-set`, `/api/avatar-*` | 名称与别名更新；选择头像；图片可缩放；修改失败保留输入 | 原生入口和结构化表单已接入，全流程待验收 |
| 发现、订阅与想要 | 发现 / 全部功能 | `/api/jav/*`, `/api/feeds/*`, `/api/wants`, `/api/resources/search` | 新作筛选、查找资源、订阅更新与想要状态持久化 | 部分读取协议通过；外部数据与写入待验收 |
| 云下载 | 管理 → 云下载 | `/api/downloads`, `/api/downloads/cancel`, `/api/downloads/postprocess` | 合法磁力提交一次；任务进度、取消、失败、后处理可见 | 状态读取通过；115/PikPak 全流程待验收 |
| 关注与来源 | 关注 / 全部功能 | `/api/follow`, `/api/follow/source`, `/api/follow/creator`, `/api/follow/author-alias`, `/api/follow/status` | 主条目、版本、重复项均可打开；分组别名修改；关注状态更新 | 分组投影单元测试、真实读取通过；全部业务写入待验收 |
| 关注凭据和计划 | 关注 → 来源凭据 / 采集计划 | `/api/follow/credential`, `/api/follow/schedule`, `/api/follow/check` | 按来源填写凭据；有效、过期与撤销状态一致；后台任务退出页面继续 | 合成空凭据撤销和状态读取通过；有效账号场景待验收 |
| 资料入库与刮削 | 管理 → 资料入库 / 资料采集 | `/api/library-processing`, `/api/scraping/*`, `/api/thumbnail-jobs`, `/api/media-processing` | 提交任务、进度、失败条目、服务器提供的停止和恢复；退出页面不取消 | 读取协议与缺失任务参数拒绝通过；全流程待验收 |
| 自定义资料与复核 | 管理 → 自定义资料来源 / 资料复核 | `/api/custom-metadata`, `/api/review/*` | 字段校验、候选选择、复核决定及冲突反馈正确 | 读取协议通过；业务写入待验收 |
| 整理与回滚 | 管理 → 文件整理 | `/api/organize/preview`, `/api/organize/apply`, `/api/organize/rollback` | 先看预览，再确认执行；只改夹具文件；回滚恢复路径及记录 | 合成原生预览与执行参数复用通过；执行、回滚端到端验收待完成 |
| 重复项、垃圾与失效记录 | 管理 → 对应功能 | `/api/duplicates`, `/api/ads`, `/api/trash/empty`, `/api/orphan-records`, `/api/orphan-records/attach`, `/api/purge-missing` | 选中范围、二次确认、执行结果与刷新一致 | 读取协议通过；清理和关联写入待验收 |
| 同步、链接与修复 | 管理 → 对应功能 | `/api/resource-sync/*`, `/api/links/*`, `/api/media-repair` | 扫描差异、确认执行、只读冲突、来源离线、任务失败恢复 | 状态读取通过；完整任务流程待验收 |
| 统计、口味与历史导入 | 全部功能 → 馆藏统计 / 我的口味；客户端设置 → 导入 | `/api/stats`, `/api/taste`, `/api/taste/import`, `/api/search-history` | 正确统计、来源设置、系统文件选择、无权限/大文件错误反馈 | 统计与口味读取通过；导入写入和全部交互待验收 |
| MP4 / HLS | 详情 → 开始播放 | `/api/stream-plan`, `/stream`, `/stream/hls/*`, `/api/stream-cancel` | Range 206、真实 HLS 片段、退出取消；每次播放使用独立会话 | 合成媒体实际播放、取消接口通过 |
| 播放手势 | 播放画面与控制栏 | 客户端手势状态机 + Media3 | 长按 350ms 触发；松手/取消/后台恢复；拖动不加速；控件不触发画面手势 | 关键状态机和真实长按通过；所有边界组合仍待验收 |
| 字幕、版本与画质 | 播放设置 / 选集 | `/api/assets/{id}/subtitles`, `/api/subtitles/*`, `/follow-qualities`, `/follow-stream?quality=` | 字幕轨道、样式、手机文件导入、在线选择、分卷/清晰度切换 | 本地字幕实际加载和样式通过；在线与关注清晰度待验收 |
| 全屏、小窗与画中画 | 播放器图标 | Activity + MediaSession | 切换保持同一播放器、位置与倍率；离开释放资源 | 模拟器全屏/小窗/系统画中画往返通过；手机及长时间资源表现待验收 |
| 服务器本机操作 | 功能中的电脑限制提示 | 原有本机配置与桌面入口 | 明确在 Mac 完成，不绕过远程权限 | 不计入安卓功能通过数量 |

错误覆盖现状：401/403/409/429/503、缺少参数、缺少可选字段、HTTP 200 中的写入错误已有单元或合成证据；各外部来源的离线、限流、账号失效与任务失败语义还需逐个验证。
