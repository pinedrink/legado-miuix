package io.legado.app.ui.config.welcome

import androidx.annotation.StringRes

/**
 * 欢迎页样式设置的一行。
 *
 * 行顺序与原来的 `res/xml/pref_config_welcome.xml` 一致，迁移到 Compose 后仍按老顺序展示。
 */
sealed interface WelcomeSettingEntry {

    /** 分组标题，例如“白天”“夜间”。 */
    data class Section(@StringRes val titleRes: Int) : WelcomeSettingEntry

    data class Item(
        val key: String,
        val title: String,
        val summary: String? = null,
        val kind: Kind = Kind.Action,
        val checked: Boolean = false,
        val value: Int = 0,
        val valueRange: IntRange = 0..100,
        val enabled: Boolean = true,
    ) : WelcomeSettingEntry

    enum class Kind {
        Action,
        Switch,
        Slider,
    }
}
