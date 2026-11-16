package io.legado.app.ui.config.coverfont

import androidx.annotation.StringRes

/**
 * 默认书名样式设置页的行模型。
 *
 * 顺序与原来的 `res/xml/pref_config_cover_font.xml` 一致，
 * 末尾的 [Kind.Preview] 对应原来自定义的 `CoverPreviewPreference`（两本书的封面预览）。
 */
sealed interface CoverFontSettingEntry {

    data class Item(
        val key: String,
        val title: String,
        val summary: String? = null,
        val kind: Kind = Kind.Action,
        val checked: Boolean = false,
        val enabled: Boolean = true,
    ) : CoverFontSettingEntry

    enum class Kind { Action, Switch, Preview }

    /** 仅用于描述分组的标题，目前该页只有一个分组，保留以与其它设置页模型一致。 */
    data class Section(@StringRes val titleRes: Int) : CoverFontSettingEntry

}
