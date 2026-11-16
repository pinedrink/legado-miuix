package io.legado.app.ui.config.cover

import androidx.annotation.StringRes

/**
 * 封面设置的列表行模型。
 *
 * 行顺序与原来的 `res/xml/pref_config_cover.xml` 一致，
 * 但当前页面已经改为 Compose 渲染，不再读取该偏好文件。
 */
sealed interface CoverSettingEntry {

    data class Section(@StringRes val titleRes: Int) : CoverSettingEntry

    data class Item(
        val key: String,
        val title: String,
        val summary: String? = null,
        val kind: Kind = Kind.Action,
        val checked: Boolean = false,
        val enabled: Boolean = true,
    ) : CoverSettingEntry

    enum class Kind { Action, Switch }
}
