# ANDROID_PLATFORM_API_GUIDELINES.md
# Legado × Miuix：Android 平台 API 优先与兼容性规范

> 本文是 Legado Miuix 迁移项目的补充开发规范，与仓库根目录 `AGENTS.md` 配合使用。
>
> 适用对象：Codex、Claude Code、DeepSeek Harness、Cursor、GitHub Copilot 等 AI 编程助手及项目开发者。
>
> 核心原则：**Miuix 负责 UI 视觉与组件体验；Android Framework / AndroidX 负责系统能力、平台行为和兼容性。**
>
> 本文不是要求所有页面都改成 Android 原生 View，也不是禁止使用 Compose。它要求 UI 框架不能取代 Android 平台本身已经提供的能力。

---

## 1. 总体目标

Legado 正在从传统 Android View/XML 逐步引入 Jetpack Compose 和 Miuix。

迁移过程中必须同时满足三个目标：

1. **视觉一致性**：使用 Miuix 建立统一的 HyperOS 风格视觉体验。
2. **平台一致性**：系统能力优先使用 Android 官方公开 API 和 AndroidX 兼容 API。
3. **设备兼容性**：应用不能依赖小米手机、HyperOS、MIUI 私有服务才能正常使用。

### 1.1 UI 与平台职责划分

```text
                    Legado Application
                            |
              +-------------+-------------+
              |                           |
          UI / Presentation           Platform / System
              |                           |
       Compose + Miuix              Android Framework
              |                     AndroidX Libraries
       Legado UI Components         Activity Result API
              |                     WindowInsets
       Theme / Layout / State       Storage Access Framework
              |                     Permission APIs
              |                     Notification APIs
              |                     WorkManager
              |                     BiometricPrompt
              |                     Android Keystore
              |                           |
              +-------------+-------------+
                            |
                    Existing Business Layer
                            |
              ViewModel / Repository / Help
                            |
                 Database / Network / Files
```

### 1.2 必须遵守的原则

- Miuix 不负责替代 Android 的系统权限机制。
- Miuix 不负责替代 Android 的文件选择器。
- Miuix 不负责替代 Android 的系统分享面板。
- Miuix 不负责替代 Android 的通知管理机制。
- Miuix 不负责替代 Android 的后台任务调度机制。
- Miuix 不负责替代 Android 的系统返回、窗口和 Insets 机制。
- Miuix 不负责替代 Android 的安全存储、系统凭据和生物识别能力。

可以使用 Miuix 对应用内部的 UI 进行视觉统一，但涉及系统能力时必须优先尊重 Android 平台规范。

---

## 2. API 选择优先级

实现新功能或迁移旧功能时，按照以下优先级选择技术：

| 优先级 | 技术来源 | 使用原则 |
|---|---|---|
| 1 | Android Framework 公共 API | 系统已有明确能力时优先使用 |
| 2 | AndroidX / Jetpack | 需要兼容不同 Android 版本时优先使用 |
| 3 | 项目现有封装 | 已有成熟实现时优先复用 |
| 4 | Miuix | 用于应用内部 UI、交互和视觉组件 |
| 5 | 自定义实现 | 仅在平台 API 和现有组件无法满足需求时使用 |

注意：

- AndroidX 本身也是 Android 官方维护的兼容性库，不应为了“原生”而拒绝使用。
- 不要为了使用某个新 API，忽略项目的最低支持版本。
- 不要因为 Miuix 提供了某个视觉组件，就把底层系统能力也交给它处理。
- 不要重复封装项目已有的功能。
- 不得使用 Android Hidden API、非公开系统接口或未经授权的反射访问来实现普通 UI 功能。

---

## 3. Android 版本兼容性

### 3.1 当前项目兼容性基线

根据当前项目迁移交接信息，Legado 需要保留 Android API 23 的最低版本兼容性。

执行任务时必须再次检查：

- `app/build.gradle` 或 `app/build.gradle.kts`
- `minSdk`
- `compileSdk`
- `targetSdk`
- Gradle Version Catalog
- 当前 Miuix 依赖的 Android 要求

不能假设本文编写时的版本配置永远不变。

### 3.2 版本适配原则

| Android 版本 | API Level | 需要特别关注 |
|---|---:|---|
| Android 6.0 | 23 | 运行时权限机制起点，最低兼容性 |
| Android 8.0 | 26 | Notification Channel、后台执行限制 |
| Android 10 | 29 | Scoped Storage 变化 |
| Android 11 | 30 | 存储访问限制、软件包可见性 |
| Android 12 | 31 | 通知与系统行为变化、动态颜色能力 |
| Android 13 | 33 | 通知运行时权限、照片/视频/音频细分权限 |
| Android 14 | 34 | 部分照片访问、前台服务类型等变化 |
| Android 15 | 35 | Edge-to-Edge 行为变化 |
| Android 16 | 36 | 新系统行为及目标 SDK 适配 |

以上是需要重点检查的版本节点，不代表每个功能都必须在所有版本上使用同一套实现。

### 3.3 API 使用规则

1. 使用新 API 前检查 `Build.VERSION.SDK_INT` 或使用 AndroidX 兼容封装。
2. 低版本必须有合理的降级路径。
3. 不允许在低版本上调用不存在的 API。
4. 不允许为了某个 UI 效果提高整个应用的 `minSdk`。
5. 不允许只在最新小米手机上验证功能。
6. 关键系统能力必须在至少一个非小米 Android 环境中验证。
7. 对无法兼容的功能，明确标记受限版本和替代方案。

---

## 4. 系统能力必须优先使用 Android API

以下规则是本规范的重点。

### 4.1 文件选择与目录访问

**优先使用 Storage Access Framework（SAF）。**

推荐 API：

- `ActivityResultContracts.OpenDocument`
- `ActivityResultContracts.OpenMultipleDocuments`
- `ActivityResultContracts.CreateDocument`
- `ActivityResultContracts.OpenDocumentTree`
- `ContentResolver`
- `DocumentFile`

适用场景：

- 导入本地 TXT、EPUB 等书籍。
- 选择备份文件。
- 导出数据。
- 选择书籍目录。
- 选择用户自定义文件。

必须遵守：

1. 优先让 Android 系统提供文件选择界面。
2. 使用 `content://` URI，不要假设所有文件都能转换成普通文件路径。
3. 需要长期访问时，按 SAF 规则申请并保存持久化 URI 权限。
4. 读取和写入通过 `ContentResolver` 完成。
5. 对用户主动选择的目录，正确处理 URI 权限和访问失败。
6. 不要为了获得文件路径而滥用 `MANAGE_EXTERNAL_STORAGE`。
7. 不要把 `MediaStore` 当作任意文件和目录访问的通用替代品。

如果项目现有文件管理器需要展示应用内部目录，可以保留应用内部文件浏览 UI；但涉及用户授权访问外部文件时，优先使用 SAF。

### 4.2 图片和媒体选择

优先考虑 Android Photo Picker 和 AndroidX 对应兼容 API。

推荐：

- `ActivityResultContracts.PickVisualMedia`
- `ActivityResultContracts.PickMultipleVisualMedia`
- `MediaStore`
- `ContentResolver`

规则：

- 用户只是选择照片或视频时，不要默认申请整个媒体库的读取权限。
- 需要选择少量媒体时优先使用系统 Photo Picker。
- 需要访问整个媒体集合时，再根据系统版本和业务必要性申请相应权限。
- 对 Android 13 及以上的媒体权限进行细分处理。
- 对 Android 14 及以上的部分照片访问行为进行兼容。
- 低版本使用 AndroidX 提供的兼容方式或明确的替代流程。

### 4.3 运行时权限

权限申请必须使用 Android 官方权限机制或 AndroidX Activity Result API。

推荐：

- `ActivityResultContracts.RequestPermission`
- `ActivityResultContracts.RequestMultiplePermissions`
- `ContextCompat.checkSelfPermission`
- `ActivityCompat.shouldShowRequestPermissionRationale`

规则：

1. 只在真正需要某项能力时申请权限。
2. 不要在应用启动时一次性申请所有权限。
3. 先解释权限用途，再在合适的用户操作节点申请。
4. 用户拒绝后，不能无限重复弹出权限请求。
5. 处理拒绝、永久拒绝、系统不再提供授权入口等情况。
6. 权限申请失败时提供合理的功能降级。
7. 不允许通过隐藏 API 或 shell 命令绕过权限机制。

Compose 页面可以使用自己的说明 UI，但最终授权必须交给 Android 权限 API。

### 4.4 通知

优先使用 Android Notification API。

需要关注：

- `NotificationManager`
- `NotificationChannel`
- `NotificationCompat`
- `NotificationManagerCompat`
- `POST_NOTIFICATIONS`
- `PendingIntent`

规则：

- Android 8.0 及以上必须正确创建通知渠道。
- Android 13 及以上正确处理通知运行时权限。
- 不要通过应用内自定义弹窗假装实现系统通知。
- 通知渠道名称、描述和重要程度应合理设置。
- 通知点击应使用正确的 PendingIntent。
- PendingIntent 的 mutability 必须根据实际用途明确指定。
- 不要为了让通知显示而绕过用户权限选择。

### 4.5 后台任务和长时间运行任务

优先根据任务性质选择 Android 官方机制。

| 任务类型 | 优先方案 |
|---|---|
| 可延迟、可重试的持久任务 | WorkManager |
| 由用户明确发起且需要持续展示的长任务 | 符合系统要求的 Foreground Service |
| 短时间异步操作 | Kotlin Coroutines |
| 精确时间触发 | 根据实际业务评估 AlarmManager |
| 应用内生命周期任务 | Lifecycle / ViewModel Scope |

规则：

1. 不要使用永久运行的后台线程实现周期性任务。
2. 不要在 Activity 销毁后继续持有 Activity。
3. 不要把所有任务都塞进 Foreground Service。
4. Foreground Service 必须符合当前 Android 版本的启动限制和服务类型要求。
5. 需要持久执行、系统重试或约束条件时优先评估 WorkManager。
6. 不要假设应用退到后台后，普通线程就能一直运行。
7. 继续复用 Legado 现有任务调度和下载体系，迁移 UI 时不要顺手重写后台业务。

### 4.6 系统分享

优先使用 Android Sharesheet。

推荐：

- `Intent.ACTION_SEND`
- `Intent.ACTION_SEND_MULTIPLE`
- `Intent.createChooser`
- `ClipData`
- `FileProvider`

规则：

- 分享文件时使用 `content://` URI。
- 正确授予临时 URI 读取权限。
- 不要把私有文件路径直接暴露给其他应用。
- 不要自行实现一套应用列表来替代系统分享面板，除非产品确实需要应用内特定功能。

### 4.7 系统返回和预测性返回

优先遵循 Android 返回导航机制。

推荐：

- `OnBackPressedDispatcher`
- AndroidX Activity Back APIs
- Android 平台预测性返回支持

规则：

1. 不要在每个 Compose 页面各自实现一套互相冲突的返回机制。
2. 不要随意覆盖系统返回行为。
3. 处理 Dialog、BottomSheet、输入法和页面导航之间的返回优先级。
4. 对预测性返回保持兼容。
5. 继续尊重 Legado 现有返回逻辑，包括阅读器退出、书架返回和双击退出等业务行为。
6. 如果需要自定义返回行为，必须明确其适用页面和优先级。

### 4.8 WindowInsets、状态栏和导航栏

优先使用 Android WindowInsets 和 AndroidX 兼容 API。

推荐：

- `WindowInsets`
- `WindowInsetsCompat`
- `WindowCompat`
- `ViewCompat.setOnApplyWindowInsetsListener`
- Compose `WindowInsets`
- 当前 AndroidX Activity 支持的 Edge-to-Edge API

规则：

1. 不要通过固定高度模拟状态栏和导航栏。
2. 不要重复消费同一组 Insets。
3. 不要在 Compose 和 View 两侧重复添加相同的系统栏 padding。
4. 正确处理 IME Insets。
5. 正确处理手势导航和三键导航。
6. Android 15 及以上的 Edge-to-Edge 行为必须实际验证。
7. 不要假设所有设备的状态栏、导航栏高度相同。
8. 兼容横屏、多窗口、分屏和自由窗口。

Legado 已经存在系统栏处理逻辑，迁移时应优先复用并明确新旧逻辑的职责，不能同时由多个位置控制 Window。

### 4.9 剪贴板

优先使用：

- `ClipboardManager`
- `ClipData`
- `Context.getSystemService`

规则：

- 复制和粘贴使用系统剪贴板。
- 不要创建与系统剪贴板相互独立的隐式存储。
- 对敏感内容复制行为进行谨慎处理。
- 对 Android 新版本的剪贴板提示和隐私行为保持兼容。

### 4.10 生物识别和安全凭据

如果未来涉及应用锁、敏感数据解锁或密钥保护，优先评估：

- AndroidX `BiometricPrompt`
- Android Keystore
- Android 官方凭据管理 API

规则：

- 不要自行实现指纹识别。
- 不要读取或保存用户的生物特征数据。
- 不要把密码或密钥明文写入普通偏好设置。
- 不要把 Miuix 的密码输入框当作安全存储。
- 安全能力必须由 Android 平台或经过验证的安全组件实现。

本项目当前没有必要为了 UI 迁移引入上述安全功能。

### 4.11 WebView

Legado 的 WebView 相关能力属于重要业务功能。

规则：

- 继续使用 Android WebView 和项目现有封装。
- 不要因为 Compose 化而重新实现浏览器内核。
- Compose 可以通过 AndroidView 承载现有 WebView。
- 正确管理 WebView 生命周期，避免 Activity/Context 泄漏。
- 不要为了统一 Miuix 视觉而破坏网页原有交互。
- JS Bridge、文件访问、Cookie、下载和安全策略必须保留原有行为并单独回归测试。

### 4.12 输入法和文本输入

优先使用 Android IME 与 Compose 官方输入机制。

规则：

- 正确处理键盘弹出、收起和焦点变化。
- 使用 Insets 处理键盘占位。
- 不要写死键盘高度。
- 不要为了自定义输入框样式破坏复制、粘贴、选择、自动填充和输入法联想。
- Material TextInputLayout 与 Compose TextField 混用时，必须明确主题和输入行为，不要只改背景就认为迁移完成。

---

## 5. Miuix 的正确使用边界

### 5.1 推荐使用 Miuix 的场景

- 应用内部页面布局。
- TopAppBar、NavigationBar 等应用内导航 UI。
- 设置项、卡片、列表行。
- 按钮、开关、选择器。
- 应用内 Dialog 和 BottomSheet。
- 应用内主题和视觉反馈。
- 应用内搜索框和标签。

### 5.2 不应由 Miuix 替代的场景

- 系统文件选择器。
- 系统权限授权界面。
- 系统通知和通知渠道。
- 系统分享面板。
- 系统生物识别界面。
- 系统应用设置页面。
- Android 后台任务调度。
- 系统媒体访问授权。
- 系统返回和窗口管理。

### 5.3 不要强行模仿小米系统私有体验

- 不要求非小米设备必须呈现小米系统专属弹窗。
- 不依赖 MIUI/HyperOS 私有广播、ContentProvider 或隐藏服务。
- 不依赖小米系统应用包名来完成基础功能。
- 不通过反射访问小米系统内部实现。
- 不把 HyperOS 私有 API 当作 Android 标准 API。
- 对非小米设备使用 Android 官方标准行为。

目标是“采用 Miuix 设计语言的 Android 应用”，不是“在所有手机上模拟一套小米系统”。

---

## 6. Legado 项目中的特别约束

### 6.1 现有业务优先复用

UI 迁移时必须优先复用：

- 原有 ViewModel。
- 原有 Repository / DAO。
- 原有 Help 类。
- 原有网络请求和缓存实现。
- 原有文件导入导出逻辑。
- 原有下载与自动任务调度。
- 原有书源解析和 JavaScript 执行机制。

不要为了 Compose 页面方便，复制出一套新的数据访问或业务逻辑。

### 6.2 现有文件管理能力

Legado 有自己的文件管理、导入和导出功能。

必须区分：

1. 应用私有目录访问。
2. 用户通过 SAF 授权的外部目录访问。
3. MediaStore 管理的共享媒体。
4. 普通 `content://` URI。
5. 旧版本 Android 的文件访问兼容行为。

不能把所有 URI 都当作文件路径，也不能把所有文件路径都当作 SAF URI。

### 6.3 现有主题系统

Miuix Theme 只作为 Compose 的主题呈现入口。

继续保留 Legado 的：

- ThemeConfig。
- ThemeStore。
- 用户自定义主题。
- E-Ink 模式。
- 背景图和壁纸主题。
- 阅读器独立主题。
- 状态栏和导航栏适配。

不要把系统动态颜色作为唯一颜色来源。

### 6.4 现有导航系统

第一阶段继续保留：

- Activity。
- Fragment。
- FragmentManager。
- ViewPager。
- 现有页面跳转逻辑。
- 现有返回处理。

Compose 页面应通过明确的接口与现有导航系统交互。

不要在每个 Compose 页面中重复创建独立导航栈。

---

## 7. 禁止的实现方式

未经用户明确同意，不得：

1. 为了模拟系统文件选择器而自定义完整文件授权流程。
2. 为了避免权限弹窗而使用非公开接口绕过权限。
3. 通过固定状态栏、导航栏、键盘高度实现 Insets。
4. 通过无限循环线程实现后台任务。
5. 通过永久 Service 保持应用存活。
6. 将所有功能都改成 Compose 自定义实现，即使 Android 已经提供成熟 API。
7. 将小米私有 API 当作所有 Android 设备通用 API。
8. 使用 Hidden API 或不稳定反射实现普通功能。
9. 未检查 URI 授权就把外部文件路径保存到数据库。
10. 在 API 23 设备上直接调用高版本 API。
11. 为了某个视觉效果随意提高 minSdk。
12. 在 Compose 中直接执行数据库、网络或文件 I/O。
13. 在 UI 迁移任务中重写现有下载、备份、导入、导出和调度逻辑。

---

## 8. 每次新增功能的 API 设计流程

AI 助手在实现功能前，必须先回答以下问题：

1. Android 是否已经提供对应的公开 API？
2. AndroidX 是否提供跨版本兼容封装？
3. Legado 是否已有成熟实现？
4. 这个功能属于系统能力还是应用内部 UI？
5. Miuix 是否只负责视觉层？
6. 最低 API 23 是否可以正常使用？
7. 高版本 API 是否需要运行时判断？
8. 是否需要用户授权？
9. 是否有低版本降级方案？
10. 是否会影响现有业务行为？

如果 Android 已经提供成熟方案，优先使用它，不要为了“统一代码风格”重新造轮子。

---

## 9. 代码审查要求

每次修改涉及 Android 系统能力时，审查报告必须包含：

| 检查项 | 必须回答的问题 |
|---|---|
| API 来源 | 使用了哪个 Android Framework / AndroidX API？ |
| 最低版本 | API 23 是否支持？ |
| 版本判断 | 是否正确处理高版本 API？ |
| 权限 | 是否需要运行时权限或 URI 授权？ |
| 生命周期 | 是否会泄漏 Activity、Context 或资源？ |
| 系统行为 | 是否尊重 Android 原生交互？ |
| 兼容性 | 非小米设备能否使用？ |
| 回归 | 是否影响 Legado 现有功能？ |

发现自定义实现时，必须说明为什么 Android 官方 API 不适用。

不得仅以“这样更方便”“这样更像 HyperOS”作为绕过平台 API 的理由。

---

## 10. 测试要求

### 10.1 设备覆盖

至少考虑：

- API 23：最低版本兼容。
- API 29/30：存储访问行为。
- API 31/32：系统主题和窗口行为。
- API 33：通知和媒体权限。
- API 34：部分照片访问和前台服务限制。
- API 35/36：Edge-to-Edge 和新系统行为。

### 10.2 厂商覆盖

至少考虑：

- 小米 HyperOS 设备。
- AOSP 或接近原生 Android 的设备/模拟器。
- 一个非小米厂商 Android 设备。

### 10.3 系统功能测试

- [ ] 文件选择器可以正常选择文件。
- [ ] SAF 授权目录可正常访问。
- [ ] URI 持久化授权行为正确。
- [ ] 图片/媒体选择不滥用全量权限。
- [ ] 权限拒绝后页面有合理降级。
- [ ] 通知渠道正常。
- [ ] 通知权限拒绝后应用仍可使用。
- [ ] 系统分享可以正确分享文本和文件。
- [ ] 返回键、预测性返回行为正常。
- [ ] 软键盘和 Insets 正常。
- [ ] 横屏和多窗口行为正常。
- [ ] 后台任务符合系统限制。
- [ ] 非小米设备没有调用小米私有 API。

与本次改动无关的项目可以标记“未涉及”，不能标记“已验证”。

---

## 11. AI 助手执行约束

每次开始任务前：

1. 读取仓库根目录 `AGENTS.md`。
2. 读取本文 `ANDROID_PLATFORM_API_GUIDELINES.md`。
3. 读取 `MIUIX_MIGRATION.md` 或最新迁移交接文档（如果存在）。
4. 阅读目标页面和相关系统能力实现。
5. 检查当前依赖版本和最低 API。

每次任务完成后：

1. 说明使用了哪些 Android API。
2. 说明使用了哪些 Miuix 组件。
3. 说明是否存在自定义替代实现。
4. 说明最低 API 兼容情况。
5. 说明实际执行的构建和测试。
6. 明确列出未验证的系统行为。

**不允许只以视觉效果判断功能完成。**

---

## 12. 最终原则

Legado 是一个 Android 应用，不是一个运行在 Android 上的 HyperOS 模拟器。

Miuix 的价值在于提供更好的 UI 设计语言和组件体验；Android 平台 API 的价值在于提供成熟、稳定、经过系统验证的设备能力。

因此：

- 应用内部 UI：优先 Miuix。
- 系统能力：优先 Android Framework / AndroidX。
- 业务逻辑：优先复用 Legado 现有实现。
- 跨版本兼容：优先使用 AndroidX 和明确的降级策略。
- 无法确定时：先调查 API 和现有实现，不要猜测或自创系统行为。

**最终目标：做出一个视觉现代、符合 Android 平台规范、兼容不同厂商设备，并且不破坏 Legado 原有能力的应用。**
