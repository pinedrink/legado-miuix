package io.legado.app.ui.config.other

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import com.jeremyliao.liveeventbus.LiveEventBus
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppFreezeMonitor
import io.legado.app.help.DispatchersMonitor
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.Cronet
import io.legado.app.model.CheckSource
import io.legado.app.model.ImageProvider
import io.legado.app.receiver.SharedReceiverActivity
import io.legado.app.service.McpService
import io.legado.app.service.WebService
import io.legado.app.utils.LogUtils
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString
import io.legado.app.utils.restart
import io.legado.app.utils.supportsPromotedNotifications
import splitties.init.appCtx

/**
 * 其它设置页的数据与副作用。
 *
 * 只做偏好读写，以及迁移前 `onSharedPreferenceChanged` 里做过的那些副作用
 * （重启服务、切换日志级别、语言重启等）；弹窗与页面跳转仍由 Fragment 处理。
 */
object OtherSettingsActions {

    const val SECTION_GENERAL = "section_general"
    const val SECTION_MAIN = "section_main"
    const val SECTION_OTHER = "section_other"

    /** 原偏好文件里没有对应 PreferKey 常量的两个 key。 */
    const val KEY_MEDIA_BUTTON_ON_EXIT = "mediaButtonOnExit"
    const val KEY_AUTO_UPDATE_VARIANT = "autoUpdateVariant"

    private data class Spec(
        val key: String,
        @StringRes val titleRes: Int = 0,
        val kind: OtherSettingEntry.Kind = OtherSettingEntry.Kind.Action,
        @StringRes val summaryRes: Int? = null,
        val titleText: String? = null,
        val defaultChecked: Boolean = false,
        @ArrayRes val entriesRes: Int? = null,
        @ArrayRes val entryValuesRes: Int? = null,
    )

    private val generalSpecs = listOf(
        Spec(
            PreferKey.language,
            R.string.language,
            OtherSettingEntry.Kind.List,
            entriesRes = R.array.language,
            entryValuesRes = R.array.language_value,
        ),
        Spec(
            PreferKey.bookMetadataSync,
            R.string.book_metadata_sync,
            OtherSettingEntry.Kind.List,
            summaryRes = R.string.book_metadata_sync_summary,
            entriesRes = R.array.book_metadata_sync_options,
            entryValuesRes = R.array.book_metadata_sync_values,
        ),
    )

    private val mainSpecs = listOf(
        Spec(
            PreferKey.autoRefresh,
            R.string.pt_auto_refresh,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.ps_auto_refresh,
        ),
        Spec(
            PreferKey.onlyUpdateRead,
            R.string.only_update_read,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.ps_only_update_read,
        ),
        Spec(
            PreferKey.defaultToRead,
            R.string.pt_default_read,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.ps_default_read,
        ),
        Spec(
            PreferKey.showDiscovery,
            R.string.show_discovery,
            OtherSettingEntry.Kind.Switch,
            defaultChecked = true,
        ),
        Spec(
            PreferKey.showDiscoveryFastScroller,
            R.string.show_discovery_fast_scroller,
            OtherSettingEntry.Kind.Switch,
        ),
        Spec(
            PreferKey.showRss,
            R.string.show_rss,
            OtherSettingEntry.Kind.Switch,
            defaultChecked = true,
        ),
        Spec(
            PreferKey.defaultHomePage,
            R.string.default_home_page,
            OtherSettingEntry.Kind.List,
            entriesRes = R.array.default_home_page,
            entryValuesRes = R.array.default_home_page_value,
        ),
    )

    private val otherSpecs = listOf(
        Spec(PreferKey.userAgent, R.string.user_agent),
        Spec(PreferKey.customHosts, R.string.custom_hosts, summaryRes = R.string.custom_hosts_summary),
        Spec(
            PreferKey.webServiceWakeLock,
            R.string.web_service_wake_lock,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.web_service_wake_lock_summary,
        ),
        Spec(PreferKey.defaultBookTreeUri, R.string.book_tree_uri_t),
        Spec(PreferKey.sourceEditMaxLine, R.string.source_edit_text_max_line),
        Spec(PreferKey.checkSource, R.string.check_source_config),
        Spec(
            PreferKey.uploadRule,
            R.string.direct_link_upload_rule,
            summaryRes = R.string.direct_link_upload_rule_summary,
        ),
        Spec(
            PreferKey.cronet,
            kind = OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.pref_cronet_summary,
            titleText = "Cronet",
        ),
        Spec(
            PreferKey.antiAlias,
            R.string.anti_alias,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.pref_anti_alias_summary,
        ),
        Spec(PreferKey.bitmapCacheSize, R.string.bitmap_cache_size),
        Spec(PreferKey.imageRetainNum, R.string.image_retain_number),
        Spec(PreferKey.preDownloadNum, R.string.pre_download),
        Spec(
            PreferKey.replaceEnableDefault,
            R.string.replace_enable_default_t,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.replace_enable_default_s,
            defaultChecked = true,
        ),
        Spec(
            KEY_MEDIA_BUTTON_ON_EXIT,
            R.string.media_button_on_exit_title,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.media_button_on_exit_summary,
            defaultChecked = true,
        ),
        Spec(
            PreferKey.readAloudByMediaButton,
            R.string.read_aloud_by_media_button_title,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.read_aloud_by_media_button_summary,
        ),
        Spec(
            PreferKey.ignoreAudioFocus,
            R.string.ignore_audio_focus_title,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.ignore_audio_focus_summary,
        ),
        Spec(
            PreferKey.autoClearExpired,
            R.string.auto_clear_expired,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.auto_clear_expired_summary,
            defaultChecked = true,
        ),
        Spec(
            PreferKey.showAddToShelfAlert,
            R.string.show_add_to_shelf_alert_title,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.show_add_to_shelf_alert_summary,
            defaultChecked = true,
        ),
        Spec(
            KEY_AUTO_UPDATE_VARIANT,
            R.string.auto_update,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.auto_update_summary,
            defaultChecked = true,
        ),
        Spec(
            PreferKey.liveUpdateNotifications,
            R.string.live_update_notifications,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.live_update_notifications_summary,
        ),
        Spec(
            PreferKey.showMangaUi,
            R.string.show_manga_ui,
            OtherSettingEntry.Kind.Switch,
            defaultChecked = true,
        ),
        Spec(PreferKey.videoSetting, R.string.video_setting, summaryRes = R.string.video_setting_summary),
        Spec(PreferKey.webPort, R.string.web_port_title),
        Spec(PreferKey.mcpPort, R.string.mcp_port_title),
        Spec(
            PreferKey.jsSourceApiTokenRequired,
            R.string.js_source_api_token_required_title,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.js_source_api_token_required_summary,
            defaultChecked = true,
        ),
        Spec(
            PreferKey.jsSourceApiToken,
            R.string.js_source_api_token_title,
            summaryRes = R.string.js_source_api_token_summary,
        ),
        Spec(PreferKey.cleanCache, R.string.clear_cache, summaryRes = R.string.clear_cache_summary),
        Spec(
            PreferKey.clearWebViewData,
            R.string.clear_webview_data,
            summaryRes = R.string.clear_webview_data_summary,
        ),
        Spec(
            PreferKey.shrinkDatabase,
            R.string.shrink_database,
            summaryRes = R.string.shrink_database_summary,
        ),
        Spec(PreferKey.threadCount, R.string.threads_num_title),
        Spec(
            PreferKey.processText,
            R.string.add_to_text_context_menu_t,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.add_to_text_context_menu_s,
            defaultChecked = true,
        ),
        Spec(
            PreferKey.recordLog,
            R.string.record_log,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.record_debug_log,
        ),
        Spec(
            PreferKey.recordHttpLog,
            R.string.record_http_log,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.record_http_log_summary,
        ),
        Spec(
            PreferKey.recordHeapDump,
            R.string.record_heap_dump_t,
            OtherSettingEntry.Kind.Switch,
            summaryRes = R.string.record_heap_dump_s,
        ),
    )

    /** 生成页面全部行，顺序与 pref_config_other.xml 一致。 */
    fun entries(context: Context): List<OtherSettingEntry> {
        val autoRefresh = context.getPrefBoolean(PreferKey.autoRefresh, false)
        val liveUpdateVisible = canConfigurePromotedNotifications(context)
        val rows = mutableListOf<OtherSettingEntry>()
        rows += generalSpecs.map { it.toItem(context) }
        rows += OtherSettingEntry.Section(R.string.main_activity)
        rows += mainSpecs.mapNotNull { spec ->
            when {
                spec.key == PreferKey.onlyUpdateRead && !autoRefresh -> null
                else -> spec.toItem(context)
            }
        }
        rows += OtherSettingEntry.Section(R.string.other_setting)
        rows += otherSpecs.mapNotNull { spec ->
            if (spec.key == PreferKey.liveUpdateNotifications && !liveUpdateVisible) null
            else spec.toItem(context)
        }
        return rows
    }

    private fun Spec.toItem(context: Context): OtherSettingEntry.Item {
        val checked = kind == OtherSettingEntry.Kind.Switch &&
            context.getPrefBoolean(key, defaultChecked)
        val value = if (kind == OtherSettingEntry.Kind.List) listLabel(context, key) else null
        return OtherSettingEntry.Item(
            key = key,
            title = titleText ?: context.getString(titleRes),
            summary = summaryFor(context, this),
            kind = kind,
            checked = checked,
            value = value,
        )
    }

    private fun summaryFor(context: Context, spec: Spec): String? {
        return when (spec.key) {
            PreferKey.userAgent -> AppConfig.userAgent
            PreferKey.customHosts -> context.getString(R.string.custom_hosts_summary)
            PreferKey.defaultBookTreeUri -> AppConfig.defaultBookTreeUri
                ?: context.getString(R.string.book_tree_uri_s)

            PreferKey.sourceEditMaxLine ->
                context.getString(R.string.source_edit_max_line_summary, AppConfig.sourceEditMaxLine.toString())

            PreferKey.checkSource -> CheckSource.summary

            PreferKey.bitmapCacheSize ->
                context.getString(R.string.bitmap_cache_size_summary, AppConfig.bitmapCacheSize.toString())

            PreferKey.imageRetainNum ->
                context.getString(R.string.image_retain_number_summary, AppConfig.imageRetainNum.toString())

            PreferKey.preDownloadNum ->
                context.getString(R.string.pre_download_s, AppConfig.preDownloadNum.toString())

            PreferKey.webPort -> context.getString(R.string.web_port_summary, AppConfig.webPort.toString())
            PreferKey.mcpPort -> context.getString(R.string.mcp_port_summary, AppConfig.mcpPort.toString())
            PreferKey.threadCount -> context.getString(R.string.threads_num, AppConfig.threadCount.toString())

            PreferKey.jsSourceApiToken -> if (AppConfig.jsSourceApiToken.isNullOrBlank()) {
                context.getString(R.string.js_source_api_token_summary)
            } else {
                context.getString(R.string.js_source_api_token_configured)
            }

            else -> spec.summaryRes?.let { context.getString(it) }
        }
    }

    /** 列表行的标题资源，用于选择弹窗的标题。 */
    @StringRes
    fun listTitleRes(key: String): Int =
        (generalSpecs + mainSpecs).firstOrNull { it.key == key }?.titleRes ?: R.string.other_setting

    /** 列表行的可选项（值 to 显示文案）。 */
    fun listOptions(context: Context, key: String): List<Pair<String, String>> {
        val spec = (generalSpecs + mainSpecs).firstOrNull { it.key == key } ?: return emptyList()
        val entriesRes = spec.entriesRes ?: return emptyList()
        val valuesRes = spec.entryValuesRes ?: return emptyList()
        val labels = context.resources.getStringArray(entriesRes)
        val values = context.resources.getStringArray(valuesRes)
        return values.indices.mapNotNull { index ->
            labels.getOrNull(index)?.let { values[index] to it }
        }
    }

    fun listValue(context: Context, key: String): String {
        val fallback = when (key) {
            PreferKey.language -> "auto"
            PreferKey.bookMetadataSync -> "never"
            PreferKey.defaultHomePage -> "bookshelf"
            else -> listOptions(context, key).firstOrNull()?.first
        }
        return context.getPrefString(key, fallback) ?: fallback ?: ""
    }

    fun listLabel(context: Context, key: String): String {
        val value = listValue(context, key)
        return listOptions(context, key).firstOrNull { it.first == value }?.second ?: value
    }

    /** 列表项变更：语言需要重启应用，其它只写偏好。 */
    fun onListSelected(context: Context, key: String, value: String) {
        context.putPrefString(key, value)
        if (key == PreferKey.language) {
            Handler(Looper.getMainLooper()).postDelayed({ appCtx.restart() }, 1000)
        }
    }

    /** 开关变更：写偏好并执行迁移前 onSharedPreferenceChanged 里的副作用。 */
    fun onSwitchChanged(context: Context, key: String, checked: Boolean) {
        context.putPrefBoolean(key, checked)
        when (key) {
            PreferKey.showDiscovery, PreferKey.showRss -> postEvent(EventBus.NOTIFY_MAIN, true)
            PreferKey.processText -> setProcessTextEnable(checked)
            PreferKey.cronet -> if (checked) Cronet.preDownload()
            PreferKey.recordLog -> onRecordLogChanged(context)
            PreferKey.jsSourceApiTokenRequired -> restartServers(context)
        }
    }

    /** 数字类设置写入后的副作用，与迁移前偏好监听里的处理保持一致。 */
    fun onNumberValueChanged(context: Context, key: String, value: Int) {
        when (key) {
            PreferKey.preDownloadNum -> AppConfig.preDownloadNum = value
            PreferKey.threadCount -> {
                AppConfig.threadCount = value
                postEvent(PreferKey.threadCount, "")
            }

            PreferKey.webPort -> {
                AppConfig.webPort = value
                if (WebService.isRun) {
                    WebService.stop(context)
                    WebService.start(context)
                }
            }

            PreferKey.mcpPort -> {
                AppConfig.mcpPort = value
                if (McpService.isRun) {
                    McpService.restart(context)
                }
            }

            PreferKey.bitmapCacheSize -> {
                AppConfig.bitmapCacheSize = value
                ImageProvider.bitmapLruCache.resize(ImageProvider.cacheSize)
            }

            PreferKey.imageRetainNum -> AppConfig.imageRetainNum = value
            PreferKey.sourceEditMaxLine -> AppConfig.sourceEditMaxLine = value
        }
    }

    /** 数字类设置项的取值范围；不在其中的 key 返回 null。 */
    fun numberRange(key: String): IntRange? = when (key) {
        PreferKey.preDownloadNum -> 0..9999
        PreferKey.threadCount -> 1..999
        PreferKey.webPort -> 1024..60000
        PreferKey.mcpPort -> 1024..65530
        PreferKey.bitmapCacheSize -> 1..1024
        PreferKey.imageRetainNum -> 0..999
        PreferKey.sourceEditMaxLine -> 10..Int.MAX_VALUE
        else -> null
    }

    fun numberValue(key: String): Int = when (key) {
        PreferKey.preDownloadNum -> AppConfig.preDownloadNum
        PreferKey.threadCount -> AppConfig.threadCount
        PreferKey.webPort -> AppConfig.webPort
        PreferKey.mcpPort -> AppConfig.mcpPort
        PreferKey.bitmapCacheSize -> AppConfig.bitmapCacheSize
        PreferKey.imageRetainNum -> AppConfig.imageRetainNum
        PreferKey.sourceEditMaxLine -> AppConfig.sourceEditMaxLine
        else -> 0
    }

    private fun onRecordLogChanged(context: Context) {
        AppConfig.recordLog = context.getPrefBoolean(PreferKey.recordLog)
        LogUtils.upLevel()
        LogUtils.logDeviceInfo()
        LiveEventBus.config().enableLogger(AppConfig.recordLog)
        AppFreezeMonitor.init(appCtx)
        DispatchersMonitor.init()
    }

    private fun restartServers(context: Context) {
        if (WebService.isRun) {
            WebService.stop(context)
            WebService.start(context)
        }
        if (McpService.isRun) {
            McpService.restart(context)
        }
    }

    private fun setProcessTextEnable(enable: Boolean) {
        val state = if (enable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        appCtx.packageManager.setComponentEnabledSetting(
            sharedReceiverComponent(), state, PackageManager.DONT_KILL_APP
        )
    }

    fun isProcessTextEnabled(): Boolean =
        appCtx.packageManager.getComponentEnabledSetting(sharedReceiverComponent()) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    private fun sharedReceiverComponent() =
        ComponentName(appCtx, SharedReceiverActivity::class.java.name)

    /** 是否在页面上展示"实时更新通知"这一行。 */
    fun canConfigurePromotedNotifications(context: Context): Boolean {
        if (!supportsPromotedNotifications()) return false
        return NotificationManagerCompat.from(context).canPostPromotedNotifications() ||
            promotedNotificationSettingsIntent(context)
                .resolveActivity(context.packageManager) != null
    }

    // ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS 是编译期内联的字符串常量，
    // 低版本系统上只是 resolve 不到 Activity，不会因此崩溃，所以这里抑制 InlinedApi。
    @SuppressLint("InlinedApi")
    fun promotedNotificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
}
