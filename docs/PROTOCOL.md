# Peach 安卓协议说明

基线：`de22796c04919aaa057d77930c33c81313765569`。`protocol-inventory.json` 记录源码处理器、前端引用和提取字段；共 238 个方法/路由组合，其中动态 GET 64、POST 60。静态提取不能推断所有响应结构、条件字段或业务权限，因此不是 OpenAPI 的替代规格或完整验收证据。

`RESPONSE_SAMPLES.json` 保存 7 组实际合成服务响应（馆藏、详情、分卷、字幕、任务及播放计划等），路径和会话标识已替换；高级业务响应仍需继续补齐，不将这些示例当作全局通用结构。

## 连接与认证

- 根地址只接受 HTTP/HTTPS，不允许把用户名、密码、查询参数或路径写在地址中。
- 首次读取 `GET /healthz`。认证使用 `POST /login`，表单字段 `token`、`days`、`next=/`，正常返回 303 并设置会话 Cookie。随后读取 `/api/libraries` 验证权限。
- 会话使用 Android Keystore AES-GCM 保存。每个服务器使用独立 Cookie 存储和 Room 键空间，Cookie 限定协议、主机和端口。编辑地址产生新的服务器身份。
- API、图片、媒体和字幕使用相同网络上下文；写请求携带服务器 Origin。跨来源图片不携带当前服务 Cookie。
- 私有 CA 只扩展当前连接的信任链，保留主机名验证。HTTP 由用户明确输入，界面持续显示未加密状态。

## 请求、错误和缓存

- 动态读取：`GET /api/...`，查询参数按处理器要求编码。分页通常使用 `limit/offset`，但不能将它假定为所有路由通用规范。
- 动态写入：`POST /api/...`，JSON 对象。字段类型、枚举及部分默认值来自源码与人工覆盖；后端仍负责最终校验。
- 兼容非 2xx JSON 错误及 200 中的 `error`。401 要求重新登录；403 显示服务权限或电脑限制；409 要求刷新；429 提示等待；503 提示离线。写失败不显示成功。
- POST 不做自动重试，累计观看时长、下载与批量操作尤其不能重放。读取和媒体可重连；写请求使用新连接避免复用已失效的空闲连接。
- Room 保存成功读取的列表缓存。离线时明确显示缓存状态；401/403 不回退到缓存掩盖权限问题。长任务每 5 秒查询已有状态，页面退出不取消服务器任务。
- 整理、清理等操作保留服务器预览、执行、回滚入口及客户端确认。当前高级表单未逐项完成全流程验收。

## 馆藏与播放

- `/api/items` 返回馆藏列表；`/api/item?id=` 读取详情。多版本与分卷分别使用 `/api/editions?id=` 和 `/api/parts?id=`；未成组时服务器返回具体错误，客户端展示空状态。
- 先读 `/api/stream-plan?id=&session=&mode=`，使用其 `src`、`mime_type` 与 `protocol`；不构造本地文件路径。`session` 每次独立，关闭或换片 POST `/api/stream-cancel?session=`。
- 原服务对可直接播放的本地 MP4 即使 `mode=hls` 也可能返回 Range；MKV 等容器可返回 HLS。客户端不能把“请求 HLS”当成“响应必为 HLS”。取消过的会话不可复用，服务器可能返回 410。
- 关注媒体走 `/follow-stream?id=&media=&quality=`，可选清晰度读取 `/follow-qualities?id=`。图片走服务图片代理；不上报上游私有 URL。
- 本地字幕列表 `/api/assets/{id}/subtitles`；在线搜索 `/api/subtitles/search?q=`，下载选择 `/api/subtitles/subtitlecat/{id}`。手机字幕经过 `/api/subtitles/convert`，保存为应用私有临时 VTT。
- 时间轴读取 `/api/timeline?id=`，按服务器 sprite 元数据定位；媒体支持范围和解码仍由服务器及设备决定。
- 开始实际播放一次 POST `/api/play` 或 `/api/follow/play`。观看累计每 10 秒及暂停、结束、关闭时 POST `/api/activity` 或 `/api/follow/activity`，携带位置、时长、观看增量、跳转次数与结束状态。增量沿用原网页的播放进度口径。
- 本地续播键包括服务器、类型、作品、版本；没有本机记录时读取 `leave_ratio`。不承诺精确跨设备同步。
- 外部播放先申请 `/api/external-playback` 临时链接，再交给 Android 播放器，不把 Cookie 或访问口令交给外部应用。

## 导入与平台限制

历史文件上传 `/api/taste/import` 为原始文件正文，并携带百分号编码的 `X-Peach-Filename`。原生系统选择器读取显示文件名，不把 content URI 当成服务器路径。当前客户端内存保护限制历史文件 64 MB、字幕 8 MB、CA 1 MB。

配置服务端文件目录、Mac 自启、安装更新和卸载等保留在电脑端。原服务不允许远程操作时原样展示权限原因，不绕过服务约束。外部来源的限流、凭据与离线恢复需要对应账号和测试来源，未以空列表代替验收。
