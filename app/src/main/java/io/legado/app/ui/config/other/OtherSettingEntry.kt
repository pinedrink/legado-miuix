package io.legado.app.ui.config.other

/**
 * 其它设置页的一行内容。
 *
 * [Item.key] 与偏好文件的 key 保持一致，方便直接映射到原有的读写逻辑；
 * [Item.title] 是已经取好文案的标题（个别项如 Cronet 是品牌名，没有对应 string 资源）。
 */
sealed interface OtherSettingEntry {

    data class Section(val titleRes: Int) : OtherSettingEntry

    data class Item(
        val key: String,
        val title: String,
        val summary: String? = null,
        val kind: Kind = Kind.Action,
        val checked: Boolean = false,
        /** [Kind.List] 行右侧展示的当前值。 */
        val value: String? = null,
        val enabled: Boolean = true,
    ) : OtherSettingEntry

    enum class Kind {
        Action,
        Switch,
        List,
    }
}
