# Peach 安卓原生客户端

连接现有 Peach 服务的 Kotlin / Jetpack Compose 客户端。最低 Android 8.0（API 26），手机和平板使用同一安装包。服务端保持不变，源码契约基线为 `de22796c04919aaa057d77930c33c81313765569`。

**当前为开发测试版本，尚未完成原系统全部功能的逐项验收。** 馆藏、详情、搜索、收藏、观看上报及原生播放器已经接入；高级维护功能通过原生结构化列表和操作表单接入，仍需要逐项验证实际业务流程。接口清点与构建成功不代表完整迁移验收通过。详情见 [验收记录](docs/VERIFICATION.md)。

## 安装与连接

1. 安装 `dist/Peach-Android-0.1.3-debug.apk`，允许当前文件管理器安装应用。已有测试版可覆盖安装。
2. Mac 上正常运行 Peach。手机需能访问该服务，填写 `http://电脑局域网地址:端口`，不能填写手机自己的 `localhost`。
3. 填写 Peach 访问口令。支持保存多个服务器、切换、重新登录和删除本机连接记录。
4. 私有 HTTPS 服务可导入 PEM CA 证书。证书及主机名校验保持开启；HTTP 在界面显示未加密提示。

馆藏卡片点按查看详情，长按打开操作菜单。向下浏览自动加载下一页，标题显示已加载数量与总数；加载失败保留已有条目，可在列表底部重试。左上角导航菜单按网页顺序提供首页、关注、JAV、艺人、标签、厂牌、已标记、管理，并保留播放列表和沉浸模式。JAV 内提供馆藏、发现、想要；艺人、标签和厂牌卡片可进入对应作品及资料。顶栏“全部功能”打开当前领域的功能列表；各页提供筛选、刷新和服务器操作。服务器路径始终指电脑上的路径，手机文件通过系统文件选择器导入。

播放器支持单击控制栏、双击暂停、长按 350 毫秒临时加速（默认 2 倍）、横向拖动定位、左侧亮度、右侧音量、手势锁定、字幕、版本和分卷、全屏、小窗及系统画中画。松开、取消或切后台恢复原倍率。实际格式和 HDR 能力取决于手机解码器与服务器转码工具。

## 构建与 CI/CD

安装 JDK 17 或 21、Android SDK Platform 36 / Build Tools，并配置 `ANDROID_HOME` 或本机 `local.properties` 的 `sdk.dir`。不要提交 `local.properties`。

```sh
./gradlew :app:assembleRelease :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
python3 tools/package_apk.py
```

打包完成后，APK 产物位于 `dist/` 目录：
- `Peach-Android-<version>.apk`：生产 Release 版本（已签名，体积更小，运行性能最优）。
- `Peach-Android-<version>-debug.apk`：调试 Debug 版本。

### 签名配置说明
- **默认回退机制**：若未配置独立的 Release 签名密钥，Gradle 会自动采用内置有效证书进行签名，编译出的 Release APK 保证可直接在 Android 手机上安装与使用。
- **生产发布签名（可选）**：如需使用正式私钥签名，可在 GitHub 仓库设置以下 Secrets（或配置同名环境变量）：
  - `KEYSTORE_BASE64`：`.keystore` / `.jks` 文件的 Base64 编码字符串
  - `KEYSTORE_PASSWORD`：密钥库密码
  - `KEY_ALIAS`：密钥别名
  - `KEY_PASSWORD`：私钥密码

项目已配置 GitHub Actions 自动化工作流（`.github/workflows/build-apk.yml`）：
- **分支推送与 PR**：推送到 `main` 分支时自动执行单元测试与 Lint 静态检查，编译打包 Release & Debug APK 并上传为 GitHub Actions 构建产物（Artifact）。
- **版本发布**：推送以 `v` 开头的版本标签（如 `git tag v0.1.3 && git push origin v0.1.3`）时，会自动创建 GitHub Release 并附带最新的 Release / Debug 安装包及 `SHA256SUMS` 校验和文件。
- **手动打包**：支持在 GitHub 仓库 Actions 页面一键点击 `workflow_dispatch` 手动触发打包与按需发布。

## 联调与测试

联调脚本只允许带标记的临时合成环境，不对真实馆藏执行删除、整理或凭据修改测试。

```sh
python3 tools/prepare_fixture.py "$FIXTURE_ROOT" --peach "$PEACH_EXECUTABLE" --ffmpeg "$FFMPEG_EXECUTABLE"
# 另一个终端使用 PEACH_DATA_ROOT=$FIXTURE_ROOT/data 在回环端口 18901 启动服务。
python3 tools/contract_smoke.py "$FIXTURE_ROOT"
adb reverse tcp:18901 tcp:18901
./gradlew :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e peachFixture true -e peachBase http://127.0.0.1:18901 video.peach.android.test/androidx.test.runner.AndroidJUnitRunner
```

协议提取脚本需要能够解析服务源码的 Python 3.14：`python3.14 tools/extract_protocol.py --help`。动态路由共 64 GET / 60 POST，另外登记认证、媒体、字幕等专用入口。协议见 [PROTOCOL.md](docs/PROTOCOL.md)，逐项清点见 [FEATURE_MATRIX.md](docs/FEATURE_MATRIX.md)，业务用例和剩余工作见 [ACCEPTANCE.md](docs/ACCEPTANCE.md)。

## 项目结构

- `data/`：协议目录、认证、Keystore、DataStore、Room 与每服务器网络上下文。
- `ui/`：主题、连接、馆藏、详情、图片查看、裁剪与结构化操作表单。
- `player/`：Media3 会话、观看计时、字幕、时间轴及单一手势状态机。
- `tools/`：只读协议提取、隔离合成夹具和验收脚本。

来源：[peach-mitao/peach](https://github.com/peach-mitao/peach)。沿用其品牌资源与协议，遵守仓库 AGPL-3.0 许可证。第三方依赖按各自许可证使用（AndroidX Apache-2.0、OkHttp/Retrofit Apache-2.0、Coil Apache-2.0、Kotlin Apache-2.0）。无埋点服务、无内置真实账号凭据。
