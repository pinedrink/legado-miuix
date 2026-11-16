package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogEditCodeBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.FragmentOtherConfigBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.normalizeJsSourceApiToken
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.service.McpService
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.ui.config.other.OtherSettingEntry
import io.legado.app.ui.config.other.OtherSettingsActions
import io.legado.app.ui.config.other.OtherSettingsScreen
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.video.config.SettingsDialog
import io.legado.app.ui.widget.code.addJsonPattern
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.isJsonObject
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString
import io.legado.app.utils.removePref
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 其它设置（阶段 D）。
 *
 * 界面由 Compose + Miuix 渲染；偏好读写、服务重启等副作用走 [OtherSettingsActions]，
 * 与原 `onPreferenceTreeClick` / `onSharedPreferenceChanged` 一一对应。
 * User-Agent、自定义 hosts、端口/线程数、清理缓存等弹窗继续复用原有实现。
 */
class OtherConfigFragment : BaseFragment(R.layout.fragment_other_config), ConfigSearchable {

    private val binding by viewBinding(FragmentOtherConfigBinding::bind)

    private val viewModel by activityViewModels<ConfigViewModel>()

    private var entries by mutableStateOf<List<OtherSettingEntry>>(emptyList())
    private val listState = LazyListState()

    private val selectBookTreeDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { treeUri -> AppConfig.defaultBookTreeUri = treeUri.toString() }
        refreshRows()
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        activity?.setTitle(R.string.other_setting)
        // 与原实现在 onCreatePreferences 里同步"添加到文本菜单"开关状态一致
        appCtxProcessTextSync()
        refreshRows()
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LegadoMiuixTheme {
                    OtherSettingsScreen(
                        entries = entries,
                        onItemClick = ::onItemClick,
                        onSwitchChange = ::onSwitchChange,
                        listState = listState,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRows()
    }

    private fun appCtxProcessTextSync() {
        putPrefBoolean(PreferKey.processText, OtherSettingsActions.isProcessTextEnabled())
    }

    private fun refreshRows() {
        context?.let { entries = OtherSettingsActions.entries(it) }
    }

    /**
     * 保留迁移前的搜索能力：按标题/摘要过滤，选中后滚动到对应分组并触发点击。
     */
    override fun searchConfig(query: String): List<ConfigSearchItem> {
        val context = context ?: return emptyList()
        val all = OtherSettingsActions.entries(context)
        return all.mapIndexedNotNull { index, entry ->
            if (entry !is OtherSettingEntry.Item) return@mapIndexedNotNull null
            val matches = entry.title.contains(query, ignoreCase = true) ||
                entry.summary?.contains(query, ignoreCase = true) == true
            if (!matches) return@mapIndexedNotNull null
            ConfigSearchItem(entry.title) {
                // requestScrollToItem 是非挂起 API，在 Compose 组合之外调用是安全的
                listState.requestScrollToItem(groupIndexOf(all, index))
                onItemClick(entry.key)
            }
        }
    }

    private fun groupIndexOf(entries: List<OtherSettingEntry>, entryIndex: Int): Int {
        var group = 0
        for (i in 1..entryIndex) {
            val prevIsItem = entries[i - 1] is OtherSettingEntry.Item
            val currentIsItem = entries[i] is OtherSettingEntry.Item
            if (prevIsItem != currentIsItem) group++
        }
        return group
    }

    private fun onSwitchChange(key: String, checked: Boolean) {
        val context = context ?: return
        OtherSettingsActions.onSwitchChanged(context, key, checked)
        if (key == PreferKey.liveUpdateNotifications && checked) {
            ensurePromotedNotificationPermission()
        }
        refreshRows()
    }

    private fun onItemClick(key: String) {
        val context = context ?: return
        val listOptions = OtherSettingsActions.listOptions(context, key)
        if (listOptions.isNotEmpty()) {
            // 用单选列表保持原 ListPreference 的"当前项打勾"体验
            val currentIndex = listOptions
                .indexOfFirst { it.first == OtherSettingsActions.listValue(context, key) }
                .coerceAtLeast(0)
            context.alert(getString(OtherSettingsActions.listTitleRes(key))) {
                singleChoiceItems(
                    listOptions.map { it.second }.toTypedArray(),
                    currentIndex,
                ) { _, which ->
                    OtherSettingsActions.onListSelected(context, key, listOptions[which].first)
                    refreshRows()
                }
                cancelButton()
            }
            return
        }
        val range = OtherSettingsActions.numberRange(key)
        if (range != null) {
            NumberPickerDialog(requireContext())
                .setTitle(getString(numberTitleRes(key)))
                .setMaxValue(range.last)
                .setMinValue(range.first)
                .setValue(OtherSettingsActions.numberValue(key))
                .show {
                    OtherSettingsActions.onNumberValueChanged(context, key, it)
                    refreshRows()
                }
            return
        }
        when (key) {
            PreferKey.userAgent -> showUserAgentDialog()
            PreferKey.customHosts -> showCustomHostsDialog()
            PreferKey.jsSourceApiToken -> showJsSourceApiTokenDialog()
            PreferKey.videoSetting -> showDialogFragment(SettingsDialog(requireActivity()))
            PreferKey.defaultBookTreeUri -> selectBookTreeDir.launch {
                title = getString(R.string.select_book_folder)
                mode = HandleFileContract.DIR_SYS
            }

            PreferKey.uploadRule -> showDialogFragment<DirectLinkUploadConfig>()
            PreferKey.checkSource -> showDialogFragment<CheckSourceConfig>()
            PreferKey.cleanCache -> clearCache()
            PreferKey.clearWebViewData -> clearWebViewData()
            PreferKey.shrinkDatabase -> shrinkDatabase()
        }
    }

    private fun numberTitleRes(key: String): Int = when (key) {
        PreferKey.preDownloadNum -> R.string.pre_download
        PreferKey.threadCount -> R.string.threads_num_title
        PreferKey.webPort -> R.string.web_port_title
        PreferKey.mcpPort -> R.string.mcp_port_title
        PreferKey.bitmapCacheSize -> R.string.bitmap_cache_size
        PreferKey.imageRetainNum -> R.string.image_retain_number
        PreferKey.sourceEditMaxLine -> R.string.source_edit_text_max_line
        else -> R.string.other_setting
    }

    @SuppressLint("InflateParams")
    private fun showUserAgentDialog() {
        requireContext().alert(getString(R.string.user_agent)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = getString(R.string.user_agent)
                editView.setText(AppConfig.userAgent)
            }
            customView { alertBinding.root }
            okButton {
                val userAgent = alertBinding.editView.text?.toString()
                if (userAgent.isNullOrBlank()) {
                    removePref(PreferKey.userAgent)
                } else {
                    putPrefString(PreferKey.userAgent, userAgent)
                }
                refreshRows()
            }
            cancelButton()
        }
    }

    @SuppressLint("InflateParams")
    private fun showCustomHostsDialog() {
        requireContext().alert(getString(R.string.custom_hosts)) {
            val alertBinding = DialogEditCodeBinding.inflate(layoutInflater).apply {
                editViewC.hint = getString(R.string.json_format)
                editView.addJsonPattern()
                editView.setText(AppConfig.customHosts)
            }
            customView { alertBinding.root }
            okButton {
                val customHosts = alertBinding.editView.text?.toString()
                if (customHosts.isJsonObject()) {
                    putPrefString(PreferKey.customHosts, customHosts!!)
                } else {
                    removePref(PreferKey.customHosts)
                }
                refreshRows()
            }
            cancelButton()
        }
    }

    /**
     * Web/MCP 访问令牌。原实现用非持久化的 EditTextPreference + 密码输入；
     * 这里用同样的输入类型和规范化逻辑，改动令牌时按原规则重启 MCP 服务。
     */
    @SuppressLint("InflateParams")
    private fun showJsSourceApiTokenDialog() {
        requireContext().alert(getString(R.string.js_source_api_token_title)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = getString(R.string.js_source_api_token_title)
                editView.inputType =
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                editView.setText(AppConfig.jsSourceApiToken)
                editView.setSelection(editView.text.length)
            }
            customView { alertBinding.root }
            okButton {
                val previousToken = AppConfig.jsSourceApiToken
                val token = normalizeJsSourceApiToken(alertBinding.editView.text?.toString())
                AppConfig.jsSourceApiToken = token
                if (McpService.isRun &&
                    AppConfig.jsSourceApiTokenRequired &&
                    previousToken != token
                ) {
                    if (token == null) {
                        McpService.stop(requireContext())
                    } else {
                        McpService.restart(requireContext())
                    }
                }
                refreshRows()
            }
            cancelButton()
        }
    }

    private fun clearCache() {
        requireContext().alert(
            titleResource = R.string.clear_cache,
            messageResource = R.string.sure_del
        ) {
            okButton { viewModel.clearCache() }
            noButton()
        }
    }

    private fun clearWebViewData() {
        requireContext().alert(R.string.clear_webview_data, R.string.sure_del) {
            okButton { viewModel.clearWebViewData() }
            noButton()
        }
    }

    private fun shrinkDatabase() {
        requireContext().alert(R.string.sure, R.string.shrink_database) {
            okButton { viewModel.shrinkDatabase() }
            noButton()
        }
    }

    /**
     * 与迁移前一致：开启"实时更新通知"时，如果没有通知推广权限，
     * 跳系统设置页；跳不过去就把开关还原并提示。
     */
    private fun ensurePromotedNotificationPermission() {
        val context = context ?: return
        if (!OtherSettingsActions.canConfigurePromotedNotifications(context)) return
        if (context.getPrefBoolean(PreferKey.liveUpdateNotifications, false) != true) return
        val intent = OtherSettingsActions.promotedNotificationSettingsIntent(context)
        if (intent.resolveActivity(context.packageManager) != null) {
            startActivity(intent)
        } else {
            putPrefBoolean(PreferKey.liveUpdateNotifications, false)
            context.toastOnUi(R.string.tip_cannot_jump_setting_page)
            refreshRows()
        }
    }
}
