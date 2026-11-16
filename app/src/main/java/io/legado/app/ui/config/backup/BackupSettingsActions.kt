package io.legado.app.ui.config.backup

import android.content.Context
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString

/**
 * 「备份与恢复」页的行清单、文案与偏好读写。
 *
 * 与迁移前 `BackupConfigFragment` 的行为保持一致：
 * - 行顺序、分组标题来自 `res/xml/pref_config_backup.xml`
 * - WebDAV 地址/账号/密码/子目录改动后会触发 `ConfigViewModel.upWebDavConfig()`
 * - 「自动备份」摘要根据开关与保留天数动态生成
 */
object BackupSettingsActions {

    /** XML 里没有对应 PreferKey 常量的行 */
    const val KEY_WEB_DAV_BACKUP = "web_dav_backup"
    const val KEY_WEB_DAV_RESTORE = "web_dav_restore"
    const val KEY_LAN_BACKUP_TRANSFER = "lan_backup_transfer"
    const val KEY_LOCAL_PASSWORD = "localPassword"

    /** 这些 key 变化后需要刷新 WebDAV 客户端配置 */
    private val webDavConfigKeys = setOf(
        PreferKey.webDavUrl,
        PreferKey.webDavAccount,
        PreferKey.webDavPassword,
        PreferKey.webDavDir,
    )

    enum class TextKind { TEXT, PASSWORD, NUMBER }

    fun entries(context: Context): List<BackupSettingEntry> = buildList {
        add(BackupSettingEntry.Section(R.string.web_dav_set))
        add(
            textItem(
                context,
                PreferKey.webDavUrl,
                R.string.web_dav_url,
                R.string.web_dav_url_s,
            )
        )
        add(
            textItem(
                context,
                PreferKey.webDavAccount,
                R.string.web_dav_account,
                R.string.web_dav_account_s,
            )
        )
        add(
            BackupSettingEntry.Item(
                key = PreferKey.webDavPassword,
                title = context.getString(R.string.web_dav_pw),
                summary = context.getPrefString(PreferKey.webDavPassword)
                    .takeUnless { it.isNullOrEmpty() }
                    ?.let { "*".repeat(it.length) }
                    ?: context.getString(R.string.web_dav_pw_s),
            )
        )
        add(
            BackupSettingEntry.Item(
                key = PreferKey.webDavDir,
                title = context.getString(R.string.sub_dir),
                summary = AppConfig.webDavDir,
            )
        )
        add(
            BackupSettingEntry.Item(
                key = PreferKey.webDavDeviceName,
                title = context.getString(R.string.webdav_device_name),
                summary = AppConfig.webDavDeviceName,
            )
        )
        add(
            BackupSettingEntry.Item(
                key = PreferKey.webDavBackupKeepCount,
                title = context.getString(R.string.webdav_backup_keep_count_t),
                summary = context.getString(R.string.webdav_backup_keep_count_s),
            )
        )
        add(switchItem(context, PreferKey.webDavBookAutoUpload, R.string.webdav_book_auto_upload_t, R.string.webdav_book_auto_upload_s))
        add(switchItem(context, PreferKey.webDavBookAutoRestore, R.string.webdav_book_auto_restore_t, R.string.webdav_book_auto_restore_s))
        add(switchItem(context, PreferKey.syncBookProgress, R.string.sync_book_progress_t, R.string.sync_book_progress_s))
        add(
            switchItem(
                context,
                PreferKey.syncBookProgressPlus,
                R.string.sync_book_progress_plus_t,
                R.string.sync_book_progress_plus_s,
                enabled = AppConfig.syncBookProgress,
            )
        )

        add(BackupSettingEntry.Section(R.string.backup_restore))
        add(
            BackupSettingEntry.Item(
                key = KEY_LOCAL_PASSWORD,
                title = context.getString(R.string.set_local_password),
                summary = context.getString(R.string.set_local_password_summary),
            )
        )
        add(
            BackupSettingEntry.Item(
                key = PreferKey.backupPath,
                title = context.getString(R.string.backup_path),
                summary = context.getPrefString(PreferKey.backupPath)
                    ?.takeIf { it.isNotBlank() }
                    ?: defaultBackupPathSummary(context),
            )
        )
        add(
            BackupSettingEntry.Item(
                key = PreferKey.backupContent,
                title = context.getString(R.string.backup_content),
                summary = context.getString(R.string.backup_content_summary),
            )
        )
        add(
            BackupSettingEntry.Item(
                key = KEY_LAN_BACKUP_TRANSFER,
                title = context.getString(R.string.lan_backup_transfer),
                summary = context.getString(R.string.lan_backup_transfer_summary),
            )
        )
        add(
            BackupSettingEntry.Item(
                key = KEY_WEB_DAV_BACKUP,
                title = context.getString(R.string.backup),
                summary = context.getString(R.string.backup_destination_summary),
            )
        )
        add(
            BackupSettingEntry.Item(
                key = KEY_WEB_DAV_RESTORE,
                title = context.getString(R.string.restore),
                summary = context.getString(R.string.restore_summary),
            )
        )
        add(
            BackupSettingEntry.Item(
                key = PreferKey.restoreIgnore,
                title = context.getString(R.string.restore_ignore),
                summary = context.getString(R.string.restore_ignore_summary),
            )
        )
        add(switchItem(context, PreferKey.onlyLatestBackup, R.string.only_latest_backup_t, R.string.only_latest_backup_s))
        add(
            BackupSettingEntry.Item(
                key = PreferKey.autoBackup,
                title = context.getString(R.string.auto_backup_t),
                summary = autoBackupSummary(context),
            )
        )
        add(switchItem(context, PreferKey.autoCheckNewBackup, R.string.auto_check_new_backup_t, R.string.auto_check_new_backup_s))
    }

    fun textKind(key: String): TextKind? = when (key) {
        PreferKey.webDavPassword -> TextKind.PASSWORD
        PreferKey.webDavBackupKeepCount -> TextKind.NUMBER
        PreferKey.webDavUrl,
        PreferKey.webDavAccount,
        PreferKey.webDavDir,
        PreferKey.webDavDeviceName -> TextKind.TEXT

        else -> null
    }

    /** 编辑框里展示的当前值 */
    fun textValue(context: Context, key: String): String = when (key) {
        PreferKey.webDavDir -> AppConfig.webDavDir.orEmpty()
        PreferKey.webDavDeviceName -> AppConfig.webDavDeviceName.orEmpty()
        PreferKey.webDavBackupKeepCount -> AppConfig.webDavBackupKeepCount.toString()
        else -> context.getPrefString(key).orEmpty()
    }

    /**
     * 保存编辑框的值。
     *
     * @return true 表示 WebDAV 客户端配置需要刷新
     */
    fun saveText(context: Context, key: String, text: String): Boolean {
        val value = text.trim()
        return when (key) {
            PreferKey.webDavBackupKeepCount -> {
                val count = value.toIntOrNull() ?: 0
                context.putPrefString(key, if (count <= 0) null else count.toString())
                false
            }

            PreferKey.webDavDir, PreferKey.webDavDeviceName -> {
                context.putPrefString(key, value.ifEmpty { null })
                false
            }

            else -> {
                context.putPrefString(key, value.ifEmpty { null })
                key in webDavConfigKeys
            }
        }
    }

    /** 开关行的保存；返回 true 表示需要刷新整页（例如依赖项状态变化） */
    fun saveSwitch(context: Context, key: String, checked: Boolean): Boolean {
        context.putPrefBoolean(key, checked)
        return key == PreferKey.syncBookProgress
    }

    fun autoBackupSummary(context: Context): String =
        if (context.getPrefBoolean(PreferKey.autoBackup, true)) {
            context.getString(
                R.string.auto_backup_destination_summary,
                context.getString(if (context.getPrefBoolean(PreferKey.autoBackupWebDav, true)) R.string.backup_local_webdav else R.string.backup_local_only),
                context.getPrefInt(PreferKey.autoBackupIntervalDays, 1).coerceAtLeast(1),
            )
        } else {
            context.getString(R.string.auto_backup_disabled)
        }

    fun defaultBackupPathSummary(context: Context): String =
        "${context.getString(R.string.default_path)}\n${context.externalFiles.absolutePath}"

    private fun textItem(
        context: Context,
        key: String,
        titleRes: Int,
        summaryRes: Int,
    ): BackupSettingEntry.Item = BackupSettingEntry.Item(
        key = key,
        title = context.getString(titleRes),
        summary = context.getPrefString(key).takeUnless { it.isNullOrBlank() }
            ?: context.getString(summaryRes),
    )

    private fun switchItem(
        context: Context,
        key: String,
        titleRes: Int,
        summaryRes: Int,
        enabled: Boolean = true,
    ): BackupSettingEntry.Item = BackupSettingEntry.Item(
        key = key,
        title = context.getString(titleRes),
        summary = context.getString(summaryRes),
        kind = BackupSettingEntry.Kind.Switch,
        checked = context.getPrefBoolean(key, defaultOf(key)),
        enabled = enabled,
    )

    private fun defaultOf(key: String): Boolean = when (key) {
        PreferKey.syncBookProgress -> true
        PreferKey.onlyLatestBackup -> true
        PreferKey.autoCheckNewBackup -> true
        else -> false
    }
}
