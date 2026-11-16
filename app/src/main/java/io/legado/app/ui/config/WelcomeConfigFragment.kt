package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.FragmentWelcomeConfigBinding
import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.newCallResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.lib.dialogs.selector
import io.legado.app.model.BookCover
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.ui.config.welcome.WelcomeSettingEntry
import io.legado.app.ui.config.welcome.WelcomeSettingsActions
import io.legado.app.ui.config.welcome.WelcomeSettingsScreen
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.utils.FileUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getPrefString
import io.legado.app.utils.inputStream
import io.legado.app.utils.putPrefString
import io.legado.app.utils.readUri
import io.legado.app.utils.removePref
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

/**
 * 欢迎页样式设置。
 *
 * 内容区已改为 Compose（[WelcomeSettingsScreen]），偏好读写与开关副作用在
 * [WelcomeSettingsActions] 里；选图、图片下载与存储继续复用原有实现，
 * 业务逻辑没有复制。
 */
class WelcomeConfigFragment : BaseFragment(R.layout.fragment_welcome_config), ConfigSearchable {

    private val binding by viewBinding(FragmentWelcomeConfigBinding::bind)
    private val listState = LazyListState()

    private var entries by mutableStateOf<List<WelcomeSettingEntry>>(emptyList())

    private val selectImage = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            it.value?.takeIf { key -> key in WelcomeSettingsActions.imageKeys }
                ?.let { key -> setWelcomeImageFromUri(key, uri) }
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        activity?.setTitle(R.string.welcome_style)
        refreshRows()
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            LegadoMiuixTheme {
                WelcomeSettingsScreen(
                    entries = entries,
                    listState = listState,
                    onItemClick = ::onItemClick,
                    onSwitchChange = { key, checked ->
                        WelcomeSettingsActions.saveSwitch(requireContext(), key, checked)
                        refreshRows()
                    },
                    onShowTimeChange = { value ->
                        WelcomeSettingsActions.saveShowTime(requireContext(), value)
                        refreshRows()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRows()
    }

    private fun refreshRows() {
        entries = WelcomeSettingsActions.entries(requireContext())
    }

    override fun searchConfig(query: String): List<ConfigSearchItem> {
        val text = query.trim()
        if (text.isEmpty()) return emptyList()
        return WelcomeSettingsActions.entries(requireContext())
            .filterIsInstance<WelcomeSettingEntry.Item>()
            .filter { it.matches(text) }
            .map { item ->
                ConfigSearchItem(item.title) {
                    listState.requestScrollToItem(groupIndexOf(item.key))
                    if (item.kind == WelcomeSettingEntry.Kind.Action) {
                        onItemClick(item.key)
                    }
                }
            }
    }

    private fun WelcomeSettingEntry.Item.matches(text: String): Boolean =
        title.contains(text, true) || summary?.contains(text, true) == true

    /** 分组标题与每张卡片各占一个 LazyColumn item，定位到包含该行的卡片。 */
    private fun groupIndexOf(key: String): Int {
        var groupIndex = 0
        var result = 0
        var pending = mutableListOf<WelcomeSettingEntry.Item>()
        fun flush() {
            if (pending.isNotEmpty()) {
                if (pending.any { it.key == key }) result = groupIndex
                groupIndex++
                pending = mutableListOf()
            }
        }
        for (entry in entries) {
            when (entry) {
                is WelcomeSettingEntry.Section -> {
                    flush()
                    groupIndex++
                }

                is WelcomeSettingEntry.Item -> pending.add(entry)
            }
        }
        flush()
        return result
    }

    private fun onItemClick(key: String) {
        when (key) {
            WelcomeSettingsActions.KEY_IMAGE,
            WelcomeSettingsActions.KEY_IMAGE_DARK -> selectWelcomeImage(key)
        }
    }

    @SuppressLint("PrivateResource")
    private fun selectWelcomeImage(key: String) {
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
                    deleteStoredImage(requireContext().getPrefString(key))
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

    private fun setWelcomeImageFromUri(preferenceKey: String, uri: Uri) {
        if (uri.scheme?.lowercase() in listOf("http", "https")) {
            lifecycleScope.launch {
                kotlin.runCatching {
                    appCtx.toastOnUi("下载图片中...")
                    val analyzeUrl = AnalyzeUrl(uri.toString())
                    val url = analyzeUrl.urlNoQuery
                    var file = requireContext().externalFiles
                    val res = okHttpClient.newCallResponse(0) {
                        addHeaders(analyzeUrl.headerMap)
                        url(url)
                    }
                    val contentType = res.header("Content-Type") ?: "image/jpeg"
                    val imageType = when {
                        contentType.contains("png", ignoreCase = true) -> "png"
                        contentType.contains("gif", ignoreCase = true) -> "gif"
                        contentType.contains("webp", ignoreCase = true) -> "webp"
                        else -> "jpg"
                    }
                    val suffix = if (url.contains(".9.png", true)) {
                        ".9.png"
                    } else {
                        ".$imageType"
                    }
                    val fileName = MD5Utils.md5Encode(url) + suffix
                    file = FileUtils.createFileIfNotExist(file, "covers", fileName)
                    res.body.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    val oldPath = requireContext().getPrefString(preferenceKey)
                    requireContext().putPrefString(preferenceKey, file.absolutePath)
                    if (oldPath != file.absolutePath) deleteStoredImage(oldPath)
                }.onSuccess {
                    appCtx.toastOnUi("设定成功")
                    refreshRows()
                }.onFailure {
                    appCtx.toastOnUi(it.localizedMessage)
                }
            }
            return
        }
        readUri(uri) { fileDoc, inputStream ->
            kotlin.runCatching {
                var file = requireContext().externalFiles
                val suffix = if (fileDoc.name.contains(".9.png", true)) {
                    ".9.png"
                } else {
                    "." + fileDoc.name.substringAfterLast(".")
                }
                val fileName = uri.inputStream(requireContext()).getOrThrow().use {
                    MD5Utils.md5Encode(it) + suffix
                }
                file = FileUtils.createFileIfNotExist(file, "covers", fileName)
                FileOutputStream(file).use {
                    inputStream.copyTo(it)
                }
                val oldPath = requireContext().getPrefString(preferenceKey)
                requireContext().putPrefString(preferenceKey, file.absolutePath)
                if (oldPath != file.absolutePath) deleteStoredImage(oldPath)
                refreshRows()
            }.onFailure {
                appCtx.toastOnUi(it.localizedMessage)
            }
        }
    }

    private fun deleteStoredImage(path: String?) {
        if (path.isNullOrEmpty()) return
        val file = File(path)
        val coversDir = File(requireContext().externalFiles, "covers")
        val isStoredImage = runCatching {
            file.canonicalFile.parentFile == coversDir.canonicalFile
        }.getOrDefault(false)
        if (isStoredImage) FileUtils.delete(file)
    }
}
