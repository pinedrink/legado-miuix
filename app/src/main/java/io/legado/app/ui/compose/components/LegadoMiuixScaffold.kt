package io.legado.app.ui.compose.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.legado.app.help.config.ThemeConfig
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Legado 的 Miuix 页面骨架（阶段 B 公共组件）。
 *
 * 只做两件事：
 * 1. 统一使用 Miuix [Scaffold]，让页面自动获得 TopAppBar/BottomBar 的 Insets 处理；
 * 2. 当用户配置了壁纸背景图时把容器背景设为透明，避免把 Legado 的壁纸主题盖住。
 *
 * 页面的业务内容、对话框和弹窗仍然各自实现，这里不做过度封装。
 *
 * @param containerColor Legacy 未配置壁纸时的容器背景色。
 * @param contentWindowInsets 交给 Scaffold 处理的内容 Insets；嵌在已有 Insets 处理的
 * Fragment 里时可传 `WindowInsets(0, 0, 0, 0)`，避免和宿主重复留白。
 */
@Composable
fun LegadoMiuixScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = MiuixTheme.colorScheme.surface,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
) {
    // 壁纸是否启用与主题档位有关，随 Composition 建立时读取一次即可；
    // 切换主题会重建 Activity，届时会重新读取。
    val hasBackgroundImage = remember { ThemeConfig.hasBackgroundImage() }
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = if (hasBackgroundImage) Color.Transparent else containerColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}
