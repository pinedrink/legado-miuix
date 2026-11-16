package io.legado.app.ui.config

/**
 * 设置页搜索能力。
 *
 * [ConfigActivity] 的搜索菜单原本只支持 [androidx.preference.PreferenceFragmentCompat]。
 * 迁移到 Compose 的设置页实现本接口后，搜索仍然可用：点击搜索结果会回调 [ConfigSearchItem.onClick]，
 * 由页面自己负责滚动到对应位置并执行点击。没有实现的页面搜索无结果属于预期行为。
 */
interface ConfigSearchable {

    fun searchConfig(query: String): List<ConfigSearchItem>

}

/**
 * 一条设置页搜索结果。
 *
 * @param label 展示给用户的条目名称
 * @param onClick 用户选中该结果后的动作（通常是滚动到该行并触发点击）
 */
data class ConfigSearchItem(
    val label: String,
    val onClick: () -> Unit,
) {

    /** 搜索结果是直接塞进 [io.legado.app.lib.dialogs.selector] 的字符串列表，这里只展示名称。 */
    override fun toString(): String = label

}
