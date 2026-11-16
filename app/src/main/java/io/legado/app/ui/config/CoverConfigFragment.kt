package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.databinding.FragmentCoverConfigBinding
import io.legado.app.lib.dialogs.selector
import io.legado.app.model.BookCover
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.ui.config.cover.CoverSettingEntry
import io.legado.app.ui.config.cover.CoverSettingsActions
import io.legado.app.ui.config.cover.CoverSettingsScreen
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.utils.FileUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getPrefString
import io.legado.app.utils.inputStream
import io.legado.app.utils.putPrefString
import io.legado.app.utils.readUri
import io.legado.app.utils.removePref
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import java.io.FileOutputStream

/**
 * 封面设置页。
 *
 * 内容区已改为 Compose（[CoverSettingsScreen]），偏好读写与副作用在
 * [CoverSettingsActions] 里；选图、封面规则对话框、封面字体设置页继续复用原有实现，
 * 业务逻辑没有复制。
 */
class CoverConfigFragment : BaseFragment(R.layout.fragment_cover_config), ConfigSearchable {

    private val binding by viewBinding(FragmentCoverConfigBinding::bind)
    private val listState = LazyListState()

    private var entries by mutableStateOf<List<CoverSettingEntry>>(emptyList())

    private val selectImage = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            it.value?.takeIf { key -> key in CoverSettingsActions.coverKeys }
                ?.let { key -> setCoverFromUri(key, uri) }
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        activity?.setTitle(R.string.cover_config)
        refreshRows()
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            LegadoMiuixTheme {
                CoverSettingsScreen(
                    entries = entries,
                    onItemClick = ::onItemClick,
                    onSwitchChange = { key, checked ->
                        CoverSettingsActions.saveSwitch(requireContext(), key, checked)
                        refreshRows()
                    },
                    listState = listState,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRows()
    }

    private fun refreshRows() {
        entries = CoverSettingsActions.entries(requireContext())
    }

    override fun searchConfig(query: String): List<ConfigSearchItem> {
        val text = query.trim()
        if (text.isEmpty()) return emptyList()
        return CoverSettingsActions.entries(requireContext())
            .filterIsInstance<CoverSettingEntry.Item>()
            .filter { it.matches(text) }
            .map { item ->
                ConfigSearchItem(item.title) {
                    listState.requestScrollToItem(groupIndexOf(item.key))
                    onItemClick(item.key)
                }
            }
    }

    private fun CoverSettingEntry.Item.matches(text: String): Boolean =
        title.contains(text, true) || summary?.contains(text, true) == true

    /** 分组标题与每张卡片各占一个 LazyColumn item，定位到包含该行的卡片。 */
    private fun groupIndexOf(key: String): Int {
        var groupIndex = 0
        var result = 0
        var pending = mutableListOf<CoverSettingEntry.Item>()
        fun flush() {
            if (pending.isNotEmpty()) {
                if (pending.any { it.key == key }) result = groupIndex
                groupIndex++
                pending = mutableListOf()
            }
        }
        for (entry in entries) {
            when (entry) {
                is CoverSettingEntry.Section -> {
                    flush()
                    groupIndex++
                }

                is CoverSettingEntry.Item -> pending.add(entry)
            }
        }
        flush()
        return result
    }

    private fun onItemClick(key: String) {
        when (key) {
            CoverSettingsActions.KEY_COVER_RULE -> showDialogFragment(CoverRuleConfigDialog())
            CoverSettingsActions.KEY_COVER_FONT_CONFIG -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.COVER_FONT_CONFIG)
            }

            in CoverSettingsActions.coverKeys -> selectCover(key)
        }
    }

    @SuppressLint("PrivateResource")
    private fun selectCover(key: String) {
        if (requireContext().getPrefString(key).isNullOrEmpty()) {
            launchImagePicker(key)
        } else {
            context?.selector(
                items = arrayListOf(
                    getString(R.string.delete),
                    getString(R.string.select_image)
                )
            ) { _, index ->
                if (index == 0) {
                    requireContext().removePref(key)
                    BookCover.upDefaultCover()
                    refreshRows()
                } else {
                    launchImagePicker(key)
                }
            }
        }
    }

    private fun launchImagePicker(key: String) {
        selectImage.launch {
            value = key
            mode = HandleFileContract.IMAGE
        }
    }

    private fun setCoverFromUri(preferenceKey: String, uri: Uri) {
        readUri(uri) { fileDoc, inputStream ->
            kotlin.runCatching {
                val dir = requireContext().externalFiles
                val suffix = if (fileDoc.name.contains(".9.png", true)) {
                    ".9.png"
                } else {
                    "." + fileDoc.name.substringAfterLast(".")
                }
                val fileName = uri.inputStream(requireContext()).getOrThrow().use {
                    MD5Utils.md5Encode(it) + suffix
                }
                val file = FileUtils.createFileIfNotExist(dir, "covers", fileName)
                FileOutputStream(file).use {
                    inputStream.copyTo(it)
                }
                requireContext().putPrefString(preferenceKey, file.absolutePath)
                BookCover.upDefaultCover()
                refreshRows()
            }.onFailure {
                appCtx.toastOnUi(it.localizedMessage)
            }
        }
    }
}
