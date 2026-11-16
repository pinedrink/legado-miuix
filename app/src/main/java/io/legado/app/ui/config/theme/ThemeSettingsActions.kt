package io.legado.app.ui.config.theme

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import io.legado.app.R
import io.legado.app.base.AppContextWrapper
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.LauncherIconHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.theme.WallpaperTheme
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.putPrefString
import io.legado.app.utils.toastOnUi

/**
 * 主题设置页的数据与行为。
 *
 * 逻辑全部沿用迁移前的 `ThemeConfigFragment`：偏好 key、默认值、主题应用时机、
 * 壁纸取色、启动图标切换都保持原样，只是把"渲染"交给了 Compose。
 */
object ThemeSettingsActions {

    const val KEY_LAUNCHER_ICON = PreferKey.launcherIcon
    const val KEY_WELCOME_STYLE = "welcomeStyle"
    const val KEY_COVER_CONFIG = "coverConfig"
    const val KEY_THEME_LIST = "themeList"
    const val KEY_BOTTOM_BAR_SKIN = PreferKey.bottomBarSkin
    const val KEY_SAVE_DAY_THEME = "saveDayTheme"
    const val KEY_SAVE_NIGHT_THEME = "saveNightTheme"

    /** 这几项改动后需要重建 Activity（与原 onSharedPreferenceChanged 行为一致）。 */
    private val recreateKeys = setOf(
        PreferKey.transparentStatusBar,
        PreferKey.immNavigationBar,
        PreferKey.disablePredictiveBack,
    )

    private val dayColorKeys = setOf(
        PreferKey.cPrimary,
        PreferKey.cAccent,
        PreferKey.cBackground,
        PreferKey.cBBackground,
        PreferKey.tNavBar,
    )

    private val nightColorKeys = setOf(
        PreferKey.cNPrimary,
        PreferKey.cNAccent,
        PreferKey.cNBackground,
        PreferKey.cNBBackground,
        PreferKey.tNavBarN,
    )

    private data class ColorSpec(
        val key: String,
        @StringRes val titleRes: Int,
        @StringRes val summaryRes: Int,
        @ColorRes val defaultColorRes: Int,
    )

    private val dayColors = listOf(
        ColorSpec(PreferKey.cPrimary, R.string.primary, R.string.day_color_primary, R.color.md_brown_500),
        ColorSpec(PreferKey.cAccent, R.string.accent, R.string.day_color_accent, R.color.md_red_600),
        ColorSpec(
            PreferKey.cBackground,
            R.string.background_color,
            R.string.day_background_color,
            R.color.md_grey_100
        ),
        ColorSpec(
            PreferKey.cBBackground,
            R.string.navbar_color,
            R.string.day_navbar_color,
            R.color.md_grey_200
        ),
    )

    private val nightColors = listOf(
        ColorSpec(
            PreferKey.cNPrimary,
            R.string.primary,
            R.string.night_primary,
            R.color.md_blue_grey_600
        ),
        ColorSpec(
            PreferKey.cNAccent,
            R.string.accent,
            R.string.night_accent,
            R.color.md_deep_orange_800
        ),
        ColorSpec(
            PreferKey.cNBackground,
            R.string.background_color,
            R.string.night_background_color,
            R.color.md_grey_900
        ),
        ColorSpec(
            PreferKey.cNBBackground,
            R.string.navbar_color,
            R.string.night_navbar_color,
            R.color.md_grey_850
        ),
    )

    fun isColorKey(key: String): Boolean = key in dayColorKeys || key in nightColorKeys

    fun isNightKey(key: String): Boolean = key in nightColorKeys

    fun colorValue(context: Context, key: String): Int {
        val spec = (dayColors + nightColors).firstOrNull { it.key == key } ?: return 0
        val default = ContextCompat.getColor(context, spec.defaultColorRes)
        return context.getPrefInt(key, default)
    }

    @ColorInt
    fun colorDialogSeed(context: Context, key: String): Int = colorValue(context, key)

    fun entries(context: Context): List<ThemeSettingEntry> {
        val result = mutableListOf<ThemeSettingEntry>()
        val launcherIconValue = context.getPrefString(KEY_LAUNCHER_ICON)
        result += ThemeSettingEntry.Item(
            key = KEY_LAUNCHER_ICON,
            titleRes = R.string.change_icon,
            summary = context.getString(R.string.change_icon_summary),
            kind = ThemeSettingEntry.Kind.List,
            value = launcherIconLabel(context, launcherIconValue),
        )
        result += ThemeSettingEntry.Item(
            key = KEY_WELCOME_STYLE,
            titleRes = R.string.welcome_style,
            summary = context.getString(R.string.welcome_style_summary),
        )
        result += ThemeSettingEntry.Item(
            key = PreferKey.transparentStatusBar,
            titleRes = R.string.immersion_status_bar,
            summary = context.getString(R.string.status_bar_immersion),
            kind = ThemeSettingEntry.Kind.Switch,
            checked = context.getPrefBoolean(PreferKey.transparentStatusBar, true),
        )
        result += ThemeSettingEntry.Item(
            key = PreferKey.immNavigationBar,
            titleRes = R.string.imm_navigation_bar,
            summary = context.getString(R.string.imm_navigation_bar_s),
            kind = ThemeSettingEntry.Kind.Switch,
            checked = context.getPrefBoolean(PreferKey.immNavigationBar, true),
        )
        result += ThemeSettingEntry.Item(
            key = PreferKey.barElevation,
            titleRes = R.string.bar_elevation,
            summary = context.getString(R.string.bar_elevation_s, AppConfig.elevation.toString()),
        )
        result += ThemeSettingEntry.Item(
            key = PreferKey.fontScale,
            titleRes = R.string.font_scale,
            summary = context.getString(
                R.string.font_scale_summary,
                AppContextWrapper.getFontScale(context)
            ),
        )
        result += ThemeSettingEntry.Item(
            key = KEY_COVER_CONFIG,
            titleRes = R.string.cover_config,
            summary = context.getString(R.string.cover_config_summary),
        )
        result += ThemeSettingEntry.Item(
            key = KEY_THEME_LIST,
            titleRes = R.string.theme_list,
            summary = context.getString(R.string.theme_list_summary),
        )
        result += ThemeSettingEntry.Item(
            key = KEY_BOTTOM_BAR_SKIN,
            titleRes = R.string.bottom_bar_skin,
            summary = context.getString(R.string.bottom_bar_skin_summary),
        )
        result += ThemeSettingEntry.Item(
            key = PreferKey.disablePredictiveBack,
            titleRes = R.string.disable_predictive_back,
            summary = context.getString(R.string.disable_predictive_back_summary),
            kind = ThemeSettingEntry.Kind.Switch,
            checked = context.getPrefBoolean(PreferKey.disablePredictiveBack),
        )
        if (WallpaperTheme.isAvailable()) {
            val follow = context.getPrefBoolean(PreferKey.wallpaperColorFollow)
            result += ThemeSettingEntry.Item(
                key = PreferKey.wallpaperColorFollow,
                titleRes = R.string.wallpaper_color_follow,
                summary = context.getString(R.string.wallpaper_color_follow_summary),
                kind = ThemeSettingEntry.Kind.Switch,
                checked = follow,
            )
            result += ThemeSettingEntry.Item(
                key = PreferKey.wallpaperColorAutoUpdate,
                titleRes = R.string.wallpaper_color_auto_update,
                summary = context.getString(R.string.wallpaper_color_auto_update_summary),
                kind = ThemeSettingEntry.Kind.Switch,
                checked = context.getPrefBoolean(PreferKey.wallpaperColorAutoUpdate, true),
                enabled = follow,
            )
        }
        result += ThemeSettingEntry.Section(R.string.day)
        dayColors.forEach { spec ->
            result += ThemeSettingEntry.Item(
                key = spec.key,
                titleRes = spec.titleRes,
                summary = context.getString(spec.summaryRes),
                kind = ThemeSettingEntry.Kind.Color,
                color = colorValue(context, spec.key),
            )
        }
        result += backgroundItem(context, isNight = false)
        result += ThemeSettingEntry.Item(
            key = PreferKey.tNavBar,
            titleRes = R.string.immersion_nav_bar,
            summary = context.getString(R.string.day_nav_bar_immersion),
            kind = ThemeSettingEntry.Kind.Switch,
            checked = context.getPrefBoolean(PreferKey.tNavBar),
        )
        result += ThemeSettingEntry.Item(
            key = KEY_SAVE_DAY_THEME,
            titleRes = R.string.save_theme_config,
            summary = context.getString(R.string.save_day_theme_summary),
        )
        result += ThemeSettingEntry.Section(R.string.night)
        nightColors.forEach { spec ->
            result += ThemeSettingEntry.Item(
                key = spec.key,
                titleRes = spec.titleRes,
                summary = context.getString(spec.summaryRes),
                kind = ThemeSettingEntry.Kind.Color,
                color = colorValue(context, spec.key),
            )
        }
        result += backgroundItem(context, isNight = true)
        result += ThemeSettingEntry.Item(
            key = PreferKey.tNavBarN,
            titleRes = R.string.immersion_nav_bar,
            summary = context.getString(R.string.night_nav_bar_immersion),
            kind = ThemeSettingEntry.Kind.Switch,
            checked = context.getPrefBoolean(PreferKey.tNavBarN),
        )
        result += ThemeSettingEntry.Item(
            key = KEY_SAVE_NIGHT_THEME,
            titleRes = R.string.save_theme_config,
            summary = context.getString(R.string.save_night_theme_summary),
        )
        return result
    }

    private fun backgroundItem(context: Context, isNight: Boolean): ThemeSettingEntry.Item {
        val key = if (isNight) PreferKey.bgImageN else PreferKey.bgImage
        val path = context.getPrefString(key)
        return ThemeSettingEntry.Item(
            key = key,
            titleRes = R.string.background_image,
            summary = if (path.isNullOrBlank()) {
                context.getString(R.string.select_image)
            } else {
                path
            },
        )
    }

    fun backgroundSummary(context: Context, isNight: Boolean): Pair<Int, String?> {
        val key = if (isNight) PreferKey.bgImageN else PreferKey.bgImage
        val path = context.getPrefString(key)
        return R.string.background_image to if (path.isNullOrBlank()) {
            context.getString(R.string.select_image)
        } else {
            path
        }
    }

    fun launcherIconLabel(context: Context, value: String?): String {
        val names = context.resources.getTextArray(R.array.icon_names)
        val values = context.resources.getTextArray(R.array.icons)
        val index = values.indexOfFirst { it.toString() == value }
        return names.getOrNull(if (index >= 0) index else 0)?.toString().orEmpty()
    }

    fun applyLauncherIcon(context: Context, value: String) {
        context.putPrefString(KEY_LAUNCHER_ICON, value)
        LauncherIconHelp.changeIcon(value)
    }

    /** 开关类设置的副作用；返回 true 表示已接受这次修改。 */
    fun onSwitchChanged(context: Context, key: String, checked: Boolean): Boolean {
        when (key) {
            PreferKey.transparentStatusBar,
            PreferKey.immNavigationBar,
            PreferKey.disablePredictiveBack -> {
                context.putPrefBoolean(key, checked)
                if (key in recreateKeys) {
                    postEvent(EventBus.RECREATE, "")
                }
            }

            PreferKey.wallpaperColorFollow -> {
                val autoUpdate = context.getPrefBoolean(PreferKey.wallpaperColorAutoUpdate, true)
                if (!WallpaperTheme.setFollow(context, checked, autoUpdate)) {
                    context.toastOnUi(R.string.wallpaper_colors_unavailable)
                    return false
                }
                context.putPrefBoolean(key, checked)
            }

            PreferKey.wallpaperColorAutoUpdate -> {
                if (context.getPrefBoolean(PreferKey.wallpaperColorFollow)) {
                    if (!WallpaperTheme.setFollow(context, enabled = true, autoUpdate = checked)) {
                        context.toastOnUi(R.string.wallpaper_colors_unavailable)
                        return false
                    }
                }
                context.putPrefBoolean(key, checked)
            }

            PreferKey.tNavBar,
            PreferKey.tNavBarN -> {
                context.putPrefBoolean(key, checked)
                applyThemeFor(key, context)
            }
        }
        return true
    }

    /** 颜色选择；返回 false 表示这次颜色被拒绝（例如日间背景太暗）。 */
    fun onColorPicked(context: Context, key: String, @ColorInt color: Int): Boolean {
        if (key == PreferKey.cBackground && !ColorUtils.isColorLight(color)) {
            context.toastOnUi(R.string.day_background_too_dark)
            return false
        }
        if (key == PreferKey.cNBackground && ColorUtils.isColorLight(color)) {
            context.toastOnUi(R.string.night_background_too_light)
            return false
        }
        context.putPrefInt(key, ColorUtils.withAlpha(color, 1f))
        applyThemeFor(key, context)
        return true
    }

    private fun applyThemeFor(key: String, context: Context) {
        val isNight = isNightKey(key)
        if (!WallpaperTheme.isApplyingColors) {
            if (key != PreferKey.tNavBar && key != PreferKey.tNavBarN) {
                WallpaperTheme.onColorPreferenceChanged(context)
            }
        }
        if (AppConfig.isNightTheme == isNight) {
            ThemeConfig.applyTheme(context)
            postEvent(EventBus.RECREATE, "")
        }
    }
}
