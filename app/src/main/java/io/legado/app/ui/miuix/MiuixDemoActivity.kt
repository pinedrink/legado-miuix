package io.legado.app.ui.miuix

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityMiuixDemoBinding
import io.legado.app.help.config.ThemeConfig
import io.legado.app.ui.compose.MiuixComposeActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 阶段 A 的 Miuix 接入验证页（见 AGENTS.md 第 13 节）。
 *
 * 目的：在现有 View/ViewBinding 工程里通过 [androidx.compose.ui.platform.ComposeView]
 * 承载 Miuix Compose UI，验证依赖、主题桥接、系统栏/Insets 和 Composition 生命周期。
 *
 * 说明：
 * - 沿用 [MiuixComposeActivity]（内部仍是 BaseActivity），主题资源、
 *   返回/预测性返回、系统栏 Insets 行为与其它 Compose 页面一致；
 * - 页面不承载业务逻辑，入口仅用于开发期验证；
 * - 开启壁纸时该页不参与，[imageBg] 固定为 false，避免验证配色时被背景图干扰。
 */
class MiuixDemoActivity : MiuixComposeActivity<ActivityMiuixDemoBinding>(
    theme = if (ThemeConfig.isDarkTheme()) Theme.Dark else Theme.Light,
    imageBg = false,
) {

    override val binding by viewBinding(ActivityMiuixDemoBinding::inflate)

    /**
     * 页面内临时覆盖的亮暗状态，null 表示跟随 Legado 主题。
     * 仅用于验证配色是否随主题切换，不写回任何配置。
     */
    private var darkThemeOverride by mutableStateOf<Boolean?>(null)

    /**
     * 是否使用 Legado 配色。默认关闭，让验证页先呈现 Miuix 官方默认外观，
     * 需要验证主题桥接时再切到 Legado 配色。
     */
    private var useLegadoPalette by mutableStateOf(false)

    override fun isMiuixDarkTheme(): Boolean = darkThemeOverride ?: ThemeConfig.isDarkTheme()

    override fun isLegadoPaletteEnabled(): Boolean = useLegadoPalette

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        // 旋转或重建后保持预览用的选择，避免页面重置造成误解。
        darkThemeOverride = when (savedInstanceState?.getInt(KEY_THEME_OVERRIDE)) {
            1 -> false
            2 -> true
            else -> null
        }
        savedInstanceState?.let {
            useLegadoPalette = it.getBoolean(KEY_USE_LEGADO_COLORS, false)
        }
        binding.composeView.setMiuixContent {
            MiuixDemoScreen(
                darkTheme = isMiuixDarkTheme(),
                darkThemeOverride = darkThemeOverride,
                onDarkThemeOverrideChange = { darkThemeOverride = it },
                useLegadoColors = useLegadoPalette,
                onUseLegadoColorsChange = { useLegadoPalette = it },
                onBack = { finish() },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            KEY_THEME_OVERRIDE,
            when (darkThemeOverride) {
                null -> 0
                false -> 1
                true -> 2
            },
        )
        outState.putBoolean(KEY_USE_LEGADO_COLORS, useLegadoPalette)
    }

    private companion object {
        const val KEY_THEME_OVERRIDE = "miuix_demo_theme_override"
        const val KEY_USE_LEGADO_COLORS = "miuix_demo_use_legado_colors"
    }
}
