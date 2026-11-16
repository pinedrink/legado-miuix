package io.legado.app.ui.config

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.FragmentCoverFontConfigBinding
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.ui.config.coverfont.CoverFontSettingEntry
import io.legado.app.ui.config.coverfont.CoverFontSettingsActions
import io.legado.app.ui.config.coverfont.CoverFontSettingsScreen
import io.legado.app.ui.font.FontSelectDialog
import io.legado.app.ui.font.installFontFile
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.utils.FileDoc
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getPrefString
import io.legado.app.utils.openInputStream
import io.legado.app.utils.putPrefString
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 默认书名样式设置。
 *
 * 内容区已改为 Compose（[CoverFontSettingsScreen]），字号/开关/字体选择的偏好读写
 * 与"重刷默认封面 + 通知书架"的副作用都在 [CoverFontSettingsActions] 里；
 * 封面预览仍复用 View 版 `CoverImageView`（通过 AndroidView 内嵌）。
 */
class CoverFontConfigFragment : BaseFragment(R.layout.fragment_cover_font_config),
    FontSelectDialog.CallBack {

    private val binding by viewBinding(FragmentCoverFontConfigBinding::bind)
    private val listState = LazyListState()

    private var entries by mutableStateOf<List<CoverFontSettingEntry>>(emptyList())

    /** 样式变化后自增，用于重建封面预览。 */
    private var previewKey by mutableIntStateOf(0)

    private var fontJob: Job? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        activity?.setTitle(R.string.cover_font_config)
        refreshRows()
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            LegadoMiuixTheme {
                CoverFontSettingsScreen(
                    entries = entries,
                    listState = listState,
                    previewKey = previewKey,
                    onItemClick = ::onItemClick,
                    onSwitchChange = { key, checked ->
                        CoverFontSettingsActions.saveSwitch(requireContext(), key, checked)
                        onStyleChanged()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRows()
        previewKey++
    }

    private fun refreshRows() {
        entries = CoverFontSettingsActions.entries(requireContext())
    }

    /** 样式已写入偏好，这里只负责刷新列表与预览。 */
    private fun onStyleChanged() {
        refreshRows()
        previewKey++
    }

    private fun onItemClick(key: String) {
        when (key) {
            PreferKey.coverFont -> showDialogFragment(FontSelectDialog())
            in CoverFontSettingsActions.sizeKeys -> showSizeDialog(key)
        }
    }

    private fun showSizeDialog(key: String) {
        NumberPickerDialog(requireContext())
            .setTitle(getString(CoverFontSettingsActions.titleRes(key)))
            .setMinValue(50)
            .setMaxValue(200)
            .setValue(CoverFontSettingsActions.sizeValue(requireContext(), key))
            .setCustomButton(R.string.btn_default_s) {
                CoverFontSettingsActions.saveSize(requireContext(), key, 100)
                onStyleChanged()
            }
            .show {
                CoverFontSettingsActions.saveSize(requireContext(), key, it)
                onStyleChanged()
            }
    }

    override val curFontPath: String get() = getPrefString(PreferKey.coverFont).orEmpty()

    override val selectSystemTypefaceOnDefault = false

    override fun selectFont(path: String) {
        fontJob?.cancel()
        if (path.isEmpty()) {
            putPrefString(PreferKey.coverFont, "")
            onStyleChanged()
            return
        }
        val directory = File(requireContext().externalFiles, "font")
        fontJob = lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val source = FileDoc.fromFile(path)
                    source.openInputStream().getOrThrow().use { input ->
                        installFontFile(input, source.name, directory) {
                            runCatching { Typeface.createFromFile(it) }.isSuccess
                        }.absolutePath
                    }
                }
            }
            result.onSuccess {
                putPrefString(PreferKey.coverFont, it)
                onStyleChanged()
            }.onFailure {
                requireContext().toastOnUi(it.localizedMessage)
            }
        }
    }
}
