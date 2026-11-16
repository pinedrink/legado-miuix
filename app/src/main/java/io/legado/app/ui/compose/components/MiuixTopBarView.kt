package io.legado.app.ui.compose.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import android.graphics.drawable.Drawable
import androidx.core.view.MenuItemCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.drawable.toBitmap
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.transparentNavBar
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.setLightStatusBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 阶段 2「顶栏 Compose 化」的容器：用 Miuix `SmallTopAppBar` 替代传统 View 版 `TitleBar`。
 *
 * 覆盖「返回 + 标题（+副标题） + 图标动作 + 溢出菜单」这种形态，菜单数据仍来自 `res/menu`
 * （`app:mtbMenu`），点击派发回 Activity 的 `onCompatOptionsItemSelected()`，避免把菜单逻辑
 * 复制一份到 Compose。带 SearchView / 自定义 contentLayout 的页面仍用 `TitleBar`，见
 * Docs-mem/Miuix迁移交接.md 的「阶段 2 可行性评估」。
 *
 * 与 `TitleBar` 对齐的几点：
 * - 底色：E-Ink 模式 -> 页面底色（近似 `bg_eink_border_bottom`）；透明顶栏模式 -> 透明；
 *   其余 -> `@color/miuix_item_surface`（亮色白、暗色 #303030），与已 opt-in 的 View 顶栏一致。
 * - 前景色按**实际底色**推导，不依赖 `toolBarTheme`（同 TitleBar.applyMiuixForegroundColor）。
 * - 状态栏图标由这里控制；`BaseActivity.setupSystemBar()` 在旋转 / 分屏后会重设，
 *   所以读取 `LocalConfiguration` 让 Composition 跟随配置变化重算一次，避免白图标压白底。
 * - 菜单：`showAsAction="always"` 的带图标项渲染成顶栏图标按钮，其余进 Compose 溢出菜单；
 *   打开溢出菜单前会调用 `Activity.onMenuOpened(Window.FEATURE_OPTIONS_PANEL, menu)`，
 *   让页面原有的 `onMenuOpened`（设置 visible / checked）继续生效。
 * - 顶栏内边距：`SmallTopAppBar` 默认自带 systemBars top inset；Legado 的窗口本身还会
 *   fitsSystemWindows，实际不会重复留白（试点页已在 API 37 模拟器上确认）。
 */
class MiuixTopBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private var titleText by mutableStateOf("")
    private var subtitleText by mutableStateOf("")
    private var showUpArrow by mutableStateOf(true)
    private var upClickListener: (() -> Unit)? = null

    private var menuResId: Int = 0
    private var overflowMenu: Menu? = null
    private var searchEnabled by mutableStateOf(false)
    private var menuVersion by mutableIntStateOf(0)
    private var submenuRoot by mutableStateOf<MenuItem?>(null)
    /** 供页面配置搜索框（hint/监听器），替代原来的 `menu.findItem(...).actionView as SearchView`。 */
    val searchBar = MiuixSearchBar(context).apply { id = R.id.search_view }

    private val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.MiuixTopBarView).use {
            titleText = it.getString(R.styleable.MiuixTopBarView_mtbTitle).orEmpty()
            subtitleText = it.getString(R.styleable.MiuixTopBarView_mtbSubtitle).orEmpty()
            showUpArrow = it.getBoolean(R.styleable.MiuixTopBarView_mtbShowUp, true)
            menuResId = it.getResourceId(R.styleable.MiuixTopBarView_mtbMenu, 0)
            searchEnabled = it.getBoolean(R.styleable.MiuixTopBarView_mtbSearch, false)
        }
        addView(searchBar, LayoutParams(0, 0))
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        composeView.setContent { MiuixTopBarContent() }
    }

    fun setTitle(title: CharSequence?) {
        titleText = title?.toString().orEmpty()
    }

    fun setTitle(resId: Int) {
        titleText = context.getString(resId)
    }

    fun setSubtitle(subtitle: CharSequence) {
        subtitleText = subtitle.toString()
    }

    /** 控制 Compose 搜索行的显示（用于原本靠 `menu_search.isVisible` 控制的页面）。 */
    fun setSearchBarVisible(visible: Boolean) {
        searchEnabled = visible
    }

    fun setShowUp(show: Boolean) {
        showUpArrow = show
    }

    fun setOnUpClickListener(listener: () -> Unit) {
        upClickListener = listener
    }

    /** 页面状态变化（invalidateOptionsMenu）后重建菜单，让 onPrepareOptionsMenu 的可见/选中生效。 */
    fun refreshMenu() {
        menuVersion++
    }

    /** 换菜单资源；传 0 表示不显示任何动作/溢出。 */
    fun setMenu(menuRes: Int) {
        menuResId = menuRes
        menuVersion++
    }

    private fun buildMenu(): Menu {
        loadInlineActionIds()
        val menu = PopupMenu(context, this).menu
        if (menuResId != 0) {
            MenuInflater(context).inflate(menuResId, menu)
        }
        // 让页面原有的 onPrepareOptionsMenu（可见性、checked、动态子菜单）继续生效
        activity()?.onPrepareOptionsMenu(menu)
        return menu
    }

    private fun isInlined(item: MenuItem): Boolean =
        item.itemId in inlineIds && item.icon != null

    private fun isActionView(item: MenuItem): Boolean = item.itemId in actionViewIds

    private fun runtimeInlineItems(menu: Menu): List<MenuItem> =
        (0 until menu.size()).map { menu.getItem(it) }
            .filter { it.isVisible && !isActionView(it) && isInlined(it) }

    private fun runtimeOverflowItems(menu: Menu): List<MenuItem> =
        (0 until menu.size()).map { menu.getItem(it) }
            .filter { it.isVisible && !isActionView(it) && !isInlined(it) }

    private val inlineIds = mutableSetOf<Int>()
    private val actionViewIds = mutableSetOf<Int>()

    private fun loadInlineActionIds() {
        inlineIds.clear()
        actionViewIds.clear()
        if (menuResId == 0) return
        runCatching {
            val parser = context.resources.getXml(menuResId)
            while (parser.eventType != android.content.res.XmlResourceParser.END_DOCUMENT) {
                if (parser.eventType == android.content.res.XmlResourceParser.START_TAG &&
                    parser.name == "item"
                ) {
                    val id = parser.attrResource("id")
                    val always = parser.attrInt("showAsAction") and MenuItem.SHOW_AS_ACTION_ALWAYS != 0
                    if (id != 0 && parser.attrString("actionViewClass") != null) {
                        actionViewIds.add(id)
                    } else if (id != 0 && always) {
                        inlineIds.add(id)
                    }
                }
                parser.next()
            }
        }
    }

    private fun android.content.res.XmlResourceParser.attrIndex(name: String): Int {
        for (i in 0 until attributeCount) {
            if (getAttributeName(i) == name) return i
        }
        return -1
    }

    private fun android.content.res.XmlResourceParser.attrResource(name: String): Int {
        val index = attrIndex(name)
        return if (index < 0) 0 else getAttributeResourceValue(index, 0)
    }

    private fun android.content.res.XmlResourceParser.attrInt(name: String): Int {
        val index = attrIndex(name)
        return if (index < 0) 0 else getAttributeIntValue(index, 0)
    }

    private fun android.content.res.XmlResourceParser.attrString(name: String): String? {
        val index = attrIndex(name)
        return if (index < 0) null else getAttributeValue(index)
    }

    private fun activity(): BaseActivity<*>? = context as? BaseActivity<*>

    private fun dispatchItem(item: MenuItem) {
        // 页面既可能用 onCompatOptionsItemSelected(menuId)，也可能用 setOnMenuItemClickListener；
        // 真实 MenuItem 先走 performClick()，页面没有挂监听时再回落到 Activity 的菜单回调。
        val clicked = runCatching {
            val m = item.javaClass.getDeclaredMethod("performClick")
            m.isAccessible = true
            m.invoke(item) as? Boolean ?: false
        }.getOrDefault(false)
        if (!clicked) activity()?.onCompatOptionsItemSelected(item)
            ?: activity()?.onMenuItemSelected(android.view.Window.FEATURE_OPTIONS_PANEL, item)
    }

    @Composable
    private fun MiuixTopBarContent() {
        // 显式读一次 Configuration：旋转 / 字体缩放 / 夜间切换时让 Composition 重组，
        // 从而重新推导底色与状态栏图标（setupSystemBar 会在这之后写一次，这里是响应式的兜底）。
        @Suppress("UNUSED_VARIABLE")
        val configuration = LocalConfiguration.current
        val hostActivity = LocalView.current.context as? ComponentActivity
        val barColorInt = when {
            AppConfig.isEInkMode -> ContextCompat.getColor(context, R.color.background)
            context.transparentNavBar -> AndroidColor.TRANSPARENT
            else -> ContextCompat.getColor(context, R.color.miuix_item_surface)
        }
        val isLightBar = ColorUtils.isColorLight(barColorInt)
        val foreground = if (isLightBar) Color.Black else Color.White
        SideEffect {
            hostActivity?.setLightStatusBar(isLightBar)
        }
        var barHeightPx by remember { mutableStateOf(0) }
        var showOverflow by remember { mutableStateOf(false) }
        val submenu = submenuRoot?.subMenu
        if (submenu != null) {
            LegadoMiuixTheme {
                OverflowMenu(
                    menu = submenu,
                    offsetY = barHeightPx,
                    onDismiss = { submenuRoot = null },
                    onSelected = { item ->
                        submenuRoot = null
                        dispatchItem(item)
                    },
                )
            }
        }
        val preparedMenu = remember(menuVersion) { buildMenu() }
        if (showOverflow) {
            val overflow = preparedMenu
            remember { activity()?.onMenuOpened(Window.FEATURE_OPTIONS_PANEL, overflow) }
            LegadoMiuixTheme {
                OverflowMenu(
                    menu = overflow,
                    offsetY = barHeightPx,
                    onDismiss = { showOverflow = false },
                    onSelected = { item ->
                        showOverflow = false
                        activity()?.onCompatOptionsItemSelected(item)
                    },
                )
            }
        }
        LegadoMiuixTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (barColorInt == AndroidColor.TRANSPARENT) Color.Transparent
                        else Color(barColorInt)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { barHeightPx = it.height }
                ) {
                    SmallTopAppBar(
                    title = titleText,
                    subtitle = subtitleText,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(barColorInt),
                    titleColor = foreground,
                    navigationIcon = {
                        if (showUpArrow) {
                            IconButton(
                                onClick = {
                                    upClickListener?.invoke()
                                        ?: hostActivity?.onBackPressedDispatcher?.onBackPressed()
                                },
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                                    contentDescription = stringResource(R.string.back),
                                    tint = foreground,
                                )
                            }
                        }
                    },
                    actions = {
                        runtimeInlineItems(preparedMenu).forEach { item ->
                            InlineAction(
                                title = item.title,
                                icon = item.icon,
                                foreground = foreground,
                            ) {
                                if (item.hasSubMenu()) submenuRoot = item else dispatchItem(item)
                            }
                        }
                        if (runtimeOverflowItems(preparedMenu).isNotEmpty()) {
                            IconButton(onClick = { showOverflow = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_more_vert),
                                    contentDescription = stringResource(R.string.more_menu),
                                    tint = foreground,
                                )
                            }
                        }
                    },
                    )
                }
                if (searchEnabled) {
                    SearchRow(foreground = foreground)
                }
            }
        }
    }

    @Composable
    private fun SearchRow(foreground: Color) {
        val fill = Color(ContextCompat.getColor(context, R.color.miuix_search_fill))
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        val localFocus = LocalFocusManager.current
        if (searchBar.focusRequested) {
            LaunchedEffect(searchBar.focusRequested) {
                searchBar.focusRequested = false
                focusRequester.requestFocus()
                keyboard?.show()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                .height(36.dp)
                .background(fill, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp)
                .onFocusChanged { searchBar.onFocusChanged(it.isFocused) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                contentDescription = null,
                tint = foreground.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = searchBar.text,
                    onValueChange = { newText ->
                        searchBar.text = newText
                        searchBar.onTextChanged(newText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                                searchBar.submitQuery()
                                localFocus.clearFocus()
                                keyboard?.hide()
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = TextStyle(color = foreground, fontSize = 14.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(foreground),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        searchBar.submitQuery()
                        localFocus.clearFocus()
                        keyboard?.hide()
                    }),
                )
                if (searchBar.text.isEmpty()) {
                    BasicText(
                        text = (searchBar.hint ?: stringResource(R.string.search)).toString(),
                        style = TextStyle(color = foreground.copy(alpha = 0.6f), fontSize = 14.sp),
                    )
                }
            }
        }
    }

    @Composable
    private fun InlineAction(
        title: CharSequence?,
        icon: Drawable?,
        foreground: Color,
        onClick: () -> Unit,
    ) {
        val bitmap = remember(icon) {
            icon?.toBitmap(72, 72)?.asImageBitmap()
        }
        IconButton(onClick = onClick) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = title?.toString(),
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(foreground),
                )
            } else {
                BasicText(
                    text = title?.toString()?.take(1).orEmpty(),
                    style = TextStyle(color = foreground, fontSize = 16.sp),
                )
            }
        }
    }

    @Composable
    private fun OverflowMenu(
        menu: Menu,
        offsetY: Int,
        onDismiss: () -> Unit,
        onSelected: (MenuItem) -> Unit,
    ) {
        val items = remember(menu) {
            (0 until menu.size()).map { menu.getItem(it) }
                .filter { it.isVisible && !isActionView(it) && !isInlined(it) }
        }
        var expandedIndex by remember { mutableStateOf(-1) }
        val rowHeightPx = with(LocalDensity.current) { 44.dp.toPx() }
        val surface = MiuixTheme.colorScheme.surface
        val onSurface = MiuixTheme.colorScheme.onSurface
        Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(0, offsetY),
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .widthIn(min = 180.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .background(surface, RoundedCornerShape(16.dp))
                    .padding(vertical = 6.dp),
            ) {
                items.forEachIndexed { index, item ->
                    val hasSubMenu = item.hasSubMenu()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable(enabled = item.isEnabled) {
                                if (hasSubMenu) expandedIndex = index else onSelected(item)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.isChecked) {
                            BasicText(
                                text = "✓ ",
                                style = TextStyle(color = onSurface, fontSize = 15.sp),
                            )
                        }
                        BasicText(
                            text = item.title.toString(),
                            style = TextStyle(color = onSurface, fontSize = 15.sp),
                            modifier = Modifier.weight(1f),
                        )
                        if (hasSubMenu) {
                            BasicText(
                                text = "›",
                                style = TextStyle(color = onSurface, fontSize = 18.sp),
                            )
                        }
                    }
                }
            }
        }
        val parent = items.getOrNull(expandedIndex)
        val subMenu = parent?.subMenu
        if (subMenu != null) {
            val subItems = remember(subMenu) {
                (0 until subMenu.size()).map { subMenu.getItem(it) }.filter { it.isVisible }
            }
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, offsetY + ((rowHeightPx * (expandedIndex + 1)).toInt())),
                onDismissRequest = { expandedIndex = -1 },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .widthIn(min = 180.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .background(surface, RoundedCornerShape(16.dp))
                        .padding(vertical = 6.dp),
                ) {
                    subItems.forEach { subItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable(enabled = subItem.isEnabled) {
                                    expandedIndex = -1
                                    onSelected(subItem)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (subItem.isChecked) {
                                BasicText(
                                    text = "✓ ",
                                    style = TextStyle(color = onSurface, fontSize = 15.sp),
                                )
                            }
                            BasicText(
                                text = subItem.title.toString(),
                                style = TextStyle(color = onSurface, fontSize = 15.sp),
                            )
                        }
                    }
                }
            }
        }
    }

}
