package io.legado.app.ui.config.theme

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * 主题设置页的界面模型。
 *
 * 只描述"这一行长什么样"，不持有业务逻辑；读写偏好、应用主题等行为都在
 * [ThemeSettingsActions] 里，保证 Compose 层不直接碰偏好与主题系统。
 */
sealed interface ThemeSettingEntry {

    data class Section(@StringRes val titleRes: Int) : ThemeSettingEntry

    data class Item(
        val key: String,
        @StringRes val titleRes: Int,
        val summary: String? = null,
        @DrawableRes val iconRes: Int? = null,
        val kind: Kind = Kind.Action,
        val checked: Boolean = false,
        val value: String? = null,
        @ColorInt val color: Int? = null,
        val enabled: Boolean = true,
    ) : ThemeSettingEntry

    enum class Kind { Action, Switch, List, Color }
}
