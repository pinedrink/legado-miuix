package io.legado.app.ui.main.my

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.ThemeConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.service.AutoTaskScheduler
import io.legado.app.service.McpService
import io.legado.app.service.WebService
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.ReadRecordActivity
import io.legado.app.ui.about.checkAppUpdate
import io.legado.app.ui.autoTask.AutoTaskActivity
import io.legado.app.ui.book.bookmark.AllBookmarkActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.book.toc.rule.TxtTocRuleActivity
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigTag
import io.legado.app.ui.dict.rule.DictRuleActivity
import io.legado.app.ui.file.FileManageActivity
import io.legado.app.ui.replace.ReplaceRuleActivity
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.getPrefStringSet
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString
import io.legado.app.utils.putPrefStringSet
import io.legado.app.utils.startActivity

/**
 * 「我的」页设置项的数据与动作。
 *
 * 这里只做两件事：描述"有哪些设置项"，以及"点它们会发生什么"。
 * 逻辑全部沿用原 [MyFragment.MyPreferenceFragment] 的实现，没有新增或改写业务行为。
 */
object MySettingsActions {

    /**
     * 一条设置项的静态描述，顺序与原来的 pref_main.xml 保持一致。
     */
    data class Spec(
        val key: String,
        @StringRes val titleRes: Int,
        @StringRes val summaryRes: Int? = null,
        val iconRes: Int? = null,
    )

    const val KEY_THEME_MODE = PreferKey.themeMode
    const val KEY_WEB_SERVICE = PreferKey.webService
    const val KEY_MCP_SERVICE = PreferKey.mcpService
    const val KEY_AUTO_TASK_SERVICE = PreferKey.autoTaskService
    const val KEY_MY_MORE = "myMore"
    const val KEY_EXIT = "exit"

    private const val SECTION_SETTING = "section_setting"
    private const val SECTION_OTHER = "section_other"

    /** 与 pref_main.xml 相同的分组顺序；分组标题用 [SECTION_SETTING] / [SECTION_OTHER] 占位。 */
    val allSpecs: List<Spec> = listOf(
        Spec("bookSourceManage", R.string.book_source_manage, R.string.book_source_manage_desc, R.drawable.ic_cfg_source),
        Spec("autoTaskManage", R.string.auto_task_manage, R.string.auto_task_manage_desc, R.drawable.ic_auto_page),
        Spec(KEY_AUTO_TASK_SERVICE, R.string.auto_task_service, R.string.auto_task_service_desc, R.drawable.ic_auto_page),
        Spec("txtTocRuleManage", R.string.txt_toc_rule, R.string.config_txt_toc_rule, R.drawable.ic_cfg_source),
        Spec("replaceManage", R.string.replace_purify, R.string.replace_purify_desc, R.drawable.ic_cfg_replace),
        Spec("dictRuleManage", R.string.dict_rule, R.string.config_dict_rule, R.drawable.ic_translate),
        Spec(KEY_THEME_MODE, R.string.theme_mode, R.string.theme_mode_desc, R.drawable.ic_cfg_theme),
        Spec(KEY_WEB_SERVICE, R.string.web_service, R.string.web_service_desc, R.drawable.ic_cfg_web),
        Spec(KEY_MCP_SERVICE, R.string.mcp_service, R.string.mcp_service_desc, R.drawable.ic_cfg_web),
        Spec(SECTION_SETTING, R.string.setting),
        Spec("web_dav_setting", R.string.backup_restore, R.string.web_dav_set_import_old, R.drawable.ic_cfg_backup),
        Spec("theme_setting", R.string.theme_setting, R.string.theme_setting_s, R.drawable.ic_cfg_theme),
        Spec("setting", R.string.other_setting, R.string.other_setting_s, R.drawable.ic_cfg_other),
        Spec(SECTION_OTHER, R.string.other),
        Spec("bookmark", R.string.bookmark, R.string.all_bookmark, R.drawable.ic_bookmark),
        Spec("readRecord", R.string.read_record, R.string.read_record_summary, R.drawable.ic_history),
        Spec("fileManage", R.string.file_manage, R.string.file_manage_summary, R.drawable.ic_folder_outline),
        Spec("about", R.string.about, null, R.drawable.ic_cfg_about),
        Spec("check_update", R.string.check_update, null, R.drawable.ic_download),
        Spec("check_beta_update", R.string.check_beta_update, null, R.drawable.ic_download),
        Spec(KEY_EXIT, R.string.exit, null, R.drawable.ic_exit),
    )

    /** 可被"自定义我的"勾选的设置项（与原实现一致：排除退出项）。 */
    val customizableSpecs: List<Spec> =
        allSpecs.filter { !it.key.startsWith("section_") && it.key != KEY_EXIT }

    fun moreItems(context: Context): Set<String> {
        return context.getPrefStringSet(
            PreferKey.myMoreItems,
            mutableSetOf("check_update", "check_beta_update"),
        ) ?: setOf("check_update", "check_beta_update")
    }

    fun setMoreItems(context: Context, keys: Set<String>) {
        context.putPrefStringSet(PreferKey.myMoreItems, keys.toMutableSet())
    }

    /**
     * 生成页面要显示的行，「我的」页与「更多」页共用。
     *
     * @param onlyMore 为 true 时只输出被"自定义我的"放进「更多」页的项；
     *   为 false 时输出留在「我的」页的项，并在末尾追加「更多」入口。
     *   分组标题只在该组仍有可见项时输出，与原来 `applyVisibility` 的行为一致。
     */
    fun buildRows(context: Context, onlyMore: Boolean): List<MySettingEntry> {
        val customized = moreItems(context)
        val rows = mutableListOf<MySettingEntry>()
        var section: MySettingEntry.Section? = null
        val items = mutableListOf<MySettingEntry.Item>()

        fun flush() {
            if (items.isEmpty()) return
            section?.let { rows += it }
            rows += items
            items.clear()
        }

        allSpecs.forEach { spec ->
            if (spec.key.startsWith("section_")) {
                flush()
                section = MySettingEntry.Section(spec.titleRes)
                return@forEach
            }
            // 「我的」页隐藏被选走的项，「更多」页只显示被选走的项
            if ((spec.key in customized) != onlyMore) return@forEach
            items += buildRow(context, spec)
        }
        flush()
        if (!onlyMore) {
            rows += MySettingEntry.Item(
                key = KEY_MY_MORE,
                title = context.getString(R.string.reader_menu_more),
                iconRes = R.drawable.ic_more_vert,
            )
        }
        return rows
    }

    private fun buildRow(context: Context, spec: Spec): MySettingEntry.Item {
        return when (spec.key) {
            KEY_WEB_SERVICE -> MySettingEntry.Item(
                key = spec.key,
                title = context.getString(spec.titleRes),
                summary = if (WebService.isRun) WebService.hostAddress
                else context.getString(R.string.web_service_desc),
                iconRes = spec.iconRes,
                kind = MySettingEntry.Kind.Switch,
                checked = WebService.isRun,
            )

            KEY_MCP_SERVICE -> MySettingEntry.Item(
                key = spec.key,
                title = context.getString(spec.titleRes),
                summary = if (McpService.isRun) McpService.hostAddress
                else context.getString(R.string.mcp_service_desc),
                iconRes = spec.iconRes,
                kind = MySettingEntry.Kind.Switch,
                checked = McpService.isRun,
            )

            KEY_AUTO_TASK_SERVICE -> MySettingEntry.Item(
                key = spec.key,
                title = context.getString(spec.titleRes),
                summary = context.getString(R.string.auto_task_service_desc),
                iconRes = spec.iconRes,
                kind = MySettingEntry.Kind.Switch,
                checked = context.getPrefBoolean(PreferKey.autoTaskService),
            )

            KEY_THEME_MODE -> MySettingEntry.Item(
                key = spec.key,
                title = context.getString(spec.titleRes),
                summary = context.getString(R.string.theme_mode_desc),
                iconRes = spec.iconRes,
                kind = MySettingEntry.Kind.List,
                value = themeModeLabel(context),
            )

            else -> MySettingEntry.Item(
                key = spec.key,
                title = context.getString(spec.titleRes),
                summary = spec.summaryRes?.let { context.getString(it) },
                iconRes = spec.iconRes,
            )
        }
    }

    /** 主题模式候选项，顺序与 arrays.xml 的 theme_mode / theme_mode_v 一致。 */
    fun themeModeOptions(context: Context): List<Pair<String, String>> {
        val labels = context.resources.getStringArray(R.array.theme_mode)
        val values = context.resources.getStringArray(R.array.theme_mode_v)
        return values.indices.map { values[it] to labels[it] }
    }

    fun currentThemeMode(context: Context): String {
        return context.getPrefString(PreferKey.themeMode, "0") ?: "0"
    }

    fun themeModeLabel(context: Context, value: String = currentThemeMode(context)): String {
        return themeModeOptions(context).firstOrNull { it.first == value }?.second
            ?: context.getString(R.string.theme_mode)
    }

    fun applyThemeMode(context: Context, value: String) {
        context.putPrefString(PreferKey.themeMode, value)
        ThemeConfig.applyDayNight(context)
    }

    fun onItemClick(activity: AppCompatActivity?, key: String) {
        when (key) {
            KEY_MY_MORE -> activity?.startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.MY_MORE)
            }

            "check_update" -> activity?.checkAppUpdate()
            "check_beta_update" -> activity?.checkAppUpdate(beta = true)
            "bookSourceManage" -> activity?.startActivity<BookSourceActivity>()
            "autoTaskManage" -> activity?.startActivity<AutoTaskActivity>()
            "replaceManage" -> activity?.startActivity<ReplaceRuleActivity>()
            "dictRuleManage" -> activity?.startActivity<DictRuleActivity>()
            "txtTocRuleManage" -> activity?.startActivity<TxtTocRuleActivity>()
            "bookmark" -> activity?.startActivity<AllBookmarkActivity>()
            "setting" -> activity?.startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.OTHER_CONFIG)
            }

            "web_dav_setting" -> activity?.startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.BACKUP_CONFIG)
            }

            "theme_setting" -> activity?.startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.THEME_CONFIG)
            }

            "fileManage" -> activity?.startActivity<FileManageActivity>()
            "readRecord" -> activity?.startActivity<ReadRecordActivity>()
            "about" -> activity?.startActivity<AboutActivity>()
            KEY_EXIT -> activity?.finish()
        }
    }

    /**
     * 开关类设置项的副作用，与原实现中 SharedPreferences 监听分支一一对应。
     */
    fun onSwitchChange(context: Context, key: String, checked: Boolean) {
        when (key) {
            KEY_WEB_SERVICE -> {
                context.putPrefBoolean(key, checked)
                if (checked) WebService.start(context) else WebService.stop(context)
            }

            KEY_MCP_SERVICE -> {
                context.putPrefBoolean(key, checked)
                if (checked) McpService.start(context) else McpService.stop(context)
            }

            KEY_AUTO_TASK_SERVICE -> {
                context.putPrefBoolean(key, checked)
                val appContext = context.applicationContext
                if (checked) {
                    Coroutine.async { AutoTaskScheduler.refresh(appContext) }
                } else {
                    AutoTaskScheduler.cancelAll(appContext)
                }
            }
        }
    }

    /**
     * 长按开关行时的原有交互（复制地址 / 浏览器打开），仅在服务运行时生效。
     */
    fun serviceAddressFor(context: Context, key: String): String? {
        return when (key) {
            KEY_WEB_SERVICE -> WebService.hostAddress.takeIf { WebService.isRun }
            KEY_MCP_SERVICE -> McpService.hostAddress.takeIf { McpService.isRun }
            else -> null
        }
    }

    fun isServiceRunning(key: String): Boolean {
        return when (key) {
            KEY_WEB_SERVICE -> WebService.isRun
            KEY_MCP_SERVICE -> McpService.isRun
            else -> false
        }
    }
}
