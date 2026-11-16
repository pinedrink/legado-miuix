# 构建说明

本仓库是 [Legado](https://github.com/LegadoTeam/legado) 的修改版（GPLv3），构建方式与上游基本一致。

## 环境要求

| 项目 | 要求 |
| --- | --- |
| JDK | 17 或 21（本项目在 JDK 21 上验证） |
| Android SDK | Platform 36、Build-Tools 36.0.0 |
| Gradle | 使用仓库自带 `gradlew`（无需单独安装） |
| 内存 | 建议 ≥ 8 GB（`gradle.properties` 中 `-Xmx6g`） |
| 网络 | 首次构建需要下载 Gradle 发行版与 Maven 依赖 |

## 配置 SDK 路径

在仓库根目录创建 `local.properties`（已被 `.gitignore` 忽略）：

```properties
sdk.dir=/path/to/Android/Sdk
```

Windows 示例：`sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk`。
也可以改用环境变量 `ANDROID_HOME`。

## 常用命令

```bash
# Debug APK（自动用 debug keystore 签名）
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/app/debug/app-app-debug.apk（包名 com.legado.app）

# Release APK（未配置签名时为 unsigned）
./gradlew :app:assembleAppRelease
# 产物：app/build/outputs/apk/app/release/app-app-release-unsigned.apk（包名 com.legado.app.release）
```

## 给 Release 包签名

项目只有在传入 `RELEASE_STORE_FILE` 等属性时才使用正式签名：

```bash
./gradlew :app:assembleAppRelease \
  -PRELEASE_STORE_FILE=/path/app.jks \
  -PRELEASE_STORE_PASSWORD=*** \
  -PRELEASE_KEY_ALIAS=*** \
  -PRELEASE_KEY_PASSWORD=***
```

没有正式 keystore 时可用调试证书签名（能安装，但不同机器生成的调试证书不同，**无法覆盖升级**，用户需要卸载重装）：

```bash
BT=$ANDROID_HOME/build-tools/36.0.0
$BT/zipalign -p -f 4 app/build/outputs/apk/app/release/app-app-release-unsigned.apk app-aligned.apk
$BT/apksigner sign --ks ~/.android/debug.keystore \
  --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey \
  --out app-signed.apk app-aligned.apk
```

## 说明与注意事项

- `versionCode` 由 `git rev-list 9639f02..HEAD --count` 计算，因此**不要用浅克隆**（`--depth=1`）构建正式包。
- `third_party/maven` 是本地 Maven 仓库（含改造过的 htmlunit-core-js），已随仓库提交，不需要额外配置。
- `app/cronetlib/*.jar` 已随仓库提交；`app/download.gradle` 中的下载任务在文件已存在时不会重复下载。
- 构建产物默认输出在 `app/build/`，该目录已被忽略。

## 常见错误

| 报错 | 处理 |
| --- | --- |
| `SDK location not found` | 缺少 `local.properties` 或 `ANDROID_HOME` |
| `Unsupported class file major version` | JDK 版本过低，换 17/21 |
| `No such property: RELEASE_STORE_FILE` 相关 | 未传签名属性时属正常，release 产物为 unsigned |
| Gradle 下载超时 | 检查网络/代理；不要随意改仓库地址 |
