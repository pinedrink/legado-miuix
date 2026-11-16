package io.legado.app.ui.config.backup

import androidx.annotation.StringRes

/**
 * 「备份与恢复」设置页的一行。
 *
 * 只描述展示需要的信息，点击后做什么仍由 [io.legado.app.ui.config.BackupConfigFragment] 分派，
 * 避免把备份/恢复这种带副作用和协程的流程塞进 Composable 或数据模型里。
 */
sealed interface BackupSettingEntry {

    /** 分组标题 */
    data class Section(@StringRes val titleRes: Int) : BackupSettingEntry

    /** 普通行 */
    data class Item(
        val key: String,
        val title: String,
        val summary: String? = null,
        val kind: Kind = Kind.Action,
        val checked: Boolean = false,
        val enabled: Boolean = true,
    ) : BackupSettingEntry

    enum class Kind {
        /** 点击后执行动作（弹窗、跳转、备份等） */
        Action,

        /** 行右侧是开关 */
        Switch,
    }
}
