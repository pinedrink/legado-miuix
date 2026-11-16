# AGENTS.md — Legado Miuix UI Migration Guide

> 本文件是 AI 编程助手在 Legado 项目中执行 Miuix UI 迁移时必须遵守的项目级约束。
>
> 适用对象：Codex、Claude Code、Cursor、GitHub Copilot 等能够读取仓库指令文件的编程助手。
>
> 更新时间：2026-11-15（验证策略改为「分级 + 批量 + 抽样」，见 §9 / §10）  
> 目标仓库：[LegadoTeam/legado](https://github.com/LegadoTeam/legado)  
> 目标 UI 库：[compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)

---

## 0. 首要约定：本项目所说的 Miuix 是什么

本迁移文档中的 **Miuix** 特指：

- GitHub：<https://github.com/compose-miuix-ui/miuix>
- 官方文档：<https://compose-miuix-ui.github.io/miuix/>
- Maven Group：`top.yukonga.miuix.kmp`
- Android 核心依赖：`miuix-ui-android`
- 技术基础：Compose Multiplatform / Jetpack Compose
- 设计方向：Xiaomi HyperOS 风格的 Android UI 组件

**不要将它与小米早期的传统 Android View 版 MIUIX 混淆。**

如果用户后续明确表示目标是另一套 `miuix` Android View 库，必须先停止依赖和 API 设计，重新核对目标仓库、坐标和文档，不能沿用本文中的 Compose API。

Miuix 当前处于 Experimental 阶段。使用前必须阅读其当前版本的官方文档、Release Notes 和示例代码，不得假设旧版本 API 仍然有效。

官方参考：

- <https://github.com/compose-miuix-ui/miuix>
- <https://compose-miuix-ui.github.io/miuix/zh_CN/guide/getting-started>
- <https://compose-miuix-ui.github.io/miuix/zh_CN/>

---

## 1. 项目现状与技术基线

以下信息根据仓库当前代码结构整理。实际执行任务时，仍需以当前 checkout 的代码为准，不要把本文当成永远不变的版本清单。

### 1.1 工程结构

主要模块：

```text
legado/
├── app/                    # Android 应用主体
│   └── src/main/
│       ├── java/io/legado/app/
│       │   ├── base/       # Activity、Fragment 等基类
│       │   ├── data/       # 数据层
│       │   ├── help/       # 业务辅助、配置、主题等
│       │   ├── model/      # 业务模型
│       │   ├── service/    # Android Service
│       │   ├── ui/         # 页面和 UI 组件
│       │   ├── utils/      # 工具类
│       │   └── web/        # Web 服务相关
│       └── res/
│           ├── layout/     # XML 布局
│           ├── values/     # 主题、颜色、尺寸、字符串
│           └── ...
├── modules/
│   ├── book/               # 图书相关模块
│   └── rhino/              # JavaScript/Rhino 相关模块
├── gradle/
│   └── libs.versions.toml  # Version Catalog
├── settings.gradle
└── build.gradle
```

### 1.2 当前 UI 架构

当前项目主要采用传统 Android View 体系：

- `AppCompatActivity`
- AndroidX `Fragment`
- XML Layout
- ViewBinding
- Material Components
- RecyclerView、ViewPager、BottomNavigationView
- 项目自定义 View、Dialog、PopupWindow 等

重要基类和入口：

- `app/src/main/java/io/legado/app/base/BaseActivity.kt`
- `app/src/main/java/io/legado/app/base/VMBaseActivity.kt`
- `app/src/main/java/io/legado/app/base/BaseFragment.kt`
- `app/src/main/java/io/legado/app/ui/main/MainActivity.kt`
- `app/src/main/java/io/legado/app/help/config/ThemeConfig.kt`
- `app/src/main/java/io/legado/app/App.kt`

项目 UI 目录说明：

- `app/src/main/java/io/legado/app/ui/README.md`

修改 UI 前必须阅读目标页面对应的 Activity/Fragment、ViewModel、XML 布局和相关自定义组件，不能只看页面类就开始改造。

### 1.3 当前工程配置基线

当前仓库检查到的配置包括：

| 配置 | 当前值 |
|---|---|
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.4.20 |
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 23 |
| Java/JVM Toolchain | 21 |
| ViewBinding | 已启用 |
| Compose | 已启用（`app/build.gradle:111` `compose = true`；Kotlin Compose 插件 `app/build.gradle:11`） |
| Miuix | `0.9.3`（`gradle/libs.versions.toml:89`） |

依赖版本主要维护在 `gradle/libs.versions.toml`。

**版本约束：**

1. 不得为了引入 Miuix 而无理由升级 AGP、Gradle、Kotlin、KSP 或 Android SDK。
2. 不得使用 `+`、`latest.release` 等动态版本。
3. Miuix、Compose Runtime、Compose Compiler、Kotlin 版本必须经过兼容性验证。
4. 版本变更应集中在 Version Catalog 和必要的 Gradle 插件配置中。
5. 如果确实需要升级 Kotlin 或 Compose 相关插件，先说明兼容性原因、影响范围和回退方式，再执行。
6. 依赖解析失败时，先判断仓库配置、网络和版本兼容性，不要随意添加多个 Maven 镜像或修改整个仓库策略。

---

## 2. 迁移目标

### 2.1 总目标

在不破坏 Legado 现有功能和数据行为的前提下，逐步使用 Miuix 重构适合现代 Compose UI 的页面，使应用视觉风格逐步统一到 HyperOS/Miuix 设计语言。

迁移重点：

- 页面布局和组件视觉
- 顶部栏、底部导航、列表和卡片
- 设置项、开关、选择器、滑块
- 对话框、弹出层和底部弹窗
- 颜色、字体、间距、圆角和交互反馈
- 深色模式、动态颜色和系统栏适配
- 无障碍和不同屏幕尺寸适配

### 2.2 不属于本次迁移的内容

除非用户明确提出，否则不要：

- 将整个项目改造成 Compose Multiplatform 工程。
- 将所有 Activity/Fragment 一次性改造成 Compose Navigation。
- 重写数据库、Room Entity、DAO 或数据迁移逻辑。
- 重写书源解析、JavaScript/Rhino 执行、网络请求和缓存逻辑。
- 修改书籍导入导出、备份恢复、WebDAV 同步等业务流程。
- 重写阅读器排版、翻页、手势、分页、文本渲染等核心逻辑。
- 修改音频、视频、WebView、文件选择、二维码扫描等系统能力的实现。
- 为了 UI 迁移改变应用的包名、applicationId、签名、版本号或权限。

**本次首先是 UI 层迁移，不是业务架构重写。**

---

## 3. 总体迁移策略：原生 View 与 Compose 混合共存

### 3.1 默认采用渐进式迁移

当前 Legado 是 ViewBinding/XML 项目。推荐保留现有 Activity、Fragment、ViewModel 和导航结构，通过 `ComposeView` 在旧页面中承载 Miuix UI。

目标结构：

```text
Activity / Fragment（现有）
        │
        ├── 现有 ViewBinding / XML
        │
        └── ComposeView
                │
                └── LegadoMiuixTheme
                        │
                        └── Miuix Composable UI
```

一个页面可以暂时同时包含传统 View 和 Compose。不要为了追求“纯 Compose”而制造大规模无关改动。

### 3.2 ComposeView 生命周期

在 Activity 或 Fragment 中使用 `ComposeView` 时，必须正确设置 Composition 生命周期。

Fragment 中优先采用与 Fragment View 生命周期绑定的策略，例如：

```kotlin
composeView.setViewCompositionStrategy(
    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
)
```

注意：

- 不要让 Compose Composition 的生命周期超过 Fragment View。
- 不要在 Fragment 重建后遗留旧 Composition。
- 不要在页面退出后继续持有 Activity、View 或 Context。
- 对话框、弹窗和临时 Compose 容器也必须检查生命周期释放。
- 具体 API 以项目实际 Compose 版本为准。

### 3.3 导航保持现状

第一阶段继续使用项目现有的 Activity、Fragment、Intent、FragmentManager 和页面跳转方式。

不要因为 Miuix 提供导航相关模块，就直接引入并替换整个应用导航系统。

Miuix 的 `miuix-nav` 等导航模块属于独立技术决策，必须先分析：

- 当前 Activity/Fragment 的任务栈行为
- `singleTask`、`singleTop` 等 launchMode
- 深链和外部 Intent
- 返回键与预测性返回
- 页面状态恢复
- 多窗口行为

没有完成设计和回归测试前，不得迁移导航。

---

## 4. Miuix 依赖接入规则

### 4.1 先做最小化接入验证

不要直接给整个项目添加一批 Miuix 模块。

优先只验证核心 UI 模块：

```kotlin
implementation("top.yukonga.miuix.kmp:miuix-ui-android:<经过验证的版本>")
```

需要设置类组件时，再评估：

```kotlin
implementation("top.yukonga.miuix.kmp:miuix-preference-android:<经过验证的版本>")
```

图标模块按实际需求评估：

```kotlin
implementation("top.yukonga.miuix.kmp:miuix-icons-android:<经过验证的版本>")
```

以上为依赖坐标示例，不代表允许直接复制某个未经验证的版本号。

### 4.2 Compose 构建配置

接入前确认：

- 当前 Kotlin 插件版本。
- 对应版本的 Compose Compiler Gradle Plugin。
- Compose UI/Runtime 依赖版本。
- Miuix 发布版本使用的 Kotlin 和 Compose 版本。
- Android Gradle Plugin 对相关插件的支持情况。
- Miuix Android AAR 的最低 SDK 要求。

如需启用 Compose，按当前 Kotlin 版本使用对应的 Compose Compiler Gradle Plugin，并在 `app/build.gradle` 中按需启用：

```groovy
android {
    buildFeatures {
        viewBinding = true
        compose = true
    }
}
```

保留 ViewBinding。不得为了启用 Compose 删除现有配置。

不要同时采用过时的 `composeOptions.kotlinCompilerExtensionVersion` 配置和不匹配的 Compose Compiler 插件配置。具体方式以当前 Kotlin 官方文档为准。

### 4.3 特别注意最低 SDK

本项目 `minSdk = 23`，必须保持对 Android 6.0 及以上设备的兼容。

Miuix 的模糊效果模块 `miuix-blur` 对 Android 有 API 33 的要求。除非已经实现并验证低版本降级方案，否则不要将它作为基础依赖。

任何新组件都必须检查：

- 依赖自身的 minSdk。
- 传递依赖的 minSdk。
- API 级别限制。
- 低版本设备上的降级行为。

不能因为某个视觉效果在新款小米手机上表现很好，就牺牲 Android 6.0 设备的启动能力。

---

## 5. 主题系统迁移规范

主题是 Legado 迁移中最容易引发全局问题的部分，必须优先设计桥接方案。

### 5.1 现有主题体系必须保留

重点关注：

- `AppConfig`
- `ThemeConfig`
- `ThemeStore`
- `ResourceThemeGeneration`
- `WallpaperTheme`
- `App.kt` 中的系统主题变化处理
- `BaseActivity.initTheme()`
- `BaseActivity.setupSystemBar()`

现有主题能力包括但不限于：

- 跟随系统深色模式
- 手动指定亮色/暗色
- 用户自定义主题颜色
- 壁纸/背景图主题
- 状态栏和导航栏颜色
- 阅读器独立主题
- E-Ink 模式

这些能力不能因为接入 Miuix 而被删除或简化。

### 5.2 建立统一主题桥接层

建议新增统一的 Compose 主题入口，例如：

```text
app/src/main/java/io/legado/app/ui/compose/
├── theme/
│   ├── LegadoMiuixTheme.kt
│   ├── LegadoColorScheme.kt
│   └── LegadoThemeState.kt
└── components/
```

实际目录可以根据当前工程习惯调整，避免无意义地创建过多抽象层。

要求：

1. 所有新 Compose 页面统一通过 `LegadoMiuixTheme` 进入 Miuix Theme。
2. 页面不得自行创建一套互不关联的主题控制器。
3. 主题模式应以 Legado 的用户配置为准，而不是简单地只读取 `isSystemInDarkTheme()`。
4. 用户自定义主色、背景色和深色模式必须能正确映射到 Miuix ColorScheme。
5. 系统主题变化、应用主题变化时，Compose 页面必须正确刷新。
6. 不得破坏原有 XML View 的主题表现。
7. 主题切换后检查 Dialog、Popup、BottomSheet、状态栏和导航栏是否同步。

如果 Miuix 当前 API 无法表达 Legado 某项主题能力，优先使用受控的兼容层，不要偷偷删除原有功能。

### 5.3 阅读器主题独立处理

阅读器的主题、背景、字体、行距、翻页效果等属于核心阅读体验。

不要强制把阅读器的主题状态直接绑定到全局 Miuix Theme。

阅读器需要保留独立的阅读配置与渲染体系。只有在用户明确要求改造阅读器外围 UI 时，才评估迁移其工具栏、菜单等非核心部分。

---

## 6. 页面迁移优先级

按风险分批，不要按目录顺序机械改造。

| 优先级 | 页面/组件 | 风险 | 迁移建议 |
|---|---|---:|---|
| P0 | Miuix 接入验证页 | 低 | 先验证依赖、主题、ComposeView 和生命周期 |
| P1 | 关于页面、简单信息页 | 低 | 适合作为第一个正式迁移页面 |
| P1 | 简单设置项、开关、单选项 | 低/中 | 先做统一设置行组件 |
| P2 | 普通 Dialog、菜单、BottomSheet | 中 | 重点验证键盘、窗口 Insets 和生命周期 |
| P2 | 普通列表页 | 中 | 验证列表性能、点击、长按、刷新和滚动状态 |
| P3 | 书源管理、订阅管理、规则编辑 | 中/高 | 业务交互复杂，逐页迁移 |
| P3 | 主界面、底部导航、书架 | 高 | 依赖现有 Fragment、ViewPager 和自定义底栏逻辑 |
| 暂缓 | 书籍阅读器、漫画阅读器 | 极高 | 保留现有渲染和手势体系 |
| 暂缓 | WebView、音视频、代码编辑器 | 高 | 原生控件优先，按需只迁移外围 UI |

### 6.1 建议的实施顺序

**阶段 A：技术验证**

- 新增 Compose Compiler 插件。
- 引入最小 Miuix 依赖。
- 新建一个测试 Activity 或独立测试页面。
- 验证 MiuixTheme、亮色/暗色、动态颜色。
- 验证 ComposeView 与 ViewBinding 混合。
- 验证 API 23 设备启动和显示。
- 验证无障碍、字体缩放和系统栏。

**阶段 B：建立公共 UI 规范**

- 建立 `LegadoMiuixTheme`。
- 统一页面 Scaffold、TopAppBar、列表行和基础按钮。
- 建立必要的 View/Compose 交互桥接。
- 不要过早创建一个包罗万象的通用 UI 框架。

**阶段 C：低风险页面**

- 关于页面。
- 简单信息展示页面。
- 不涉及复杂业务状态的设置子页面。
- 少量通用 Dialog。

**阶段 D：高频业务页面**

- 设置中心。
- 书源管理。
- 订阅管理。
- 普通列表和编辑页面。

每次只迁移一个页面或一组紧密相关的组件。

**阶段 E：主界面评估**

主界面和书架依赖现有 Fragment、ViewPager、BottomNavigationView、底栏皮肤等行为。迁移前必须先梳理完整状态和交互，不允许只根据静态截图重写。

**阶段 F：阅读器外围评估**

阅读器核心渲染保持不动。若确实需要 Miuix 风格，可以单独评估工具栏、设置面板和弹出菜单，不得影响正文绘制和翻页。

---

## 7. UI 编码规范

### 7.1 Composable 设计

- 使用 Kotlin 编写 Compose UI。
- Composable 尽量保持小而清晰。
- 优先使用无状态 Composable，通过参数传递状态和事件。
- 状态提升（State Hoisting），避免组件内部持有业务状态。
- `Modifier` 作为 Composable 的常规参数，并保持调用顺序合理。
- 只在确实需要时使用 `remember`。
- 不要在 Composable 函数体内直接执行数据库、文件或网络操作。
- 不要在每次重组时创建新的长期任务。
- 使用 `LaunchedEffect` 等 API 时，明确 key 和取消语义。
- 需要从 Flow 订阅状态时，优先使用生命周期感知的收集方式。
- 不要把 Activity、Fragment 或 ViewModel 强引用放入全局单例。

### 7.2 业务逻辑边界

Compose 层只负责展示和 UI 交互。

业务逻辑应继续通过项目现有 ViewModel、Help、Repository、DAO 等路径执行。

禁止：

- 在 Composable 内直接访问 Room DAO。
- 在 Composable 内直接修改数据库 Entity。
- 在 Composable 内直接执行书源脚本。
- 将网络请求写进 UI 组件。
- 为了让 UI 更容易实现而复制一份业务逻辑。
- 绕开原有数据校验、权限检查和错误处理。

如果旧页面没有清晰的 ViewModel 边界，先分析并说明现状，不要顺手大规模重构。

### 7.3 资源与国际化

- 优先复用 `strings.xml` 中的现有文案。
- 新增文案必须考虑项目现有多语言资源。
- 不要在 Compose 中随意硬编码中文。
- 不要删除现有 XML 资源，除非确认没有其他页面引用。
- 注意简体中文、繁体中文、英文及其他现有语言。
- 使用系统字体缩放，不能固定字号导致大字体用户无法使用。

### 7.4 交互和无障碍

必须关注：

- 点击、长按、双击行为。
- 返回键与预测性返回。
- TalkBack 描述。
- 焦点顺序。
- 触控区域大小。
- 键盘弹出和收起。
- 输入框焦点恢复。
- 横屏和多窗口。
- 系统手势区域。
- 列表滚动位置恢复。

不能只保证视觉上相似，还必须保证交互行为一致。

### 7.5 性能

- 长列表避免不必要的重组。
- 使用稳定 Key。
- 不要在列表每个 Item 中重复创建昂贵对象。
- 图片继续复用项目现有图片加载能力，除非有明确理由更换。
- 避免在主线程执行文件、网络和大规模计算。
- 检查 Compose 与 RecyclerView 混用时的滚动冲突。
- 不得为了动画效果牺牲低端设备的流畅度。

---

## 8. 系统栏、Insets 与窗口行为

Legado 已经有自己的系统栏、沉浸式、导航栏、键盘和多窗口处理逻辑。

迁移时必须特别检查：

- `BaseActivity.setupSystemBar()`
- `BaseActivity.upNavigationBarColor()`
- 项目现有 WindowInsets 工具方法
- `MainActivity` 对导航栏和 IME Insets 的处理
- 全屏阅读器与普通页面的差异

规则：

1. 不要在 Compose 页面中随意覆盖 Activity 的 Window 配置。
2. 不要同时由 View 和 Compose 重复消费同一组 Insets。
3. 不要简单写死顶部或底部 padding。
4. 全屏、透明状态栏、导航栏和键盘场景必须分别验证。
5. 页面切换后不能出现状态栏颜色错误或底部留白。
6. 多窗口模式下标题栏和内容区域必须正常。
7. Dialog 和 BottomSheet 必须检查软键盘、边到边显示及系统手势区域。

---

## 9. AI 助手执行任务的工作流程

每次收到 UI 迁移任务，按以下步骤执行。验证采用「分级 + 批量 + 抽样」：小改动只做静态自检，同类改动攒成一批再上设备抽样验证，全量矩阵留到阶段里程碑。禁止为了单个小改动反复装包、反复截图。

### 第一步：理解现状

先 grep 定位再读代码，不要无差别通读：

1. 目标 Activity/Fragment。
2. 对应 XML Layout。
3. 对应 ViewModel。
4. 使用到的 Adapter、自定义 View、Dialog。
5. 相关主题和颜色处理。
6. 页面跳转入口和返回行为。
7. 相关测试代码。

读文件用 `read` 的 offset/limit 只取相关段落，同一文件不要重复通读；不要只根据文件名猜测页面功能。

### 第二步：给出迁移计划

动手前，简短列出：

- 本次迁移的页面或组件。
- 计划修改的文件。
- 保留的业务逻辑。
- View 与 Compose 的边界。
- 可能影响的功能。
- 本批验证到哪一级（L0/L1/L2，定义见第四步）。

如果发现任务涉及数据库、阅读器渲染、导航架构或大规模依赖升级，先指出风险，不要擅自扩大任务范围。

### 第三步：小步实施

- 同类改动先攒成一批（例如同一属性铺开到 N 个 layout），改完整批再验证，不要改一个验一个。
- 一次只迁移一个页面或一组紧密相关的组件。
- 保持现有业务接口。
- 不做无关格式化。
- 不做全局字符串替换。
- 不删除尚未确认无引用的旧布局和资源。
- 不擅自改变产品交互。
- 不为了“代码更漂亮”重构无关模块。

### 第四步：分级验证（核心，按此执行）

**L0 静态自检 —— 每次改动，必须，零设备成本**

- 改动可编译。纯资源/XML 改动可并入批次验证统一编译，但本批次收尾前必须有成功构建。
- grep 核对引用：改了属性/资源/字符串名，确认调用点同步。
- 构建日志只保留错误行或末尾 20 行，不贴全量输出。

**L1 批次验证 —— 每批 5–10 个同类页面，一次装包、一组抽样**

- 机制相同的改动只验 1 个代表页，覆盖「亮色 + 暗色 + 旋转」三态；再验 1 个未改动页面做零回归基线（如书架）。
- 只有差异点才单独补：不同的 `TitleBar` id、不同的 `contentLayout`、页面自带主题色搜索栏、E-Ink / 透明底栏 / 自定义皮肤分支。
- 每批截图上限 6 张；一次 adb 会话内把亮暗、横竖屏切完再截，不要每切一次装一次包。
- 未抽样到的页面写“未逐页截图（与已验页机制相同）”，不算缺陷。

**L2 里程碑验证 —— 阶段收尾或发版前，每阶段一次**

- lint 基线对比：只对比 errors/warnings 是否回归基线，不追求清零。
- 设备矩阵：API 23、29/33、35/36；HyperOS 真机；非小米设备。
- 分屏/多窗口、键盘、TalkBack、130% 字体、E-Ink、`transparentNavBar`、自定义底栏图集皮肤、badge。
- 涉及阅读器 / 数据库 / 导航 / 导入导出的改动，在此补对应回归，不得用“批次抽样”代替。

环境无法提供某级验证时如实说明，不得声称已通过。

### 第五步：汇报结果（批次/任务结束一次性汇报）

模板（≤ 8 行，不逐条复述截图、不重复贴日志）：

```
改动：<文件/页面清单>
验证：L0 构建通过 | L1 抽样：<代表页>亮/暗/旋转、<差异页>、<零回归基线页>
未验证：<抽样未覆盖的页/场景>（原因）
未解决：<如有>
```

未执行的动作只写“待执行/未验证”。

---

## 10. 回归测试清单（按验证层级归类）

清单不再要求「每次全跑」，按 §9 的分级挑：

- **L0（每次改动）**：编译 + grep 引用核对，零设备成本。
- **L1（每批一次）**：本批抽样页的三态与差异点，截图 ≤6 张。
- **L2（每阶段一次）**：下方全矩阵，阶段收尾或发版前跑。

### 10.1 L0 · 静态自检（每次改动）

- [ ] 改动可编译；纯资源批次也必须在收尾前有成功构建。
- [ ] 改了属性/资源/字符串/调用名时，grep 确认引用点同步更新。
- [ ] 新增或删除的 layout、id、资源没有留下悬空引用。
- [ ] 变更范围只包含本批页面，没有混入无关格式化。

### 10.2 L1 · 批次抽样（每批 5–10 个同类页面一次）

对选中页面检查：

- [ ] 页面能正常打开和关闭。
- [ ] 返回键行为正确。
- [ ] 亮色模式显示正确。
- [ ] 暗色模式显示正确。
- [ ] 旋转/Activity 重建后状态正确，不崩溃。
- [ ] 状态栏和导航栏显示正常。
- [ ] 与一个未改动页做零回归基线比对（例如书架）。

以下差异点只在机制确实不同才补验，机制相同不重复：

- [ ] 不同 `TitleBar` id / 不同 `contentLayout` 的页面。
- [ ] 页面自带主题色搜索栏的页面。
- [ ] E-Ink、`transparentNavBar`、自定义底栏图集皮肤、badge 分支。
- [ ] 含 Dialog/Popup/BottomSheet 的页面（键盘、Insets、生命周期）。
- [ ] 含列表滚动/长按/刷新/状态恢复的页面。

### 10.3 L2 · 里程碑全量（阶段收尾或发版前一次）

**基础 UI**

- [ ] 用户自定义主题显示正确。
- [ ] 大字体和系统字体缩放正常（至少 130%）。
- [ ] TalkBack 可以识别主要控件。
- [ ] 键盘不会遮挡关键内容。
- [ ] 横屏、多窗口/分屏正常。

**Legado 核心功能**

迁移过程中不得破坏以下功能：

- [ ] 书架显示、排序和管理。
- [ ] 搜索书籍。
- [ ] 打开书籍和阅读章节。
- [ ] 阅读进度保存和恢复。
- [ ] 目录跳转。
- [ ] 字体、字号、行距、背景和阅读主题设置。
- [ ] 书源管理、导入、导出和编辑。
- [ ] 订阅源管理和 RSS 阅读。
- [ ] 书籍导入、下载和缓存。
- [ ] 备份、恢复和同步。
- [ ] WebView、音频和视频功能。

如果本次改动没有涉及某项功能，可以标记为“本次未涉及”，不要伪称已完成验证。

**设备/API 覆盖**

- [ ] Android API 23：最低版本兼容性。
- [ ] Android API 28/29：旧系统行为。
- [ ] Android API 33：动态颜色和新系统能力。
- [ ] Android API 35/36：新系统栏、边到边和目标 SDK 行为。
- [ ] 小米 HyperOS 设备：目标设计风格。
- [ ] 非小米 Android 设备：不能依赖小米系统私有能力。
- [ ] lint：与现有基线（9 errors / 1062 warnings）对比，无新增回归，不追求清零。

---

## 11. 严格禁止事项

未经用户明确同意，不得：

1. 一次性把全项目 XML 替换成 Compose。
2. 删除或重写阅读器核心渲染模块。
3. 改变书源规则执行和 JavaScript 行为。
4. 改变 Room 数据库结构或数据迁移。
5. 改变备份、恢复、导入、导出格式。
6. 改变 Activity 的 launchMode 或任务栈行为。
7. 替换现有导航体系。
8. 移除原有主题配置、E-Ink 模式或壁纸主题。
9. 无理由提高 minSdk。
10. 无理由升级 Kotlin、AGP、Gradle 或 targetSdk。
11. 删除旧 View/XML 组件而不做引用检查和回归验证。
12. 引入与本次任务无关的新 UI 框架或第三方依赖。
13. 将 Miuix Experimental API 当作稳定 API 使用而不锁定版本。
14. 将“能够编译”描述成“迁移完成”。

---

## 12. 完成标准（Definition of Done）

完成迁移按分级验证的粒度判定，不要求每个页面都跑全矩阵。

**必须满足（逐页或逐批确认，L0/L1）**

- [ ] 已使用 Miuix 组件或基于 Miuix 的 Compose UI 完成目标界面。
- [ ] 原有业务逻辑没有被复制、绕过或破坏。
- [ ] 页面导航和返回行为保持一致。
- [ ] 亮色、暗色显示正确（代表页实测；机制相同的同类页可抽样）。
- [ ] 系统栏、Insets、键盘和弹窗行为在抽样页上正常。
- [ ] 生命周期没有泄漏或重复订阅。
- [ ] 最低支持 API 23 的兼容性没有被破坏（编译期检查 + 已有验证记录）。
- [ ] Debug 构建通过。
- [ ] 变更范围清晰，没有混入无关重构。

**里程碑确认（每阶段或发版前一次，L2）**

- [ ] 用户自定义主题、E-Ink、壁纸主题正常。
- [ ] 字体缩放（≥130%）与 TalkBack 基本可用。
- [ ] 横屏、分屏/多窗口正常。
- [ ] 与页面相关的核心功能回归通过；未涉及的项标注“本次未涉及”。
- [ ] lint 与基线对比无新增回归。

“能够编译”只代表 L0 通过，不等于迁移完成；抽样未覆盖的场景必须如实标注。

---

## 13. 推荐的第一个实际任务

不要从主界面或阅读器开始。

建议先建立一个独立的 Miuix 验证页面，完成以下内容：

1. 接入最小 Miuix Android 依赖。
2. 在现有 AppCompat Activity 中通过 ComposeView 展示 Miuix 页面。
3. 使用统一的 `LegadoMiuixTheme`。
4. 展示 TopAppBar、列表、按钮、开关和 Dialog 等基础组件。
5. 验证亮色、暗色、系统主题切换。
6. 验证 Android API 23 兼容性。
7. 验证 ComposeView 生命周期释放。
8. 记录构建配置、依赖版本和已知限制。

以上验证项按 §9 分级执行：调试期只做 L0/L1 抽样，完整设备矩阵（API 23/29/33/35–36、HyperOS、分屏、TalkBack）归入 L2 一次跑完，不要每改一处就重跑一遍。

验证完成后，再迁移一个低风险的真实页面，例如“关于页面”。

**第一阶段的成功标准不是改造了多少页面，而是证明 Miuix 能够在现有 Legado 工程中稳定运行，并且不会破坏现有 View 系统。**

---

## 14. 版本和 API 变更纪律

Miuix 仍处于快速迭代阶段。AI 助手每次编写 Miuix 代码前：

1. 先查看官方文档中对应组件的当前 API。
2. 查看当前锁定版本的 Release Notes。
3. 优先参考官方 Example，而不是搜索引擎中的旧代码片段。
4. 不要凭记忆编造组件名、参数名或 import 路径。
5. 如果 API 与本文示例不同，以当前实际依赖版本为准。
6. 不要为了适配某个过时示例而降级整个项目。
7. 如果必须升级 Miuix，先说明 breaking changes 和回归范围。

官方 Release 页面：

<https://github.com/compose-miuix-ui/miuix/releases>

---

## 15. 最重要的一条

**迁移 UI，不等于重写应用。**

Legado 已经积累了大量成熟的业务逻辑、阅读能力、数据兼容和系统适配。Miuix 的任务是改善界面体验，而不是让这些成熟能力为 UI 框架让路。

遇到不确定的架构问题时，先读代码、查官方文档、给出方案；不要猜测，不要擅自扩大改动范围。
