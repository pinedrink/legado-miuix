package io.legado.app.ui.config

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorInt
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.jaredrummler.android.colorpicker.ColorShape
import io.legado.app.R
import io.legado.app.base.AppContextWrapper
import io.legado.app.base.BaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.FragmentThemeConfigBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ThemeConfig
import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.newCallResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.prefs.ColorPreference
import io.legado.app.lib.prefs.IconListPreference
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.ui.config.theme.ThemeSettingEntry
import io.legado.app.ui.config.theme.ThemeSettingsActions
import io.legado.app.ui.config.theme.ThemeSettingsScreen
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.utils.FileUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.applyTint
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.inputStream
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.putPrefString
import io.legado.app.utils.readUri
import io.legado.app.utils.removePref
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import splitties.init.appCtx
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * 主题设置（阶段 D）。
 *
 * 界面由 Compose + Miuix 渲染，偏好读写与应用主题仍走 [ThemeSettingsActions]；
 * 颜色/数字/壁纸/主题列表等弹窗暂时复用原有实现，行为与迁移前一致。
 */
class ThemeConfigFragment : BaseFragment(R.layout.fragment_theme_config), MenuProvider,
    ColorPickerDialogListener, ConfigSearchable {

    private val binding by viewBinding(FragmentThemeConfigBinding::bind)

    private val requestCodeBgLight = 121
    private val requestCodeBgDark = 122

    private var entries by mutableStateOf<List<ThemeSettingEntry>>(emptyList())
    private val listState = LazyListState()

    /** 正在等待颜色结果的偏好 key。 */
    private var pendingColorKey: String? = null

    private val selectImage = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                requestCodeBgLight -> setBgFromUri(uri, PreferKey.bgImage) {
                    upTheme(false)
                    refreshRows()
                }

                requestCodeBgDark -> setBgFromUri(uri, PreferKey.bgImageN) {
                    upTheme(true)
                    refreshRows()
                }
            }
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        activity?.setTitle(R.string.theme_setting)
        activity?.addMenuProvider(this, viewLifecycleOwner)
        refreshRows()
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LegadoMiuixTheme {
                    ThemeSettingsScreen(
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

    private fun refreshRows() {
        context?.let { entries = ThemeSettingsActions.entries(it) }
    }

    /**
     * 找回迁移前 Preference 页面自带的搜索能力：按标题/摘要过滤，选中后滚动到对应分组并触发点击。
     */
    override fun searchConfig(query: String): List<ConfigSearchItem> {
        val context = context ?: return emptyList()
        val all = ThemeSettingsActions.entries(context)
        return all.mapIndexedNotNull { index, entry ->
            if (entry !is ThemeSettingEntry.Item) return@mapIndexedNotNull null
            val title = context.getString(entry.titleRes)
            val matches = title.contains(query, ignoreCase = true) ||
                entry.summary?.contains(query, ignoreCase = true) == true
            if (!matches) return@mapIndexedNotNull null
            ConfigSearchItem(title) {
                // requestScrollToItem 是非挂起 API，可以在 Compose 组合之外安全调用；
                // 用 animateScrollToItem 需要 MonotonicFrameClock，在 lifecycleScope 里会崩。
                listState.requestScrollToItem(groupIndexOf(all, index))
                onItemClick(entry.key)
            }
        }
    }

    private fun groupIndexOf(entries: List<ThemeSettingEntry>, entryIndex: Int): Int {
        var group = 0
        for (i in 1..entryIndex) {
            val prevIsItem = entries[i - 1] is ThemeSettingEntry.Item
            val currentIsItem = entries[i] is ThemeSettingEntry.Item
            if (prevIsItem != currentIsItem) group++
        }
        return group
    }

    private fun onSwitchChange(key: String, checked: Boolean) {
        val context = context ?: return
        if (!ThemeSettingsActions.onSwitchChanged(context, key, checked)) {
            return
        }
        refreshRows()
    }

    private fun onItemClick(key: String) {
        val context = context ?: return
        when (key) {
            ThemeSettingsActions.KEY_LAUNCHER_ICON -> showIconDialog()
            ThemeSettingsActions.KEY_WELCOME_STYLE -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.WELCOME_CONFIG)
            }

            PreferKey.barElevation -> showElevationDialog()
            PreferKey.fontScale -> showFontScaleDialog()
            ThemeSettingsActions.KEY_COVER_CONFIG -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.COVER_CONFIG)
            }

            ThemeSettingsActions.KEY_THEME_LIST -> ThemeListDialog().show(
                childFragmentManager,
                "themeList"
            )

            ThemeSettingsActions.KEY_BOTTOM_BAR_SKIN -> startActivity<BottomBarSkinActivity>()
            PreferKey.bgImage -> selectBgAction(false)
            PreferKey.bgImageN -> selectBgAction(true)
            ThemeSettingsActions.KEY_SAVE_DAY_THEME -> alertSaveTheme(ThemeSettingsActions.KEY_SAVE_DAY_THEME)
            ThemeSettingsActions.KEY_SAVE_NIGHT_THEME -> alertSaveTheme(ThemeSettingsActions.KEY_SAVE_NIGHT_THEME)
            else -> if (ThemeSettingsActions.isColorKey(key)) {
                showColorPicker(key)
            } else {
                context.toastOnUi("$key") // 理论上不会到这里
            }
        }
    }

    private fun showColorPicker(key: String) {
        pendingColorKey = key
        val context = context ?: return
        val dialog = ColorPreference.ColorPickerDialogCompat.newBuilder()
            .setDialogType(ColorPickerDialog.TYPE_PRESETS)
            .setDialogTitle(com.jaredrummler.android.colorpicker.R.string.cpv_default_title)
            .setColorShape(ColorShape.CIRCLE)
            .setPresets(ColorPickerDialog.MATERIAL_COLORS)
            .setAllowPresets(true)
            .setAllowCustom(true)
            .setShowAlphaSlider(false)
            .setShowColorShades(true)
            .setColor(ThemeSettingsActions.colorDialogSeed(context, key))
            .create()
        dialog.setColorPickerDialogListener(this)
        childFragmentManager.beginTransaction()
            .add(dialog, "color_$key")
            .commitAllowingStateLoss()
    }

    override fun onColorSelected(dialogId: Int, @ColorInt color: Int) {
        val key = pendingColorKey ?: return
        pendingColorKey = null
        val context = context ?: return
        // 应用颜色可能会触发 Activity 重建（ThemeConfig.applyTheme + RECREATE）。
        // 颜色弹窗此刻还没走完自己的 dismiss，如果直接重建会被 FragmentManager 还原出来，
        // 所以先同步移除它，再应用颜色。
        removeColorDialog(key)
        ThemeSettingsActions.onColorPicked(context, key, color)
        refreshRows()
    }

    private fun removeColorDialog(key: String) {
        val tag = "color_$key"
        val dialog = childFragmentManager.findFragmentByTag(tag) ?: return
        childFragmentManager.beginTransaction().remove(dialog).commitNowAllowingStateLoss()
    }

    override fun onDialogDismissed(dialogId: Int) {
        pendingColorKey = null
    }

    private fun showIconDialog() {
        val context = context ?: return
        val names = context.resources.getTextArray(R.array.icon_names)
        val values = context.resources.getTextArray(R.array.icons)
        val dialog = IconListPreference.IconDialog().apply {
            arguments = Bundle().apply {
                putString("value", getPrefString(ThemeSettingsActions.KEY_LAUNCHER_ICON))
                putCharSequenceArray("entries", names)
                putCharSequenceArray("entryValues", values)
                putCharSequenceArray("iconNames", values)
            }
            onChanged = { value ->
                ThemeSettingsActions.applyLauncherIcon(context, value)
                refreshRows()
            }
        }
        childFragmentManager.beginTransaction()
            .add(dialog, "icon_${ThemeSettingsActions.KEY_LAUNCHER_ICON}")
            .commitAllowingStateLoss()
    }

    private fun showElevationDialog() {
        NumberPickerDialog(requireContext())
            .setTitle(getString(R.string.bar_elevation))
            .setMaxValue(32)
            .setMinValue(0)
            .setValue(AppConfig.elevation)
            .setCustomButton(R.string.btn_default_s) {
                AppConfig.elevation = AppConst.sysElevation
                recreateActivities()
            }
            .show {
                AppConfig.elevation = it
                recreateActivities()
            }
    }

    private fun showFontScaleDialog() {
        NumberPickerDialog(requireContext())
            .setTitle(getString(R.string.font_scale))
            .setMaxValue(16)
            .setMinValue(8)
            .setValue(
                (AppContextWrapper.getFontScale(requireContext()) * 10)
                    .roundToInt()
                    .coerceIn(8, 16)
            )
            .setCustomButton(R.string.btn_default_s) {
                putPrefInt(PreferKey.fontScale, 0)
                recreateActivities()
            }
            .show {
                putPrefInt(PreferKey.fontScale, it)
                recreateActivities()
            }
    }

    private fun alertSaveTheme(key: String) {
        alert(R.string.theme_name) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "name"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.takeIf { it.isNotBlank() }?.let { name ->
                    when (key) {
                        ThemeSettingsActions.KEY_SAVE_DAY_THEME ->
                            ThemeConfig.saveDayTheme(requireContext(), name)

                        ThemeSettingsActions.KEY_SAVE_NIGHT_THEME ->
                            ThemeConfig.saveNightTheme(requireContext(), name)
                    }
                }
            }
            cancelButton()
        }
    }

    private fun selectBgAction(isNight: Boolean) {
        val bgKey = if (isNight) PreferKey.bgImageN else PreferKey.bgImage
        val blurringKey = if (isNight) PreferKey.bgImageNBlurring else PreferKey.bgImageBlurring
        val actions = arrayListOf(
            getString(R.string.background_image_blurring),
            getString(R.string.select_image)
        )
        if (!getPrefString(bgKey).isNullOrEmpty()) {
            actions.add(getString(R.string.delete))
        }
        context?.selector(items = actions) { _, i ->
            when (i) {
                0 -> alertImageBlurring(blurringKey) {
                    upTheme(isNight)
                    refreshRows()
                }

                1 -> {
                    if (isNight) {
                        selectImage.launch {
                            requestCode = requestCodeBgDark
                            mode = HandleFileContract.IMAGE
                        }
                    } else {
                        selectImage.launch {
                            requestCode = requestCodeBgLight
                            mode = HandleFileContract.IMAGE
                        }
                    }
                }

                2 -> {
                    removePref(bgKey)
                    upTheme(isNight)
                    refreshRows()
                }
            }
        }
    }

    private fun alertImageBlurring(preferKey: String, success: () -> Unit) {
        val progress = getPrefInt(preferKey, 0)
        alert(R.string.background_image_blurring) {
            val alertBinding = io.legado.app.databinding.DialogImageBlurringBinding
                .inflate(layoutInflater).apply {
                    seekBar.progress = progress
                    textViewValue.text = progress.toString()
                    seekBar.setOnSeekBarChangeListener(
                        object : io.legado.app.ui.widget.seekbar.SeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: android.widget.SeekBar,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                textViewValue.text = progress.toString()
                            }
                        }
                    )
                }
            customView { alertBinding.root }
            okButton {
                putPrefInt(preferKey, alertBinding.seekBar.progress)
                success.invoke()
            }
            cancelButton()
        }
    }

    private fun upTheme(isNightTheme: Boolean) {
        if (AppConfig.isNightTheme == isNightTheme) {
            binding.root.post {
                ThemeConfig.applyTheme(requireContext())
                recreateActivities()
            }
        }
    }

    private fun recreateActivities() {
        postEvent(EventBus.RECREATE, "")
    }

    private fun setBgFromUri(uri: Uri, preferenceKey: String, success: () -> Unit) {
        val context = context ?: return
        if (uri.scheme?.lowercase() in listOf("http", "https")) {
            lifecycleScope.launch {
                kotlin.runCatching {
                    appCtx.toastOnUi("下载背景图片中...")
                    val analyzeUrl = AnalyzeUrl(uri.toString())
                    val url = analyzeUrl.urlNoQuery
                    var file = context.externalFiles
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
                    file = FileUtils.createFileIfNotExist(file, preferenceKey, fileName)
                    res.body.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    putPrefString(preferenceKey, file.absolutePath)
                    if (isAdded && context != null) {
                        success()
                    }
                }.onSuccess {
                    appCtx.toastOnUi("设定成功")
                }.onFailure {
                    appCtx.toastOnUi(it.localizedMessage)
                }
            }
            return
        }
        readUri(uri) { fileDoc, inputStream ->
            kotlin.runCatching {
                var file = context.externalFiles
                val suffix = if (fileDoc.name.contains(".9.png", true)) {
                    ".9.png"
                } else {
                    "." + fileDoc.name.substringAfterLast(".")
                }
                val fileName = uri.inputStream(requireContext()).getOrThrow().use {
                    MD5Utils.md5Encode(it) + suffix
                }
                file = FileUtils.createFileIfNotExist(file, preferenceKey, fileName)
                FileOutputStream(file).use {
                    inputStream.copyTo(it)
                }
                putPrefString(preferenceKey, file.absolutePath)
                success()
            }.onFailure {
                appCtx.toastOnUi(it.localizedMessage)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.theme_config, menu)
        context?.let { menu.applyTint(it) }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.menu_theme_mode) {
            AppConfig.isNightTheme = !AppConfig.isNightTheme
            ThemeConfig.applyDayNight(requireContext())
            return true
        }
        return false
    }
}
