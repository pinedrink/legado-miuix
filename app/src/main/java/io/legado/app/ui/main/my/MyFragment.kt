package io.legado.app.ui.main.my

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.FragmentMyConfigBinding
import io.legado.app.lib.dialogs.selector
import io.legado.app.service.McpService
import io.legado.app.service.WebService
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.observeEventSticky
import io.legado.app.utils.openUrl
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showHelp
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 「我的」标签页。
 *
 * 阶段 D 起页面内容改为 Miuix Compose（[MySettingsScreen]）：
 * 外层标题栏、菜单以及标签页导航仍由原有 View/Fragment 体系负责，
 * 设置项的跳转与开关副作用仍走 [MySettingsActions]（逻辑迁移自原来的偏好实现）。
 */
class MyFragment() : BaseFragment(R.layout.fragment_my_config), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    private var entries by mutableStateOf<List<MySettingEntry>>(emptyList())
    private var currentThemeValue by mutableStateOf("0")
    private var showThemePicker by mutableStateOf(false)
    private var showCustomizeDialog by mutableStateOf(false)
    private var customizeKeys by mutableStateOf<Set<String>>(emptySet())
    private var customizeOptions by mutableStateOf<List<MySettingEntry>>(emptyList())
    private var themeOptions by mutableStateOf<List<Pair<String, String>>>(emptyList())

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val context = requireContext()
        themeOptions = MySettingsActions.themeModeOptions(context)
        customizeOptions = MySettingsActions.customizableSpecs.map {
            MySettingEntry.Item(key = it.key, title = context.getString(it.titleRes))
        }
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            LegadoMiuixTheme {
                MySettingsScreen(
                    entries = entries,
                    showThemePicker = showThemePicker,
                    themeOptions = remember { themeOptions },
                    currentThemeValue = currentThemeValue,
                    showCustomizeDialog = showCustomizeDialog,
                    customizeOptions = customizeOptions,
                    selectedCustomizeKeys = customizeKeys,
                    onItemClick = ::onItemClick,
                    onItemLongClick = ::onItemLongClick,
                    onSwitchChange = { key, checked ->
                        MySettingsActions.onSwitchChange(requireContext(), key, checked)
                        refreshRows()
                    },
                    onThemeOptionClick = { value ->
                        showThemePicker = false
                        currentThemeValue = value
                        MySettingsActions.applyThemeMode(requireContext(), value)
                    },
                    onDismissThemePicker = { showThemePicker = false },
                    onDismissCustomize = { showCustomizeDialog = false },
                    onToggleCustomize = { key, checked ->
                        val newKeys = if (checked) customizeKeys + key else customizeKeys - key
                        customizeKeys = newKeys
                        MySettingsActions.setMoreItems(requireContext(), newKeys)
                        refreshRows()
                    },
                )
            }
        }
        refreshRows()
        observeServiceState()
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) refreshRows()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> showHelp("appHelp")
            R.id.menu_customize_my -> {
                customizeKeys = MySettingsActions.moreItems(requireContext())
                showCustomizeDialog = true
            }
        }
    }

    private fun observeServiceState() {
        observeEventSticky<String>(EventBus.WEB_SERVICE) { refreshRows() }
        observeEventSticky<String>(EventBus.MCP_SERVICE) { refreshRows() }
    }

    /**
     * 重新计算页面内容：服务运行状态、主题档位、以及当前该显示哪些设置项。
     */
    private fun refreshRows() {
        if (!isAdded) return
        val context = requireContext()
        customizeKeys = MySettingsActions.moreItems(context)
        currentThemeValue = MySettingsActions.currentThemeMode(context)
        entries = MySettingsActions.buildRows(context, onlyMore = false)
    }

    private fun onItemClick(key: String) {
        when (key) {
            MySettingsActions.KEY_THEME_MODE -> showThemePicker = true
            else -> {
                MySettingsActions.onItemClick(activity as? AppCompatActivity, key)
                refreshRows()
            }
        }
    }

    /**
     * 服务地址的长按操作，沿用原来的「复制地址 / 浏览器打开」。
     */
    private fun onItemLongClick(key: String) {
        val context = requireContext()
        val address = MySettingsActions.serviceAddressFor(context, key) ?: return
        context.selector(
            context.getString(R.string.web_service),
            arrayListOf(context.getString(R.string.copy_text), context.getString(R.string.browser)),
        ) { _, index ->
            when (index) {
                0 -> context.sendToClip(address)
                1 -> context.openUrl(address)
            }
        }
    }
}
