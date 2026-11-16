package io.legado.app.ui.main.my

import android.os.Bundle
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
import io.legado.app.databinding.FragmentMyMoreBinding
import io.legado.app.lib.dialogs.selector
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.ui.config.ConfigSearchable
import io.legado.app.utils.observeEventSticky
import io.legado.app.utils.openUrl
import io.legado.app.utils.sendToClip
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 「我的」→「更多」页。
 *
 * 原来是一个 PreferenceFragment（`pref_main.xml` 按 `myMoreItems` 过滤），
 * 现在与「我的」页共用同一套 Compose 行渲染（[MySettingsScreen]）与行数据
 * （[MySettingsActions.buildRows]，`onlyMore = true`），行为保持不变。
 */
class MyMoreFragment : BaseFragment(R.layout.fragment_my_more), ConfigSearchable {

    private val binding by viewBinding(FragmentMyMoreBinding::bind)

    private var entries by mutableStateOf<List<MySettingEntry>>(emptyList())
    private var currentThemeValue by mutableStateOf("0")
    private var showThemePicker by mutableStateOf(false)
    private var themeOptions by mutableStateOf<List<Pair<String, String>>>(emptyList())

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        activity?.setTitle(R.string.reader_menu_more)
        themeOptions = MySettingsActions.themeModeOptions(requireContext())
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

    override fun searchConfig(query: String): List<io.legado.app.ui.config.ConfigSearchItem> {
        val text = query.trim()
        if (text.isEmpty()) return emptyList()
        return entries.filterIsInstance<MySettingEntry.Item>()
            .filter { item ->
                item.title.contains(text, true) || item.summary?.contains(text, true) == true
            }
            .map { item ->
                io.legado.app.ui.config.ConfigSearchItem(item.title) { onItemClick(item.key) }
            }
    }

    private fun observeServiceState() {
        observeEventSticky<String>(EventBus.WEB_SERVICE) { refreshRows() }
        observeEventSticky<String>(EventBus.MCP_SERVICE) { refreshRows() }
    }

    private fun refreshRows() {
        if (!isAdded) return
        val context = requireContext()
        currentThemeValue = MySettingsActions.currentThemeMode(context)
        entries = MySettingsActions.buildRows(context, onlyMore = true)
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
