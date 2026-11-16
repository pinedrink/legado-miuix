package io.legado.app.ui.compose.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.legado.app.R
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.ColorUtils
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * Legado 主题到 Miuix 主题的唯一桥接入口，所有 Compose 页面都必须通过它进入 Miuix。
 *
 * 亮暗判定以 Legado 的用户配置为准（[ThemeConfig.isDarkTheme]），而不是直接跟随系统夜间模式，
 * 这样“跟随系统 / 强制亮色 / 强制暗色 / E-Ink”四种模式与现有 View 页面保持一致。
 *
 * 颜色映射（取自 [ThemeStore] 与 Legado 的 `@color/background_card`，用户自定义主题颜色会同步生效）：
 * - 主色 -> primary
 * - 页面背景 -> background / surface（Miuix 的 Scaffold 用 surface 作底色）
 * - 卡片底色 -> surfaceContainer / surfaceContainerHigh / surfaceContainerHighest，沿用 Miuix 的层级色
 *   （Legado 自己的 background_card 与页面底色几乎同色，直接映射会把卡片和页面糊成一个平面）
 * - 文字色按背景/卡片的实际明暗推导（保证对比度），而不是直接取主题里的 textColorPrimary，
 *   因为 Compose 页面的背景可能被用户自定义成与主题文字色不匹配的组合。
 *
 * 已知限制：
 * - 壁纸背景由 `LegadoMiuixScaffold` 把容器设为透明来兼容，卡片仍是半透明之外的不透明底色。
 * - 禁用态、半透明容器色仍使用 Miuix 默认值，后续按页面需要再细化。
 * - 若调用方强制切换明暗、而 Legado 当前主题的明暗与之相反（例如验证页预览暗色时 Activity 仍是亮色主题），
 *   则只沿用 Legado 主色，背景与文字色改用 Miuix 自带的明/暗配色，避免出现“暗色主色 + 亮色背景”的错配。
 *
 * @param darkTheme 是否使用暗色配色，默认取 Legado 当前主题。
 * @param useLegadoColors 是否使用 Legado 配色。false 时完全使用 Miuix 官方默认配色，
 *   用于确认页面本身渲染的确实是 Miuix 组件，而不是被 Legado 配色“盖住”。
 */
@Composable
fun LegadoMiuixTheme(
    darkTheme: Boolean = ThemeConfig.isDarkTheme(),
    useLegadoColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(context, darkTheme, useLegadoColors) {
        val mode = if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light
        if (useLegadoColors) {
            ThemeController(
                colorSchemeMode = mode,
                lightColors = legadoColors(context, isDark = false),
                darkColors = legadoColors(context, isDark = true),
            )
        } else {
            ThemeController(colorSchemeMode = mode)
        }
    }
    MiuixTheme(controller = controller, content = content)
}

/**
 * 把 Legado 的主题色映射成 Miuix 的 [Colors]。
 *
 * 只有 Legado 调色板本身的明暗与目标明暗一致时才完整映射：颜色取自当前 Activity 主题，
 * 当调用方强制切换明暗（例如验证页预览暗色而 Activity 仍是亮色主题）时，直接套用会得到
 * “暗色主色 + 亮色背景”这类错配，此时保留 Miuix 自带的明/暗配色，只沿用 Legado 的主色。
 */
private fun legadoColors(context: Context, isDark: Boolean): Colors {
    val primaryInt = ThemeStore.primaryColor(context)
    val primary = Color(primaryInt)
    val onPrimary = if (ColorUtils.isColorLight(primaryInt)) Color.Black else Color.White
    val backgroundInt = ThemeStore.backgroundColor(context)
    val base = if (isDark) darkColorScheme() else lightColorScheme()

    val paletteIsDark = !ColorUtils.isColorLight(backgroundInt)
    if (paletteIsDark != isDark) {
        return base.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryVariant = primary,
        )
    }

    // 卡片底色优先用 Legado 的 background_card（夜间 #303030，与页面 #212121 有明显层次）；
    // 但日间它是 #F5F5F5、页面是 #FAFAFA，两者几乎同色，直接映射会让卡片和页面糊成一个平面，
    // 这时退回到 Miuix 自带的卡片色（亮色为白），保住卡片层级。
    val legadoCardInt = ContextCompat.getColor(context, R.color.background_card)
    val cardBackgroundInt = if (contrastRatio(legadoCardInt, backgroundInt) >= 1.15) {
        legadoCardInt
    } else {
        base.surfaceContainer.toArgb()
    }
    val cardBackground = Color(cardBackgroundInt)
    val background = Color(backgroundInt)
    // 文字色按背景明暗推导：用户可能把背景改成任意颜色，直接套主题里的文字色会出现“深底深字”。
    val onBackground = autoContrastColor(backgroundInt)
    val onCard = autoContrastColor(cardBackgroundInt)
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primary,
        background = background,
        onBackground = onBackground,
        onBackgroundVariant = onBackground.copy(alpha = 0.66f),
        surface = background,
        onSurface = onBackground,
        surfaceVariant = cardBackground,
        onSurfaceSecondary = onCard.copy(alpha = 0.8f),
        onSurfaceVariantSummary = onCard.copy(alpha = 0.6f),
        onSurfaceVariantActions = onCard.copy(alpha = 0.42f),
        surfaceContainer = cardBackground,
        onSurfaceContainer = onCard,
        onSurfaceContainerVariant = onCard.copy(alpha = 0.8f),
        surfaceContainerHigh = cardBackground,
        onSurfaceContainerHigh = onCard,
        surfaceContainerHighest = cardBackground,
        onSurfaceContainerHighest = onCard,
        dividerLine = onCard.copy(alpha = 0.12f),
        outline = onCard.copy(alpha = 0.12f),
    )
}

/**
 * 取与给定底色对比度足够的文字色。
 */
private fun autoContrastColor(backgroundColor: Int): Color =
    if (ColorUtils.isColorLight(backgroundColor)) Color.Black else Color.White
/**
 * WCAG 对比度，用来判断卡片色与页面色是否真的能看出层次。
 */
private fun contrastRatio(colorA: Int, colorB: Int): Double {
    val luminanceA = androidx.core.graphics.ColorUtils.calculateLuminance(colorA)
    val luminanceB = androidx.core.graphics.ColorUtils.calculateLuminance(colorB)
    val lighter = maxOf(luminanceA, luminanceB)
    val darker = minOf(luminanceA, luminanceB)
    return (lighter + 0.05) / (darker + 0.05)
}