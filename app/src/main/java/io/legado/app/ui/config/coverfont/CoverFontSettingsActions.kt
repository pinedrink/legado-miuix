package io.legado.app.ui.config.coverfont

import android.content.Context
import androidx.annotation.StringRes
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.model.BookCover
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt
import java.io.File

/**
 * 默认书名样式设置的偏好读写。
 *
 * 逻辑全部迁移自原来的 `CoverFontConfigFragment`：
 * 字号项默认 100%，范围 50..200；任意样式项变化都要重刷默认封面并通知书架刷新。
 */
object CoverFontSettingsActions {

    val sizeKeys = listOf(
        PreferKey.coverTitleLargeSize,
        PreferKey.coverTitleSmallSize,
        PreferKey.coverAuthorLargeSize,
        PreferKey.coverAuthorSmallSize,
    )

    val styleKeys = sizeKeys + listOf(
        PreferKey.coverHorizontal,
        PreferKey.coverTitleAdaptive,
        PreferKey.coverKeepPunctuation,
        PreferKey.coverFont,
        PreferKey.coverCustomFontSize,
    )

    const val KEY_PREVIEW = "coverPreview"

    private val switchDefaults = mapOf(
        PreferKey.coverHorizontal to false,
        PreferKey.coverTitleAdaptive to true,
        PreferKey.coverKeepPunctuation to false,
        PreferKey.coverCustomFontSize to false,
    )

    private val switchTitles = mapOf(
        PreferKey.coverHorizontal to R.string.cover_horizontal,
        PreferKey.coverTitleAdaptive to R.string.cover_title_adaptive,
        PreferKey.coverKeepPunctuation to R.string.cover_keep_punctuation,
        PreferKey.coverCustomFontSize to R.string.cover_custom_font_size,
    )

    private val switchSummaries = mapOf(
        PreferKey.coverHorizontal to R.string.cover_horizontal_summary,
        PreferKey.coverTitleAdaptive to R.string.cover_title_adaptive_summary,
        PreferKey.coverKeepPunctuation to R.string.cover_keep_punctuation_summary,
        PreferKey.coverCustomFontSize to R.string.cover_custom_font_size_summary,
    )

    private val sizeTitles = mapOf(
        PreferKey.coverTitleLargeSize to R.string.cover_title_large_size,
        PreferKey.coverTitleSmallSize to R.string.cover_title_small_size,
        PreferKey.coverAuthorLargeSize to R.string.cover_author_large_size,
        PreferKey.coverAuthorSmallSize to R.string.cover_author_small_size,
    )

    /** 生成页面全部行，顺序与 pref_config_cover_font.xml 一致。 */
    fun entries(context: Context): List<CoverFontSettingEntry> {
        val customSize = context.getPrefBoolean(PreferKey.coverCustomFontSize, false)
        val rows = mutableListOf<CoverFontSettingEntry>()
        for (key in listOf(
            PreferKey.coverHorizontal,
            PreferKey.coverTitleAdaptive,
            PreferKey.coverKeepPunctuation,
        )) {
            rows.add(switchItem(context, key))
        }
        rows.add(
            CoverFontSettingEntry.Item(
                key = PreferKey.coverFont,
                title = context.getString(R.string.cover_font_select),
                summary = fontSummary(context),
            )
        )
        rows.add(switchItem(context, PreferKey.coverCustomFontSize))
        for (key in sizeKeys) {
            rows.add(
                CoverFontSettingEntry.Item(
                    key = key,
                    title = context.getString(requireNotNull(sizeTitles[key])),
                    summary = sizeSummary(context, key),
                    enabled = customSize,
                )
            )
        }
        rows.add(
            CoverFontSettingEntry.Item(
                key = KEY_PREVIEW,
                title = "",
                kind = CoverFontSettingEntry.Kind.Preview,
            )
        )
        return rows
    }

    private fun switchItem(context: Context, key: String): CoverFontSettingEntry.Item {
        val titleRes = requireNotNull(switchTitles[key])
        val summaryRes = switchSummaries[key]
        return CoverFontSettingEntry.Item(
            key = key,
            title = context.getString(titleRes),
            summary = summaryRes?.let { context.getString(it) },
            kind = CoverFontSettingEntry.Kind.Switch,
            checked = context.getPrefBoolean(key, switchDefaults[key] ?: false),
        )
    }

    fun fontSummary(context: Context): String {
        val path = context.getPrefString(PreferKey.coverFont).orEmpty()
        return path.takeIf { it.isNotBlank() }
            ?.let { File(it).name }
            ?: context.getString(R.string.default_font)
    }

    fun sizeValue(context: Context, key: String): Int =
        context.getPrefInt(key, 100).coerceIn(50, 200)

    private fun sizeSummary(context: Context, key: String): String = "${sizeValue(context, key)}%"

    fun titleRes(key: String): Int = sizeTitles[key] ?: switchTitles[key] ?: R.string.cover_font_select

    fun saveSwitch(context: Context, key: String, checked: Boolean) {
        context.putPrefBoolean(key, checked)
        onStyleChanged(context)
    }

    fun saveSize(context: Context, key: String, value: Int) {
        context.putPrefInt(key, value.coerceIn(50, 200))
        onStyleChanged(context)
    }

    /** 样式变化后重刷默认封面并通知书架，与原 onSharedPreferenceChanged 行为一致。 */
    fun onStyleChanged(context: Context) {
        BookCover.upDefaultCover()
        postEvent(EventBus.BOOKSHELF_REFRESH, "")
    }

}
