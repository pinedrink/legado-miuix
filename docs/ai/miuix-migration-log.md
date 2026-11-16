# Legado × Miuix 迁移交接

项目根：`D:\program\legado-master\legado-master`
构建：`$env:JAVA_HOME='C:\Users\asahi-ms\.jdks\jbr-21.0.11'; $env:GRADLE_USER_HOME='D:\gradle-home'; .\gradlew.bat :app:assembleDebug --console=plain`
Linux 复现构建（2026-11-15 起的工作区）：`JAVA_HOME=/home/asahi/.jdks/jbr-21.0.11 ./gradlew :app:assembleDebug`，需把 `local.properties` 的 `sdk.dir` 临时改成 `/home/asahi/Android/Sdk`（构建完还原为 Windows 路径）；模拟器 `emulator -avd Medium_Phone_API_37.0 -no-window -gpu swiftshader_indirect`，adb 在 `/home/asahi/Android/Sdk/platform-tools/adb`
lint 基线：`:app:lintAppDebug` → 9 errors / 1062 warnings（9 个 error 均为接入前既有）；顶栏/底栏阶段 1 改完后复跑仍为 **9 errors / 1062 warnings**；E2 第 1 批复跑 = **9 errors / 1076 warnings**（error 未变，本批改动的 5 个文件 0 新增告警，差值未逐条归因）

## 已完成
- 阶段 A：`ui/miuix/MiuixDemoActivity.kt` + `MiuixDemoScreen.kt`
- 阶段 B：`ui/compose/theme/LegadoMiuixTheme.kt`、`ui/compose/MiuixComposeActivity.kt`、`ui/compose/components/LegadoMiuixScaffold.kt`；drawable `bg_miuix_list_item.xml` / `bg_miuix_card_item.xml` / `bg_miuix_grid_item.xml`；色值 `miuix_item_surface`
- 阶段 C：关于页整页 Compose 化
- 阶段 D：我的、主题设置、其它设置、备份与恢复、封面设置、启动界面、文件管理、替换净化、字典规则、缓存/导出、规则订阅、自动任务、书架列表、书架管理、搜索结果、发现结果、RSS 首页网格、RSS 文章列表（样式 0/1/3）、书签、高亮、书籍详情微调
- 阶段 E（顶栏 + 底栏，阶段 1，opt-in）：见下节「顶栏 / 底栏阶段 1 已完成」

## 已知坑
1. `themeMode` 被设成「亮色」时 `cmd uimode night yes` 无效，先改回「跟随系统」
2. 卡片底色别用 `@color/background_card`（与页面几乎同色），用 `@color/miuix_item_surface`
3. 适配器 `convert()` 里的 `setBackgroundColor` 会覆盖 XML 背景
4. Material `TextInputLayout` 在 AppCompat 主题下无法换皮，表单页暂不迁移
5. `bg_miuix_card_item` 的 `state_focused` 用 `btn_bg_press`，手机上首行可能短暂发灰
6. mock 数据：`python -m http.server <port> --bind 127.0.0.1` + 模拟器 `10.0.2.2:<port>`
7. `miuix-icons` 是 **Compose `ImageVector`**（`top.yukonga.miuix.kmp.icon.extended.*`），**不能直接赋给 Toolbar 的 `navigationIcon` / `menu.item.icon`（Drawable）**。阶段 1 没换图标，只做了 24dp + tint；真要换得新画 vector drawable 或做 ImageVector→Drawable 桥，建议留到阶段 2 随 Compose TopAppBar 一起做
8. 本项目“暗色”是由**主色明暗**推导的（`MaterialValueHelper.kt:138` `Context.isDarkTheme = isColorLight(ThemeStore.primaryColor)`），所以顶栏/底栏前景色必须按**实际背景色**算，不能复用 `isDarkTheme`；`context.getPrimaryTextColor(isLightBar)` 才是正确的
9. `BaseActivity.setupSystemBar()` 只认构造参数 `toolBarTheme`，**不看 TitleBar 自己的底色**。把顶栏改成浅色 surface 后状态栏图标会白压白（看不见），必须在 `TitleBar.applyMiuixForegroundColor()` 里自己调 `activity?.setLightStatusBar(isLightBar)`。**并且**这一处会和 `setupSystemBar()` 里的 `setStatusBarColorAuto` 形成「两个控制点」（违反 `ANDROID_PLATFORM_API_GUIDELINES.md` §4.8）：旋转 / 分屏 / 配置变化时 `setupSystemBar()` 会把它改回主色判断。已修：`TitleBar` 暴露 `refreshMiuixForeground()`，`BaseActivity.setupSystemBar()` 末尾 `findViewById<TitleBar>(R.id.title_bar)?.refreshMiuixForeground()`（onCreate 那次 title_bar 还没 inflate，null 自动跳过）
10. 本项目主题是 AppCompat（非 Material3），**没有 `bottomNavigationStyle`**：`BottomNavigationView` 不会创建 active indicator View，代码里 `setItemActiveIndicator*` 全是空操作。必须在 XML 给 `app:itemActiveIndicatorStyle` 一个真样式（`MiuixBottomNavIndicator`）才能让指示器 View 被创建
11. `<style name="TextAppearance.App.Xxx">` 这类**带点的自定义样式名会被 AAPT 当隐式父样式解析**，报 `resource style/TextAppearance.App not found`。自定义样式名不要用点分前缀（本项目用 `MiuixBottomNavText` / `MiuixBottomNavIndicator`）
12. 未导出 Activity 不能 `am start`（`SecurityException: not exported`），只能从界面点进去
13. 导入本地书测试：`/sdcard/Download/Books/x.txt` + 书架“导入书籍”走 **SAF 图形目录选择**，“Manual Input”输入路径不生效
14. 有些页面在 TitleBar 下面还自带**主题色（primaryColor）搜索栏/工具条**（例：`activity_txt_toc_rule.xml`），把顶栏改成白色后会和这条棕色栏割裂。这类页先不要 opt-in，等把自带工具条一起迁到 `miuix_item_surface` 再做；`activity_txt_toc_rule.xml` 已回滚为未 opt-in
15. 模拟器 `adb input tap` 坐标必须来自 `uiautomator dump`；按预览图（838 宽）目测的坐标要乘 `1080/838`，否则点在空处
16. 把 `TitleBar` 换成自定义 View 时**不能沿用 `@+id/title_bar`**：`BaseActivity.kt:101` 有 `findViewById<TitleBar>(R.id.title_bar)` 的泛型强转，类型不符直接 `ClassCastException` 崩在 onCreate。试点页改用 `@+id/miuix_top_bar`（`BaseActivity.setupSystemBar()` 里的 `title_bar`/`titleBar` 查找自然返回 null）

17. Compose 顶栏解析菜单 XML 时，`showAsAction` 必须用 `XmlPullParser.getAttributeIntValue()`；`getAttributeValue()` 拿到的是 `0x2` 这类字符串，按 `"always"` 比较会全部落到 `never`（表现：图标动作全消失、全进溢出菜单）
18. 若 `MenuAction.iconRes` 为 0 又判成 inline，`ContextCompat.getDrawable(ctx, 0)` 会抛 `Resources$NotFoundException: Resource ID #0x0`；取值和渲染两处都要判 0（已加防线）
19. Compose 顶栏派发点击要真实 `MenuItem`：用 `PopupMenu(context, this).menu.add(0, itemId, 0, title)` 造同 id 的 item 再调 `BaseActivity.onCompatOptionsItemSelected(item)`，页面原有 `when (item.itemId)` 分支照常工作；溢出菜单的 visible/checked 依赖页面自己的 `onMenuOpened`，打开前要手动调一次 `Activity.onMenuOpened(Window.FEATURE_OPTIONS_PANEL, menu)`（`book_source_edit` 的「登录」隐藏、`自动补全` 勾选即靠这个）
## 顶栏 / 底栏阶段 1 已完成（2026-11-08，opt-in，可回滚）

备份：`Docs-mem\backup-topbar-bottombar-20261108.zip`（`app/src/main/res` + `TitleBar.kt` + `ThemeBottomNavigationVIew.kt` + `MainActivity.kt`）

### 顶栏（opt-in，默认关闭）
新增属性 `app:miuixTopBar="true"`（`attrs.xml` 的 `TitleBar` styleable）。

**已 opt-in 的页面（32 个 activity + 4 个主界面 fragment，另 2 个 contentLayout 辅助布局）**

其中 31 个 activity 的顶栏已换成 Compose 版 `MiuixTopBarView`（图标动作 + 溢出菜单 + 其中 10 页的 Compose 搜索框）：第 7 批 1 页、第 8 批 2 页、第 9 批 9 页、第 10 批 10 页、第 11 批 3 页、第 12 批 2 页、第 13 批 3 页、第 14 批 1 页（见下）。只剩 `activity_donate`（孤儿布局，无 Activity 类）。
- 第 1 批（试点）：`activity_book_source.xml`（contentLayout=`@layout/view_search_miuix`）、`activity_chapter_list.xml`（`@layout/view_tab_layout_miuix`）
- 第 2 批（备份 `Docs-mem\backup-optin-batch1-20261108.zip`）：`activity_auto_task.xml`、`activity_replace_rule.xml`、`activity_rss_source.xml`、`activity_import_book.xml`、`activity_read_record.xml`，全部把 `contentLayout` 从 `@layout/view_search` 换成 `@layout/view_search_miuix`
- 第 3 批（备份 `Docs-mem\backup-optin-batch2-20261108.zip`）：`activity_arrange_book.xml`、`activity_book_search.xml`、`activity_file_manage.xml`、`activity_rss_source_debug.xml`、`activity_search_content.xml`、`activity_source_debug.xml`，同样换 `view_search_miuix`
- 第 4 批（备份 `Docs-mem\backup-optin-batch3-20261108.zip`，只有标题、无 contentLayout 的普通页）：`activity_all_bookmark.xml`、`activity_cache_book.xml`、`activity_bottom_bar_skin.xml`、`activity_config.xml`、`activity_donate.xml`，只加 `app:miuixTopBar="true"`
- 第 5 批（备份 `Docs-mem\backup-optin-batch4-20261109.zip`）：`activity_dict_rule.xml`、`activity_highlight_rule.xml`、`activity_rule_sub.xml`、`activity_rss_favorites.xml`、`activity_bottom_bar_skin_assign.xml` 加 `app:miuixTopBar="true"`；`activity_txt_toc_rule.xml` **已回滚**（页面自带主题色搜索栏，见已知坑 14）
- 第 6 批（2026-11-15，备份 `Docs-mem/backup-optin-batch5-20261115.zip`）：`activity_auto_task_debug.xml`、`activity_auto_task_edit.xml`、`activity_book_info_edit.xml`、`activity_book_source_edit.xml`、`activity_explore_show.xml`、`activity_qrcode_capture.xml`、`activity_replace_edit.xml`、`activity_rss_artivles.xml`、`activity_rss_source_edit.xml` 只加 `app:miuixTopBar="true"`（9 个均无 `contentLayout`，无 `themeMode`/`opaque`）
- 第 7 批（2026-11-15，E2 第 1 批，备份 `Docs-mem/backup-e2-batch1-20261115.zip`）：`fragment_bookshelf1.xml`、`fragment_bookshelf2.xml`、`fragment_rss.xml`、`fragment_explore.xml` 加 `app:miuixTopBar="true"`；rss/explore 的 `contentLayout` 换成 `@layout/view_search_miuix`（id 仍是 `search_view`）。bookshelf1 的 tab 条暂留 `view_tab_layout_min`（`TitleBar.applyMiuixForegroundColor()` 会把 tab 文字按白底调成深色，实测可读）
- 第 8 批（2026-11-15，E2 第 2 批「顶栏 Compose 化 · 菜单桥」，备份 `Docs-mem/backup-e2-batch2-20261115.zip`）：`activity_book_source_edit.xml`、`activity_auto_task_edit.xml` 的 `TitleBar` 换成 `io.legado.app.ui.compose.components.MiuixTopBarView`（id 改 `miuix_top_bar`，`app:mtbTitle` + `app:mtbMenu="@menu/source_edit"` / `@menu/auto_task_edit`）；`MiuixTopBarView` 新增 `mtbMenu` 属性、菜单 XML 解析、图标动作与 Compose 溢出菜单；`attrs.xml` 新增 `mtbMenu`（reference）
- 铺开时的机械步骤：在目标 layout 的 `<io.legado.app.ui.widget.TitleBar>` 块里加 `app:miuixTopBar="true"`，有 `app:contentLayout` 的同步换成 `_miuix` 版；每页亮/暗 + 旋转各看一眼
- 注意 `TitleBar` 的 id 有两种：多数 layout 用 `@+id/title_bar`，少数用 `@+id/titleBar`（`activity_import_book.xml`、`activity_file_manage.xml`），`BaseActivity.setupSystemBar()` 里两种都要刷新
- 第 9 批（2026-11-15，E2 第 3 批「菜单桥铺开」，备份 `Docs-mem/backup-e2-batch3-20261115.zip`）：`activity_all_bookmark`、`activity_book_info_edit`、`activity_bottom_bar_skin`、`activity_bottom_bar_skin_assign`、`activity_cache_book`、`activity_highlight_rule`、`activity_qrcode_capture`、`activity_replace_edit`、`activity_rss_favorites` 的 `TitleBar` 换成 `MiuixTopBarView`（id `miuix_top_bar`，`mtbTitle` + `mtbMenu` 指向各自 menu）；`CacheActivity.kt` 改用 `miuixTopBar.setSubtitle(...)`、`BottomBarSkinAssignActivity.kt` 改用 `miuixTopBar.setTitle(...)` 并去掉 `setNavigationOnClickListener`；`MiuixTopBarView` 修两处通用问题：`showAsAction=always` 但无图标的项（二维码页「Gallery」）改判进溢出菜单，且溢出按钮在有这类项时也显示
- 第 10 批（2026-11-15，E2 第 4 批「Compose 搜索框」，备份 `Docs-mem/backup-e2-batch4-20261115.zip`）：新建 `ui/compose/components/MiuixSearchBar.kt`（SearchView 兼容 API 的状态载体：`query`/`setQuery(text, submit)`/`queryHint`/`isIconified`/`onActionViewExpanded`/`setOnQueryTextListener(SearchView.OnQueryTextListener)`/`setOnQueryTextFocusChangeListener`/`setOnSearchClickListener`/`applyTint`），`MiuixTopBarView` 新增 `app:mtbSearch`，在顶栏下方渲染 Compose 胶囊搜索行（id 仍 `search_view`，方便页面继续 `findViewById`）；10 个 `view_search_miuix` 页面换成 `MiuixTopBarView + mtbSearch` 并把页面里的 `searchView` 类型从 `SearchView` 改成 `MiuixSearchBar`：`activity_book_source`、`activity_book_search`、`activity_arrange_book`、`activity_auto_task`、`activity_file_manage`、`activity_import_book`、`activity_read_record`、`activity_rss_source`、`activity_search_content`、`activity_source_debug`
- 第 11 批（2026-11-15，E2 第 5 批「动态菜单/子菜单」，备份 `Docs-mem/backup-e2-batch5-20261115.zip`）：`MiuixTopBarView` 菜单管线改为「真实 Menu + onPrepareOptionsMenu」——用 `MenuInflater` 把 `mtbMenu` inflate 进 `PopupMenu(context,this).menu`，再调页面的 `onPrepareOptionsMenu(menu)`，所以可见性/checked/动态子菜单全部沿用页面原逻辑；溢出菜单支持一层子菜单（父项显示 `›`，点开二级 Popup）；`BaseActivity` 新增 `override fun invalidateOptionsMenu()`，把调用转成 `MiuixTopBarView.refreshMenu()`（`menuVersion++` 触发重建）；inline 动作按 `mtbMenu` XML 里 `showAsAction=always` 的 id 集合判定。转换 3 页：`activity_replace_rule`（搜索 + 分组子菜单）、`activity_dict_rule`（动态菜单）、`activity_rss_source_debug`（搜索）。
- 第 12 批（2026-11-15，E2 第 6 批「菜单里的 actionView 搜索项」，备份 `Docs-mem/backup-e2-batch6-20261115.zip`）：`mtbMenu` 里带 `app:actionViewClass="androidx.appcompat.widget.SearchView"` 的项不再渲染成图标/菜单行，改由 `MiuixTopBarView.searchBar`（`MiuixSearchBar`）承担；新增 `MiuixTopBarView.setTitle(resId: Int)` / `setTitle(CharSequence?)` / `setSearchBarVisible(visible)`；`MiuixSearchBar` 的 `BasicTextField` 增加硬件 Enter = `onQueryTextSubmit`（`onPreviewKeyEvent`）。转换 2 页：`activity_config`（设置页，原来的 `menu.search_view` 搜索）、`activity_rss_artivles`（`RssSortActivity`，搜索行按 `rssSource.searchUrl` 是否为空显示）。
- 第 13 批（2026-11-15，E2 第 7 批「收尾普通页」，备份 `Docs-mem/backup-e2-batch7-20261115.zip`）：`activity_rule_sub`（`RuleSubActivity`，`mtbMenu=@menu/source_subscription`）、`activity_rss_source_edit`（`RssSourceEditActivity`，`mtbMenu=@menu/source_edit`）、`activity_explore_show`（`ExploreShowActivity`，标题由 `binding.miuixTopBar.setTitle(...)` 动态设置）三页换成 Compose 顶栏；`MiuixTopBarView.dispatchItem` 改为「先反射 `MenuItemImpl.performClick()`，未处理再回落 `onCompatOptionsItemSelected`」，以兼容 `ExploreShowActivity` 这种用 `menu.add(...).setOnMenuItemClickListener {}` 动态建的菜单。`activity_chapter_list` 暂缓：`TocActivity` 把 `TabLayout` 放在 `TitleBar` 的 `app:contentLayout` 里，且菜单搜索项是 actionView，需要先设计 Compose tab 行。
- 第 14 批（2026-11-15，E2 第 8 批「目录页 + 收尾修复」，备份 `Docs-mem/backup-e2-batch8-20261115.zip`）：`activity_chapter_list`（`TocActivity`）改成 Compose 顶栏——`MiuixTopBarView`（`mtbSearch="true"` + `mtbMenu=@menu/book_toc`）+ 原来的 `TabLayout` 移到顶栏下方单独一行（`@color/miuix_item_surface` 底色、`AppTheme.MiuixBarOverlay` 文字主题、52dp 高）；`TocActivity` 里 `tabLayout = binding.tabLayout`、搜索框改成 `binding.miuixTopBar.searchBar`（`MiuixSearchBar`），去掉 `(search.actionView as SearchView)`。
- 同批修复前几批遗留的布局/代码问题：
  - 9 个 ConstraintLayout 页面（`activity_all_bookmark`/`arrange_book`/`book_search`/`file_manage`/`import_book`/`search_content`/`source_debug`/`rss_source_debug`/`explore_show`）里顶栏缺 `layout_constraintTop/Start/End`，且下级视图仍引用 `@id/title_bar` → 统一补约束并把引用改成 `@id/miuix_top_bar`（lint 的 `MissingConstraints`/`NotSibling`/`UnknownId` 从 21 errors 降到基线 9）。
  - `BaseActivity` 删掉已无布局定义的 `R.id.titleBar` 兜底（布局里 TitleBar 统一 `@id/title_bar`）。
  - `SearchActivity.receiptIntent` 里 `searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text).requestFocus()` 改为 `searchView.requestFocus()`（MiuixSearchBar 没有内部 search_src_text，原写法直接 NPE 崩溃）。
  - `BaseImportBookActivity` 的 `searchView` 类型从 `SearchView` 改为 `MiuixSearchBar`（否则 `ClassCastException` 崩溃）。
- 第 15 批（2026-11-15，E2 第 9 批「全局下拉菜单 Miuix 化」，备份 `Docs-mem/backup-e2-batch9-20261115.zip`）：书架/订阅等页右上角 ⋮ 弹出的菜单原本由两套实现绘制——未迁移到 Compose 顶栏的页面走 `ToolbarOverflowMenuExtensions.installMd3OverflowMenu` → `PopupAction`（MD3 样式：`applyMd3PopupStyle()` 给 8dp 阴影 + `context.popupBackground`），所以即使顶栏已是 Miuix 风格，菜单仍是旧样子。本批改成 Miuix 外观：
  - `app/src/main/java/io/legado/app/utils/PopupWindowExtensions.kt` 的 `applyMd3PopupStyle()`：`elevation = 0f`、内容背景改 `@drawable/bg_popup_menu`（16dp 圆角 + `miuix_item_surface`，亮 #FFFFFF / 暗 #303030），不再用 MD3 阴影。
  - `app/src/main/res/layout/popup_action.xml`：外壳背景改 `bg_popup_menu`，内边距 8dp 纵向 / 6dp 横向。
  - `app/src/main/res/layout/item_popup_action.xml`：文字 15sp、颜色 `@color/miuix_menu_text`（values #1A1A1A / values-night #F2F2F2）、前置图标 20dp。
  - `PopupAction.kt`：竖排菜单行高 48dp → 44dp、左右内边距 16dp → 14dp；`showAsDropDown` 里按可见区域把弹窗往左收 8dp，避免贴右边缘时圆角被裁。
  - `ToolbarOverflowMenuExtensions.kt`：溢出菜单 `showAsDropDown(anchor, -8dp, 4dp)`。
  - `values/styles.xml`：`Style.MenuItemText` 改 15sp + `miuix_menu_text`；`Style.PopupMenu` 补 `popupElevation=0`、`overlapAnchor=false`；`AppTheme.AppBarOverlay.Light/Dark`、`AppTheme.MiuixBarOverlay`、`AppTheme.PopupOverlay` 都补 `actionOverflowMenuStyle` / `android:popupMenuStyle` / `android:popupBackground` / `itemTextAppearance` / `colorBackgroundFloating`，覆盖仍在用 AppCompat Toolbar 的页面与对话框。
  - `values/colors_miuix.xml`、`values-night/colors_miuix.xml` 新增 `miuix_menu_text`。
- 验证（API 37）：书架 ⋮ 菜单亮色 #FFFFFF / 暗色 #303030、16dp 圆角、无阴影、行高 44dp、右侧留 8dp，**不再是 MD3 的 #EEEEEE 方角菜单**；同一套 `PopupAction` 也被其它页面的排序/分组弹出菜单复用。
- 第 16 批（2026-11-15，阅读器菜单 Miuix 化，备份 `Docs-mem/backup-reader-menu-20261115.zip`）：用户明确要求动阅读器菜单，调研后按「只改外围 chrome、不动正文渲染/翻页/手势」处理：
  - `app/src/main/res/layout/view_read_menu.xml`：四个 FAB（fabSearch/fabAutoPage/fabReplaceRule/fabNightTheme）`app:elevation` 2dp→0dp、`pressedTranslationZ` 2dp→0dp、补 `borderWidth=0dp`（去 MD3 阴影，贴近 Miuix 扁平圆形按钮）；底部按钮行（目录/朗读/界面/设置/备忘录）宽度 60dp→64dp、图标 maxHeight 20dp→22dp、paddingBottom 7dp→8dp。
  - `ReadMenu.initView()`：非 E-Ink 下 `llBottomBg` 背景由纯色改为顶部 16dp 圆角的 `GradientDrawable`（底部菜单呈 Miuix 浮层观感，颜色仍跟随阅读主题 `bgColor`）；阅读器顶栏底色从主题主色改为跟随 `bgColor`（亮色浅灰 / 暗色深灰），并同步 `setLightStatusBar(ColorUtils.isColorLight(bgColor))`。
  - `ReadMenu.applyMenuStatusBar()` 新方法 + `ReadBookActivity.upSystemUiVisibility()` 在菜单可见时调用，修正「顶栏变浅后状态栏图标仍是白色」。
  - 不动：正文绘制、翻页、手势、朗读、阅读进度、E-Ink 分支、沉浸式分支（immersive 仍用阅读页背景色）。
- 验证（API 37 模拟器，`assembleDebug` 通过）：亮色阅读器 → 顶栏浅色 + 深色图标/文字、状态栏深色图标、底部菜单顶部 16dp 圆角、FAB 无阴影；切夜间 → 顶栏 #303030 级深色 + 浅色图标、状态栏浅色图标、底栏同色圆角；菜单进出动画与按钮点击未变，logcat 无 FATAL。

- 第 17 批（2026-11-15，阅读器底栏按钮行 Compose 化，备份 `Docs-mem/backup-reader-bottom-bar-20261115.zip`）：按「方案 B」只替换底部 5 个按钮那一行，其余（TitleBar、亮度条、上一章/进度条/下一章、4 个 FAB）保持原生 View。
  - 新增 `app/src/main/java/io/legado/app/ui/book/read/ReadMenuBottomBar.kt`：`FrameLayout + ComposeView`（`DisposeOnViewTreeLifecycleDestroyed`），内部 `Row` 均分 5 项（目录/朗读/界面/设置/备忘录），图标 22dp、标签 12sp、行高 56dp、按下 8% 文字色圆角高亮（12dp），长按朗读可用；颜色由 `setTextColor()` 传入，不使用固定色；整体套 `LegadoMiuixTheme`。
  - `view_read_menu.xml`：把原来 5 个 60dp 按钮 + 间隔 View 的横向 LinearLayout 换成 `<io.legado.app.ui.book.read.ReadMenuBottomBar android:id="@+id/rv_bottom_nav" height=56dp>`。
  - `ReadMenu.kt`：`initView()` 里 10 行 `iv*/tv* setColorFilter/setTextColor` → `rvBottomNav.setTextColor(textColor)`；`runMenuIn()` 的 `llMemo/memoSpacer` 可见性 → `rvBottomNav.setShowMemo(showMemo)`；`bindEvent()` 的 6 个点击/长按监听改为给 `rvBottomNav` 赋回调，回调体与原来完全一致（`runMenuOut { ... }`）。
- 验证（API 37，`assembleDebug` 通过）：亮色/夜间两套主题下底栏渲染正常（颜色跟随阅读主题、无写死色）；目录 → 打开 `TocActivity`；朗读 → 起 TTS（弹出通知权限提示）、朗读中再点 → 打开朗读设置；界面/设置/备忘录沿用原回调；菜单进出动画正常；logcat 无 `FATAL EXCEPTION`。

- 第 18 批（2026-11-15，阅读器设置弹窗 Miuix 化，备份 `Docs-mem/backup-reader-dialogs-20261115.zip`）：处理「界面」「更多设置」「朗读设置」三个底部弹窗。
  - 弹窗 chrome：`ReadStyleDialog` / `ReadAloudDialog` / `MoreConfigDialog` 的窗口背景由 `@color/background` 换成 16dp 顶部圆角 `GradientDrawable`（颜色 `context.bottomBackground`，`utils/SheetBackground.roundedTopDrawable`），内容根背景改透明露出圆角；新增 `fitSheetHeight()` 兜底。
  - 开关：`lib/theme/view/ThemeSwitch.kt` 从 `SwitchCompat + applyTint` 重写为 `CompoundButton` 自绘 Miuix 风（44x26dp 轨道、22dp 白色滑块、on=`primaryColor`、off=30% 黑/白、160ms 动画、`onMeasure` 最小高 26dp），保留 `setOnUserCheckedChangeListener` 语义，全工程 25 个使用处无需改动。
  - 布局：两个弹窗布局根外各包一层 `io.legado.app.ui.widget.MaxHeightNestedScrollView`（上限 90% 屏高，内容不足时仍 wrap）。
- 验证（API 37，`assembleDebug` 通过）：「界面」弹窗顶部 16dp 圆角、背景跟随 `bottomBackground`；「更多设置」圆角正常、内部 Preference 列表可滚动；「朗读设置」圆角 + 开关渲染为 Miuix 胶囊（关态轨道 30% 黑、开态 primary）、点击切换与文字颜色正常；logcat 无 `FATAL EXCEPTION`。
- 已知问题：density 2.5 的 1080x2400 模拟器上「界面」弹窗最后一行色卡被裁（改动前同样存在，属布局定高问题，真机待复核）；「更多设置」内部 Preference 的开关仍是 AndroidX 默认样式（未替换为 `ThemeSwitch`）。

- 第 19 批（2026-11-15，Preference 列表开关 Miuix 化，备份 `Docs-mem/backup-preference-switch-20261115.zip`）：解决第 18 批遗留的「更多设置」内部开关仍是 AndroidX 默认样式。
- 第 20 批（2026-11-15，真机反馈四项之一/之四：顶栏搜索色差 + 悬浮底栏）：
  - 顶栏色差根因：`MiuixTopBarView` 的 `Column` 自身没有背景，`SmallTopAppBar` 只有标题行着色，搜索行下方露出窗口底色（`@color/background`），与标题行的 `miuix_item_surface` 形成一条"拼接线"；已给 `Column` 统一加 `background(barColor)`（透明/E-Ink 时保持透明），标题行与搜索行同色。
  - 底栏改悬浮式（类 iOS）：新增 `app/src/main/res/drawable/bg_miuix_floating_nav.xml`（`miuix_item_surface` + 28dp 全圆角）；`ThemeBottomNavigationVIew` 非透明底栏时改用该背景，`outlineProvider = BACKGROUND`、`clipToOutline = true`、`elevation = 8dp`；`activity_main.xml` 给底栏包一层 `FrameLayout`，留左右 12dp、底部 10dp 外边距，ViewPager 内容不再被贴边底栏压住。
  - 验证（API 37）：书源页顶栏与搜索行同为 #FFFFFF（搜索胶囊 6% 黑）；底栏为圆角悬浮卡片 + 投影，选中胶囊与文案正常；「我的」页暗/亮两套均正常，无 FATAL。
- 第 21 批（2026-11-15，书籍信息页现代化 + 标签统一）：
  - `LabelsBar`（`app/src/main/java/io/legado/app/ui/widget/LabelsBar.kt`）不再用 `AccentBgTextView` 的强调色块，改成自绘中性胶囊：`GradientDrawable` 8dp 圆角、亮色 `0x11000000`/暗色 `0x22FFFFFF` 底、`@color/primaryText` 文字、内边距 9dp×5dp、间距 6dp；影响所有使用 LabelsBar 的页面（书源/书籍列表标签、书籍信息页等）。
  - `activity_book_info.xml`：书名 `18sp` → `21sp` + `textStyle="bold"`；「换源 / 设置分组 / 查看目录」由 `AccentBgTextView`（实心强调色块）改为 `AccentTonalBgTextView`（12% 强调色的 tonal 胶囊，radius 8dp，内边距 10dp×4dp）；底部操作条背景由 `@color/background_menu` 改为不透明 `@color/background`，内边距 16dp×10dp，「阅读」按钮恢复 `AccentBgTextView` 实心强调色主按钮。
  - 验证（API 37）：书籍信息页标题、灰色标签胶囊（16.8万/连载中/日期）、tonal 操作胶囊、实心「阅读」按钮、底部操作条均正常，logcat 无 FATAL。
- 第 22 批（2026-11-15，AlertDialog 全局 Miuix 化）：
  - 新增 `app/src/main/res/drawable/bg_miuix_dialog.xml`（20dp 全圆角 + `miuix_item_surface`：亮 #FFFFFF / 暗 #303030）。
  - `app/src/main/java/io/legado/app/lib/dialogs/AndroidAlertBuilder.kt` 新增 `AlertDialog.styleMiuix()`，在 `build()` 与 `show()` 里统一应用：窗口背景换 `bg_miuix_dialog`、左右各留 24dp（`setLayout(screenWidth - 48dp, WRAP_CONTENT)`）、`android.R.id.title` 改 20sp 加粗。覆盖全工程通过 `alert(...)` 创建的所有对话框（书架布局、添加网址、各类确认/输入框等）。
  - 验证（API 37）：书架布局弹窗为 20dp 圆角 + 20sp 加粗标题 + 24dp 边距；添加网址输入弹窗同款；暗色下弹窗底色实测 `(48,48,48)`，与菜单/顶栏/底栏的 Miuix surface 一致；无 FATAL。
- 第 23 批（2026-11-15，书籍信息页折叠顶栏）：
  - `BookInfoActivity` 新增 `setupCollapsingBar()`：监听 `scrollView` 滚动，`t = scrollY/220dp`，顶栏底色由透明渐变到 `miuix_item_surface`、文字/图标由白渐变为 `#FF1A1A1A`、封面 `bgBook` 透明度 `1-t`、`t>0.5` 时切换 `setLightStatusBar`。
  - 验证（API 37）：亮/暗渲染正常、无 FATAL；当前测试书简介较短、页面不满屏不可滚动，折叠过程未能实测（需长简介书籍复核）。
- 第 24 批（2026-11-15，订阅/发现列表卡片现代化）：
  - 新增颜色 `miuix_card_surface`（亮 #FFFFFF / 暗 #3A3A3A）与 `miuix_card_stroke`（亮 #14000000 / 暗 #1FFFFFFF），解决暗色下卡片与页面同色（都是 #303030）看不出层次的问题。
  - `app/src/main/res/layout/item_rss.xml`（订阅页来源卡片）：图标 50→56dp、圆角 12→14dp、名称 13sp→14sp 且改用 `primaryText`。
  - `app/src/main/res/drawable/bg_find_book_group.xml`（发现页源列表行）：6dp 灰底 → 14dp 白卡 + 0.5dp 细描边；`item_find_book.xml` 行高 ≥52dp、内边距 16/14dp、标题 15sp 加粗、箭头 20→18dp。
  - 验证（API 37）：订阅页卡片、发现页行在亮色下为白色卡片 + 细边；暗色下卡片 (58,58,58) vs 页面 (48,48,48)，层次清晰；无 FATAL。
- 第 25 批（2026-11-15，阅读器剩余底部弹窗统一圆角）：
  - `BgTextConfigDialog`、`AutoReadDialog` 的 `onStart` 背景由 `@color/background` 改为 `roundedTopDrawable(bottomBackground, 16dp)`，内容根 `rootView/root` 底色改透明，与「界面/更多设置/朗读设置」三个弹窗保持一致。
  - `AutoReadDialog` 补 `import io.legado.app.utils.dpToPx`。
  - 验证：`assembleDebug`/`assembleAppRelease` 通过；release 包冒烟启动无 FATAL；这两个弹窗本次未在模拟器上逐个打开（进入路径较深），仅静态 + 编译验证。
- 第 26 批（2026-11-15，单选框 Miuix 化）：
  - 新增 `app/src/main/res/drawable/bg_miuix_radio.xml`：选中 = 强调色实心圆 + 白色内点，未选中 = 2dp 细环；颜色 `miuix_radio_stroke`（亮 #C0000000 / 暗 #C0FFFFFF）。
  - `app/src/main/java/io/legado/app/lib/theme/view/ThemeRadioButton.kt`：初始化改为 `setButtonDrawable(null)` + `setCompoundDrawablesRelativeWithIntrinsicBounds(bg_miuix_radio)` + `compoundDrawablePadding = 10.dpToPx()`（原来只是 `applyTint(accentColor)`），全工程所有 `ThemeRadioButton` 一次性换成 Miuix 单选框。
  - `dialog_bookshelf_config.xml` 里的原生 `RadioButton` 同样改为 `button=@null` + `drawableStart=@drawable/bg_miuix_radio` + `drawablePadding=10dp` + `minHeight=42dp`。
  - 验证（API 37）：书架布局弹窗的单选组在暗色下为强调色圆点 + 细环，行距正常、文字与圆点有 10dp 间距，无 FATAL。
- 第 27 批（2026-11-15，底栏改为居中紧凑悬浮胶囊）：
  - `activity_main.xml`：底栏容器加 `android:id="@+id/bottom_bar_container"`、左右内边距 12→16dp、底部 10→8dp；`ThemeBottomNavigationVIew` 宽度 `match_parent` → `304dp` 且 `layout_gravity="center_horizontal"`（4 个入口各 76dp），成为居中悬浮胶囊。
  - `MainActivity.initView()`：导航栏 inset 从底栏内部移到容器外部（`bottomBarContainer.bottomPadding = 8.dpToPx() + navigationBarHeight`），卡片不再压在手势导航条上；IME 逻辑保持不变。
  - `ThemeBottomNavigationVIew`：悬浮背景改为 `doOnAttach` 里用 `GradientDrawable` 动态生成，圆角读设备屏幕圆角（`WindowInsets.getRoundedCorner`，夹 24~40dp，取不到回落 28dp）。
  - 验证（API 37）：亮/暗两套主题下胶囊居中、宽度 304dp，与导航条之间留出 8dp + 导航栏高度，圆角/投影正常；无 FATAL。








  - 新增 `app/src/main/res/layout/view_preference_widget_miuix_switch.xml`：widget 用 `io.legado.app.lib.theme.view.ThemeSwitch`（id 保持 `switchWidget`）。
  - `app/src/main/java/io/legado/app/lib/prefs/SwitchPreference.kt`：`widgetLayoutResource` 指向新 layout；`onBindViewHolder` 改为 `bindView<CompoundButton>` 后手动同步 `isChecked`/`isEnabled`，并给开关设点击回调（toggle preference）。`SwitchPreferenceCompat` 内部只认 `SwitchCompat`，因此同步与点击由我们接管。
  - `ThemeSwitch.init` 补 `setButtonDrawable(null)`，去掉 `CompoundButton` 自带的方块。
- 验证（API 37，`assembleDebug` 通过）：「更多设置」三个开关渲染为 Miuix 胶囊（关态 30% 灰轨、开态 `primaryColor` 棕色 #795548）、无多余方块；点击开关切换到开态、关闭弹窗重开仍保持（持久化正常）；logcat 无 `FATAL EXCEPTION`。

- 尚未 opt-in（2026-11-15 剩余 7 个 layout / 1 个 activity）：
  - 阶段 2：`fragment_my_config.xml`、`fragment_web_view_login.xml`（主界面 tab 页）、`dialog_source_picker.xml`（对话框内 Toolbar）
  - 暂缓（高风险）：`view_read_menu.xml`、`view_manga_menu.xml`（阅读器菜单）、`activity_code_edit.xml`（代码编辑器）、`activity_rss_read.xml`、`activity_web_view.xml`（WebView）、`activity_video_player.xml`（视频）、`activity_audio_play.xml`、`activity_book_info.xml`（两者 `app:themeMode="dark"` 且代码里 `setBackgroundResource(R.color.transparent)`，需先做深色沉浸页适配）
  - 有菜单但暂未换 Compose 顶栏：`activity_dict_rule.xml`（动态菜单：`invalidateOptionsMenu` + `onPrepareOptionsMenu`）、`activity_rss_artivles.xml`（`menu/rss_articles` 的 `menu_search` 是 `actionViewClass=SearchView`）、`activity_config.xml`（`menu/search_view` 同上）、`activity_explore_show.xml`（与 `SearchActivity` 共用布局，代码引用 `titleBar`）
  - 已无 activity 使用 `view_search_miuix`（`view_search_miuix.xml` 现仅作为 `view_search` 的 MiuiX 变体保留）
  - 已知坑 14：`activity_txt_toc_rule.xml`（页面自带主题色搜索栏，先不 opt-in）

- `ui/widget/TitleBar.kt`：读 `miuixTopBar`；背景分支插在“透明/E-Ink”之后、原主色分支之前 → `miuix_item_surface` 且**不设 elevation**；`usesMiuixForeground = miuixTopBar && !isEInkMode`，走新增的 `applyMiuixForegroundColor()`：按自身底色算 `isLightBar` → 标题/子标题/导航图标/overflow/menu 里 SearchView/`R.id.search_view`/TabLayout 全部按实际背景上色，并调 `activity?.setLightStatusBar(isLightBar)`；未改 `setColorFilter`、`alpha<255 → elevation=0.1f` 横屏 hack、inset、`attachToActivity`
- `view_title_bar.xml` / `view_title_bar_dark.xml`：`minHeight=56dp`、`contentInsetStart/End=16dp`
- 标题**仍 20sp**（`ToolbarTitle`），没改 24sp：Miuix 0.9.3 `title2`=24sp 会让 48 个页面中文长标题截断，留到阶段 2
- 新增 `AppTheme.MiuixBarOverlay`（`values`/`values-night` 两套）+ `view_search_miuix.xml`（36dp 高、18dp 圆角、`miuix_search_fill` 填充）、`view_tab_layout_miuix.xml`
- **删除** `app/src/main/res/layout/view_tab_layout.xml`：它原本只被 `activity_chapter_list.xml` 引用，改用 miuix 版后变成死资源（lint `UnusedResources` 会多 1 条）；如需给非 opt-in 页面复用，从 `view_tab_layout_miuix.xml` 去掉 `android:theme` 重建即可
- 搜索框：`bg_searchview` 原本 radius 35dp **已经是胶囊**，阶段 1 只调了高度/底色/去描边

### 底栏（全局生效，`MainActivity` 唯一使用处）
- `activity_main.xml`：`minHeight` 50→64dp、`labelVisibilityMode` unlabeled→**labeled**、`itemTextAppearance*`=`@style/MiuixBottomNavText`（12sp）、`itemActiveIndicatorStyle`=`@style/MiuixBottomNavIndicator`
- `ThemeBottomNavigationVIew.kt`：底色 `miuix_item_surface`、`elevation=0`；新增 `applyMiuixActiveIndicator()`：胶囊色=`blendColors(底栏底色, 主题强调色, 0.14f)`、圆角 16dp、高 32dp；`miuixIndicatorAllowed = !isEInkMode && !transparentNavBar`，且 `applySkin(iconMap)` 有自定义图集时关闭胶囊（PNG 皮肤与胶囊冲突）
- 保留：`applySkin` 图集、`addBadgeView`、`bottomMenuCount`、ViewPager 联动、E-Ink/透明底栏原有分支

16. **把 `TitleBar` 换成 `MiuixTopBarView` 时 id 不能沿用 `@+id/title_bar`**（`BaseActivity.onCreate` 会 `findViewById<TitleBar>` 强转，直接 `ClassCastException`）；统一用 `@+id/miuix_top_bar`。
17. **`MenuItem.showAsAction` 从 XML 里读必须用 `getAttributeIntValue()`**；`getAttributeValue()` 返回 `"0x2"` 这类字符串，跟 `"always"` 比较会全部落到 never。
18. **`showAsAction=always` 但没有图标的项**（如二维码页「Gallery」）要判 `iconRes == 0` 后放进溢出菜单，并且溢出按钮的显示条件也要把这类项算进去，否则该项和 ⋮ 都会消失。
19. **搜索页换 `MiuixSearchBar` 时，`searchView` 的声明类型必须一起改**：`findViewById(R.id.search_view)` 会按声明类型做泛型推断，保留 `SearchView` 会在运行期强转崩溃；改成 `MiuixSearchBar` 后，`SearchView.OnQueryTextListener` 等监听器接口仍可直接复用。

20. **Compose 溢出菜单不要再用自解析 XML 的扁平列表**：子菜单会被拍平成顶层项，且 `MenuItemCompat.getShowAsAction/requiresActionButton` 在本项目的 appcompat 版本里不可用；正确做法是 `MenuInflater.inflate(mtbMenu, PopupMenu(context,this).menu)` 拿真实 `Menu`，`showAsAction` 仍从 XML 里读（`getAttributeIntValue`），子菜单用 `item.hasSubMenu()`/`item.subMenu` 渲染。

21. **Compose 顶栏的页面不会自动执行 `onCompatCreateOptionsMenu`**：AppCompat 的 options menu 只有在存在 Toolbar/ActionBar 时才真正创建，`invalidateOptionsMenu()` 不会触发 `onCreateOptionsMenu`；`BaseActivity.onCreate` 里对含 `miuix_top_bar` 的页面用 `PopupMenu(this, View(this)).menu` 主动调一次 `onCompatCreateOptionsMenu(scratch)`，页面里「给搜索框设监听器 / 设标题」这类初始化才会生效。
22. **`MenuItem` 的 actionView 不能在 Compose 顶栏里复用**：`MenuItemCompat.getShowAsAction/requiresActionButton` 在项目当前 appcompat 版本不可用，`actionViewClass` 项改为跳过渲染，由 `MiuixTopBarView.searchBar` 承担；页面把 `(menu.findItem(...).actionView as SearchView)` 换成 `binding.miuixTopBar.searchBar`。

### 验证情况（模拟器 emulator-5554，1080x2400）
- 亮色：顶栏白底深图标 + 搜索胶囊；底栏白底 + 红色胶囊 + 12sp 标签 ✓
- 暗色：顶栏 #303030 浅色图标，底栏深底 + 暗红胶囊 ✓
- 横屏：单行 56dp、搜索框拉伸无截断 ✓
- 130% 字体：底栏标签放大不裁切 ✓（顶栏试点页无标题文字，无截断风险）
- E-Ink 模式（主题模式→E-Ink，截图 185）：顶栏走 `bg_eink_border_bottom`、底栏走 `bg_eink_border_top`，**胶囊被关闭**、选中图标纯黑加粗 ✓；注意底栏现在带 12sp 文字标签（以前是 unlabeled），属本次有意变更
- 未 opt-in 的书架页顶栏/底栏均保持旧样式（零回归）✓
- 状态栏图标：修复前白压白不可见 → 修复后亮色深图标、暗色浅图标 ✓（`TitleBar.kt` + `setLightStatusBar`）
- **旋转/配置变化后状态栏图标回归（已修）**：`BaseActivity.setupSystemBar()` 会在 `onConfigurationChanged` / `onMultiWindowModeChanged` 里重设图标色，覆盖 miuix 顶栏的判断。修法：`TitleBar.refreshMiuixForeground()`（仅 `usesMiuixForeground` 时执行）+ `setupSystemBar()` 末尾对 `R.id.title_bar` 与 `R.id.titleBar` 各调用一次 → 系统栏重建后由 miuix 顶栏重新接管（符合 `ANDROID_PLATFORM_API_GUIDELINES.md` §4.8「一处 Window 行为只能有一个控制点」）。实测：书源页横屏亮色 = 深色图标（190→191）、暗色横屏 = 白色图标（192）、替换净化横屏（196）、导入书籍横屏（198）✓
- 第 2 批 opt-in 验证：自动任务亮色（193）/ 暗色（194）、替换净化亮色（195）/ 横屏（196）、导入书籍（`id=titleBar`）亮色（197e）/ 横屏（198）—— 顶栏白底、深图标、搜索胶囊、状态栏深图标均正常 ✓；规则订阅（`activity_rss_source.xml`）与阅读记录（`activity_read_record.xml`）只做了代码改动与构建，未逐页人工截图
- 第 3 批 opt-in 验证：搜书页（200，输入框自动聚焦弹键盘，顶栏白底深图标 ✓）、文件管理（201，`id=titleBar`，面包屑行正常 ✓）、阅读记录（202 ✓）；整理书架（`activity_arrange_book.xml`）、正文搜索（`activity_search_content.xml`）、两个调试页（`activity_source_debug.xml` / `activity_rss_source_debug.xml`）只做了构建，未逐页人工截图
- 第 4 批 opt-in 验证：书签页（204，只有标题的页面 → 白底深色标题 + 状态栏深图标）✓；缓存（`activity_cache_book.xml`）、底栏皮肤（`activity_bottom_bar_skin.xml`）、配置文件（`activity_config.xml`）、捐赠（`activity_donate.xml`）只做了构建，未逐页人工截图
- 第 6 批 opt-in 验证（2026-11-15，Linux 模拟器 API 37，1080x2400）：书源编辑（亮色 / 暗色 #303030 / 横屏，状态栏深/浅图标均正确）✓、自动任务编辑（亮色，Tab 条正常）✓、替换规则编辑（亮色，长标题不截断）✓、书架零回归基线（顶栏仍为主色旧样式）✓；`activity_auto_task_debug`、`activity_book_info_edit`、`activity_explore_show`、`activity_qrcode_capture`、`activity_rss_artivles`、`activity_rss_source_edit` 只做了构建 + 静态检查，未逐页截图（explore / rss_artivles 的动态 Tab 条与 auto_task_edit 同构）
- 第 7 批 / E2 试点验证（2026-11-15，Linux 模拟器 API 37，1080x2400）：书架顶栏白底 + tab 深色文字 + 红色 indicator（零回归基线从“旧棕色”改为“新白顶栏”，底栏胶囊正常）✓、RSS 首页（miuix 搜索胶囊 + 时钟/星标/扫码/设置深色图标）✓、发现（搜索胶囊 + 深色图标）✓；Compose 顶栏试点页 `AutoTaskDebugActivity`：亮色白底深图标 / 暗色 #303030 浅色图标 / 横屏单行 56dp、状态栏图标深浅正确、返回箭头可用、无重复 insets 留白、任务可正常跑完 ✓；`cmd uimode night` 切换后 Compose 顶栏与状态栏图标同步刷新 ✓
- 第 8 批 / E2 第 2 批验证（2026-11-15，Linux 模拟器 API 37，1080x2400）：书源编辑页 Compose 顶栏 = 返回 + 标题 + 代码/保存/虫子三个图标动作 + 溢出菜单；溢出 11 项（「登录」按 `onMenuOpened` 正确隐藏）、点「帮助」弹出规则帮助、点「保存」触发 `name and URL cannot be empty` toast、点代码图标触发页面原生「先把光标放进文本框」提示 ✓；自动任务编辑页同样正常，点保存成功返回列表 ✓；两页亮色/暗色（#303030 + 浅色图标）/横屏（标题不截断、无重复 insets）✓；无崩溃
- `transparentNavBar`（主题设置 → immersion navigation bar）本次仍**未验证**：该页是已迁移的 Miuix Compose 页面，用 `adb input tap` 点两处坐标都没能切换开关（怀疑是 Miuix Switch 的可点区域/合成点击问题，与本批改动无关）
- lint：阶段 1 改完后 `:app:lintAppDebug` = **9 errors / 1062 warnings**，与基线完全一致；E2 第 1 批后复跑 = **9 errors / 1076 warnings**（error 未变；`MiuixTopBarView` 的 `Recycle` 告警已用 `TypedArray.use` 消除，改动文件无新增告警；阶段 1 的 `view_tab_layout.xml` UnusedResources 早已通过删除该死资源抵消，`TitleBar.kt:82 UseKtx` 也是改动前就有的）
- 未验证：`transparentNavBar` 模式、自定义底栏图集皮肤（需要素材文件）、角标 badge
- 测试数据已清理：书架 `test_book`、`/sdcard/Download/Books/`、`/sdcard/Download/test_book.txt`、`build/topbar-verify/`、临时 `ui-*.xml`（E2 试点在模拟器里留了一条调试用自动任务 `e2test`，仅模拟器数据，不影响仓库）

### 合规自查（`ANDROID_PLATFORM_API_GUIDELINES.md` §11 要求的报告）
- 用到的 Android / AndroidX API：`WindowInsetsCompat`（原有 `setOnApplyWindowInsetsListenerCompat`，未改）、`WindowInsetsControllerCompat`（经项目封装 `ActivityExtensions.kt:113 setLightStatusBar`，API≥R / API≥M 双分支）、`View.setBackgroundColor` / `PorterDuffColorFilter`（图标着色）、`WindowInsets`（原逻辑）。
- 用到的 Miuix 组件：阶段 1 仅有色值（`miuix_item_surface`）与设计语言；E2 试点开始使用 Compose 组件 `SmallTopAppBar`、`IconButton`、`Icon`（+ `LegadoMiuixTheme`），`ImageVector.vectorResource` 载入 `ic_arrow_back`。
- 自定义替代实现：无（未用 Miuix 替代任何系统能力；底栏使用项目既有 AndroidX Material `NavigationBarView` 的 active indicator API）。
- 最低 API：全部为 API 23 可用；无 Hidden API、无反射、无小米私有能力；minSdk 未改。
- 实际验证：`:app:assembleDebug` 通过（BUILD SUCCESSFUL）；`:app:lintAppDebug` 9 errors / 1062 warnings = 基线；模拟器 API 37（Medium_Phone_API_37.0）亮/暗/横屏/130% 字体/E-Ink 实测。
- 未验证（如实声明）：API 23/29/33/35 真机或模拟器未跑；HyperOS 设备未测；分屏/多窗口、`transparentNavBar`、自定义底栏图集皮肤、badge 未验证；键盘、TalkBack 未针对顶栏/底栏回归。

- 第 9 批 / E2 第 3 批验证（2026-11-15，Linux 模拟器 API 37）：替换规则编辑页 Compose 顶栏 = 返回 + 标题 + 代码/保存图标 + 溢出（Copy Rule / Paste Rule），亮色/暗色（#303030）✓；二维码页 Compose 顶栏溢出只剩「Gallery」（无图标 always 项修复后），点它正确打开应用内文件选择 `HandleFileActivity` ✓；书源管理页（仍 View 版 opt-in 顶栏）无回归 ✓；`assembleDebug` 通过、logcat 无 FATAL。其余 6 页（书签、书籍详情编辑、底栏皮肤、底栏皮肤分配、缓存、高亮规则、RSS 收藏）只做构建 + 静态检查，未逐页点开

- 第 10 批 / E2 第 4 批验证（2026-11-15，Linux 模拟器 API 37）：书源管理页 = Compose 顶栏 + Compose 胶囊搜索框（放大镜 + 占位文案 + 光标），亮色/暗色（#303030 底 + 12% 白填充）正常；自动任务页输入 `zzz` 列表变「No automatic tasks」、改成 `e2` 又恢复 `e2test`，证明 `onQueryTextChange` 链路通；输入/键盘弹出无异常、logcat 无 FATAL；`assembleDebug` 通过。其余 8 页（书籍搜索、书架整理、文件管理、导入书籍、阅读记录、RSS 源、正文搜索、书源调试）只做构建 + 静态检查

- 第 11 批 / E2 第 5 批验证（2026-11-15，Linux 模拟器 API 37）：替换净化页 = Compose 顶栏 + Compose 搜索框 + 分组图标（点开二级菜单：Group management/Enabled/Disabled/Ungrouped）+ ⋮（6 项，不再出现被拍平的分组子项）；点「Select replacements」切换后重开菜单出现 `✓`，证明 `invalidateOptionsMenu() -> refreshMenu() -> onPrepareOptionsMenu` 链路生效；词典规则页菜单（Import local/online/QR、Import default rules、Help）+ 内联 `+` 正常；`assembleDebug` 通过，logcat 无 FATAL。`activity_rss_source_debug` 只做构建 + 静态检查（入口需在 RSS 源编辑页点调试）。

- 第 12 批 / E2 第 6 批验证（2026-11-15，Linux 模拟器 API 37）：`Other settings`（`ConfigActivity`）= Compose 顶栏 + Compose 搜索框，标题由 `setTitle(resId)` 正确显示；输入 `auto` 后按硬件 Enter 弹出 Search 结果列表（Auto-refresh / Auto jump to Recent list / Clear expired search histories automatically），说明 `searchBar.setOnQueryTextListener` + `onQueryTextSubmit` 链路通；`assembleDebug` 通过，logcat 无 FATAL。`activity_rss_artivles`（`RssSortActivity`）只做构建 + 静态检查（需要 RSS 源带 `searchUrl` 才能看到搜索行）。

- 第 13 批 / E2 第 7 批验证（2026-11-15，Linux 模拟器 API 37）：`Rule Sub`（`RuleSubActivity`）= Compose 顶栏 + 内联「+」，点「+」弹出新建 Rule Subscription 对话框 ✓；`Edit RSS source`（`RssSourceEditActivity`）= Compose 顶栏 + 3 个内联图标（代码/保存/虫子）+ ⋮，溢出菜单 11 项齐全，页面 tab 行（BASIC/启动/LIST/WEB_VIEW）照常显示；`assembleDebug` 通过，logcat 无 FATAL。`activity_explore_show` 只做构建 + 静态检查（Discovery 分类为空，无法进入）。

- 第 14 批 / E2 第 8 批验证（2026-11-15，Linux 模拟器 API 37）：修完后 `assembleDebug` 通过、`SearchActivity`（书架搜索）与 `ImportBookActivity`（导入书籍）均能正常打开、logcat 无 FATAL；`Book sources` 页在 **暗色 + 系统字体 130%** 下顶栏/搜索胶囊/底部选择栏均正常、无截断。lint 复跑 = **9 errors / 1081 warnings**（error 与基线一致，warning 比基线 1062 多 19，主要来自新 Compose 组件与未使用的 `view_tab_layout_miuix` 等资源）。确认 `miuix-ui-android-0.9.3.aar` 的 `uses-sdk android:minSdkVersion="23"`，未抬高 minSdk。目录页（`TocActivity`）本轮已实测：用 `adb` 推一个 txt 到 `/sdcard/Download/Books` 再走「导入书籍 → Manual Input 填目录 → 加入书架 → 打开 → 目录」，确认 Compose 顶栏 + 独立 TabLayout 行（CHAPTERS/BOOKMARKS/HIGHLIGHTS）显示正常，⋮ 菜单在 CHAPTERS 页显示 7 项（含 3 个 ✓）、切到 BOOKMARKS 页后 `onMenuOpened` 分组显隐生效（只剩 Export/Export(MD)/Log）。

## 下一步：阶段 2（进行中，顶栏试点已落地）
- 顶栏换 Miuix `SmallTopAppBar`（ComposeView 嵌入），重做 menu / SearchView / insets；标题改用 `MiuixTheme.textStyles.title2`
- 底栏换 Miuix `NavigationBar`，与 ViewPager 双向同步；注意 `NavigationBarItem` 只收 `ImageVector`，**自定义图集皮肤（PNG StateListDrawable）无法直接塞入**，需自定义 item 或位图承载

## 阶段 2 可行性评估（进行中）
- **顶栏换 Compose `SmallTopAppBar` 不划算做全量**：`TitleBar` 的 `up` 导航、menu、溢出菜单、`SearchView`（含 actionView 上的 `SearchView`/`EditText`）全部建立在 AppCompat `Toolbar` 上，`BaseActivity` 还依赖 `setSupportActionBar` + `onCreateOptionsMenu` + `menu.applyTint` + `installActivityOverflowMenu`。替换等于把菜单/搜索整套重写成 Compose，属高风险重构
  - 可做的局部试点：只对「标题 + 返回」、无 menu 的页面用 `ComposeView` + `SmallTopAppBar`，Toolbar 路径留给有 menu/search 的页面
  - **已落地**：`ui/compose/components/MiuixTopBarView.kt`（FrameLayout + `ComposeView`，`DisposeOnViewTreeLifecycleDestroyed`；XML 属性 `mtbTitle`/`mtbSubtitle`/`mtbShowUp`；API `setTitle`/`setSubtitle`/`setShowUp`/`setOnUpClickListener`）。底色链与 `TitleBar` 对齐（E-Ink → `R.color.background`、`transparentNavBar` → 透明、否则 `miuix_item_surface`），前景按实际底色推导，`SideEffect` 里 `setLightStatusBar`，并读 `LocalConfiguration` 让旋转/夜间切换重组。试点页 `activity_auto_task_debug.xml` 用它替换了 `TitleBar`（id 改为 `miuix_top_bar`，见已知坑 16）。第 8 批起支持 `app:mtbMenu`：`showAsAction=always` 且带图标的项渲染成顶栏图标（Drawable→Bitmap→`Image` + tint），其余进 Compose 溢出菜单（`Popup` + `LegadoMiuixTheme` surface），点击统一派发回 `onCompatOptionsItemSelected`，页面菜单逻辑零复制（见已知坑 17–19）
- **底栏换 Miuix `NavigationBar` 有硬阻塞**：`NavigationBarItem` 只收 `ImageVector`，而 `BottomBarSkinManager` 的皮肤是 PNG `StateListDrawable`（30dp 图标）；`addBadgeView` 依赖 `getChildAt(0)` 的 menuView 结构；ViewPager 联动靠 `menu[i].isChecked`
  - 需要：位图 → ImageVector 桥或自绘 item、自建 badge 容器、自管 ViewPager 同步。中高风险，且会让现有皮肤功能降级
- 建议：底栏保持现有 Material `BottomNavigationView`（外观已 Miuix 化），顶栏只做局部 Compose 试点

## 执行顺序（新会话照着做）

1. 先读 `app/src/main/java/io/legado/app/ui/widget/TitleBar.kt` 全文，确认现有 `fitStatusBar` / menu / SearchView / E-Ink 分支的位置，再动手
2. 读 `MainActivity` 的布局与 `BottomBarSkinManager`，确认底栏的皮肤与图集入口
3. 阶段 1 已完成（见上节）。若要铺开 opt-in：给目标页 layout 的 `TitleBar` 加 `app:miuixTopBar="true"`，有 contentLayout 的同时换 `view_search_miuix` / `view_tab_layout_miuix`，每页亮暗 + 旋转自测；注意已知坑 14（页内自带主题色搜索栏的页面先跳过）
4. 阶段 2 按上面的可行性评估执行，不要无脑替换

常用命令：

```powershell
# 编译
$env:JAVA_HOME='C:\Users\asahi-ms\.jdks\jbr-21.0.11'; $env:GRADLE_USER_HOME='D:\gradle-home'
cd D:\program\legado-master\legado-master
.\gradlew.bat :app:assembleDebug --console=plain

# 安装并重启（模拟器）
$adb='C:\Users\asahi-ms\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r app\build\outputs\apk\app\debug\app-app-debug.apk
& $adb shell am force-stop com.legado.app.debug
& $adb shell monkey -p com.legado.app.debug -c android.intent.category.LAUNCHER 1

# 暗色截图前务必确认主题模式为「跟随系统」
& $adb shell cmd uimode night yes
```

## 未完成
- 底栏：`transparentNavBar` 模式、自定义图集皮肤、角标 badge 尚未验证（需要主题开关/皮肤素材/未读角标场景）
- 顶栏：试点页 130% 字体未取到干净截图（试点两页无标题文字，风险低）；换 miuix-icons 与 title2 留给阶段 2
- 设备覆盖：只跑过模拟器 API 37，API 23/29/33/35 与 HyperOS 真机未验证；分屏/多窗口未验证
- 自动任务页暗色截图（已完成：194）；正文搜索页未用真实数据验证；表单页待决策
- 顶栏 opt-in 第 2/3 批里 `activity_rss_source.xml`、`activity_arrange_book.xml`、`activity_search_content.xml` 与两个调试页未逐页人工截图验证
- 阅读记录里还有旧测试数据（`test_book`、`长夜行`），需要时在阅读记录页逐条删除

## 截图
`D:\program\legado-master\legado-verify-screenshots\`（01–202；顶栏/底栏阶段 1 = 167–202）
- 167/171 书源页亮色（167 是状态栏修复前，171 是修复后）、168 暗色、173 暗色横屏
- 169 未 opt-in 书架页（零回归基线）
- 170/181 目录页亮色、182 目录页暗色
- 175/176/183 底栏亮色（175 无胶囊、176 有胶囊、183 最终版）、177 底栏暗色、178 暗色滑动联动、179 底栏 130% 字体
- 184/185 E-Ink 模式（我的页/书架页底栏）、186 测试数据清理后的空书架
- 187/187b 旧的模拟器异常（Waiting For Debugger / 启动超时，环境问题）；189 冷启动后首页、190 书源页竖屏（修复双控制点后）、191 书源页横屏亮色、192 书源页横屏暗色 —— 191/192 用于确认旋转后状态栏图标不再回退
- 193/194 自动任务页亮/暗、195/196 替换净化页亮/横屏、197e/198 导入书籍页亮/横屏（该页 TitleBar 的 id 是 `titleBar`，用来验证第二个 id 的刷新）
- 199 RSS tab、200 搜书页、201 文件管理页（`id=titleBar`）、202 阅读记录页 —— 第 3 批 opt-in 验证；204 书签页 —— 第 4 批 opt-in 验证；205 字典规则页 —— 第 5 批 opt-in 验证；206 txt 目录规则页（opt-in 后与自带棕色搜索栏割裂，已回滚）；207 回滚后状态
