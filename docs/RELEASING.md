# 发布流程

本项目使用「tag 触发 GitHub Actions」发布签名 APK。

## 一次性准备（仓库管理员）

1. 生成 release keystore（**只做一次，务必备份；丢失后无法覆盖升级**）：

   ```bash
   keytool -genkeypair -v -keystore legado-miuix-release.jks \
     -alias legado-miuix -keyalg RSA -keysize 4096 -validity 10000 \
     -storepass '<密码>' -keypass '<密码>' \
     -dname "CN=Legado Miuix, O=pinedrink, C=CN"
   ```

2. 把 keystore 转为 base64，并写入仓库 Secrets（Settings → Secrets and variables → Actions）：

   ```bash
   base64 -w0 legado-miuix-release.jks    # → RELEASE_KEY_STORE_BASE64
   ```

   | Secret | 内容 |
   | --- | --- |
   | `RELEASE_KEY_STORE_BASE64` | keystore 的 base64 |
   | `RELEASE_STORE_PASSWORD` | keystore 密码 |
   | `RELEASE_KEY_ALIAS` | 别名，如 `legado-miuix` |
   | `RELEASE_KEY_PASSWORD` | 私钥密码（通常同上） |

   未配置 `RELEASE_KEY_STORE_BASE64` 时，workflow 仍会构建，但产出的是 **unsigned** APK。

## 发布一个版本

```bash
git tag v0.1.0-miuix
git push origin v0.1.0-miuix
```

workflow `Release (Miuix)` 会：构建 `:app:assembleAppRelease`（签名）→ 复制为 `legado-miuix-<tag>.apk` →
生成 `SHA256SUMS` → 用 `softprops/action-gh-release` 创建 Release（含自动生成的 release notes）。

## 版本号与包名

- `versionName`：`3.` + `yyMMddHH`（见 `app/build.gradle`），与 tag 无强绑定，建议 tag 用语义化前缀。
- `versionCode`：`30000 + git rev-list <base>..HEAD --count`，因此 CI 必须 `fetch-depth: 0`。
- 包名：debug `com.legado.app.debug`、release `com.legado.app.release`，与官方 `com.legado.app` 并存。

## 本地手动发布（CI 不可用时）

```bash
./gradlew :app:assembleAppRelease \
  -PRELEASE_STORE_FILE=$HOME/android-release/legado-miuix-release.jks \
  -PRELEASE_STORE_PASSWORD='<密码>' -PRELEASE_KEY_ALIAS=legado-miuix -PRELEASE_KEY_PASSWORD='<密码>'

gh release create v0.1.0-miuix \
  app/build/outputs/apk/app/release/app-app-release.apk \
  --title "Legado Miuix v0.1.0" --notes-file <说明.md> --prerelease
```
