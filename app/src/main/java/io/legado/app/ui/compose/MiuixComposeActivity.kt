package io.legado.app.ui.compose

import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import io.legado.app.base.BaseActivity
import io.legado.app.constant.Theme
import io.legado.app.help.config.ThemeConfig
import io.legado.app.ui.compose.theme.LegadoMiuixTheme

/**
 * 承载 Miuix Compose 页面的 Activity 基类（阶段 B 公共基类）。
 *
 * 与 [BaseActivity] 的关系：
 * - 主题资源、预测性返回、LiveBus 重建等行为完全沿用 [BaseActivity]；
 * - 区别在于内容用 edge-to-edge 呈现，系统栏 Insets 由 Miuix `Scaffold` 消费，
 *   这里只负责窗口边到边和系统栏图标明暗，避免 View 与 Compose 重复消费 Insets；
 * - Compose 内容统一经过 [LegadoMiuixTheme]，页面不再各自创建主题控制器。
 *
 * 注意：如果页面需要壁纸背景，保持 [imageBg] = true，配合
 * [io.legado.app.ui.compose.components.LegadoMiuixScaffold] 的透明容器即可。
 */
abstract class MiuixComposeActivity<VB : ViewBinding>(
    theme: Theme = if (ThemeConfig.isDarkTheme()) Theme.Dark else Theme.Light,
    imageBg: Boolean = true,
    fullScreen: Boolean = true,
) : BaseActivity<VB>(
    fullScreen = fullScreen,
    theme = theme,
    imageBg = imageBg,
) {

    /**
     * 页面实际使用的亮暗。默认跟随 Legado 主题，验证类页面可以覆盖它做临时预览。
     */
    protected open fun isMiuixDarkTheme(): Boolean = ThemeConfig.isDarkTheme()

    /**
     * 是否把 Legado 的用户配色映射进 Miuix 配色，默认开启。
     */
    protected open fun isLegadoPaletteEnabled(): Boolean = true

    /**
     * 把 Compose 内容挂到 [ComposeView] 上。
     *
     * 组合生命周期跟随 Fragment/Activity 的 ViewTree，避免重建后残留旧 Composition。
     */
    protected fun ComposeView.setMiuixContent(content: @Composable () -> Unit) {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val darkTheme = isMiuixDarkTheme()
            val legadoPalette = isLegadoPaletteEnabled()
            LaunchedEffect(darkTheme) {
                applySystemBarAppearance(darkTheme)
            }
            LegadoMiuixTheme(
                darkTheme = darkTheme,
                useLegadoColors = legadoPalette,
                content = content,
            )
        }
    }

    override fun setupSystemBar() {
        // Compose 页面由 Miuix Scaffold 处理 Insets，这里只负责边到边与系统栏图标明暗。
        enableEdgeToEdge()
        applySystemBarAppearance(isMiuixDarkTheme())
    }

    private fun applySystemBarAppearance(darkTheme: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}
