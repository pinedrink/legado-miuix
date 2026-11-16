package io.legado.app.ui.main.my

import androidx.annotation.StringRes

/**
 * 「我的」页的一行内容：分组标题或具体设置项。
 */
sealed interface MySettingEntry {

    data class Section(@StringRes val titleRes: Int) : MySettingEntry

    data class Item(
        val key: String,
        val title: String,
        val summary: String? = null,
        val iconRes: Int? = null,
        val kind: Kind = Kind.Action,
        val checked: Boolean = false,
        val value: String? = null,
    ) : MySettingEntry

    enum class Kind {
        /** 点击后跳转或执行一次性动作。 */
        Action,

        /** 右侧显示开关。 */
        Switch,

        /** 点击后弹出候选项，右侧显示当前值。 */
        List,
    }
}
