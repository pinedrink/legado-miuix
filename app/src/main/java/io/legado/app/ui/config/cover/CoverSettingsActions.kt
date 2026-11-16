package io.legado.app.ui.config.cover

import android.content.Context
import androidx.annotation.StringRes
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.model.BookCover
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefBoolean

/**
 * 封面设置的行清单与副作用。
 *
 * 这里的逻辑从原来的 [io.legado.app.ui.config.CoverConfigFragment]
 * （PreferenceFragment 版本）迁移而来，行为保持一致：
 * 开关变化后刷新默认封面并通知书架，封面图片只做偏好读写。
 */
object CoverSettingsActions {

    const val KEY_COVER_RULE = "coverRule"

    /** 与 `ConfigTag.COVER_FONT_CONFIG` 相同 */
    const val KEY_COVER_FONT_CONFIG = "coverFontConfig"

    val coverKeys = listOf(
        PreferKey.defaultCover,
        PreferKey.defaultCoverDark,
        PreferKey.readRecordCover,
        PreferKey.readRecordCoverDark
    )

    private val coverTitleRes = mapOf(
        PreferKey.defaultCover to R.string.default_cover,
        PreferKey.defaultCoverDark to R.string.default_cover,
        PreferKey.readRecordCover to R.string.read_record_cover_day,
        PreferKey.readRecordCoverDark to R.string.read_record_cover_night
    )

    fun entries(context: Context): List<CoverSettingEntry> = buildList {
        add(
            switchItem(
                context,
                PreferKey.loadCoverOnlyWifi,
                R.string.only_wifi,
                R.string.only_wifi_summary
            )
        )
        add(
            CoverSettingEntry.Item(
                key = KEY_COVER_RULE,
                title = context.getString(R.string.cover_rule),
                summary = context.getString(R.string.cover_rule_summary)
            )
        )
        add(
            switchItem(
                context,
                PreferKey.useDefaultCover,
                R.string.use_default_cover,
                R.string.use_default_cover_s
            )
        )
        add(
            CoverSettingEntry.Item(
                key = KEY_COVER_FONT_CONFIG,
                title = context.getString(R.string.cover_font_config)
            )
        )

        add(CoverSettingEntry.Section(R.string.day))
        add(coverItem(context, PreferKey.defaultCover))
        add(
            switchItem(
                context,
                PreferKey.coverShowName,
                R.string.cover_show_name,
                R.string.cover_show_name_summary
            )
        )
        add(
            switchItem(
                context,
                PreferKey.coverShowAuthor,
                R.string.cover_show_author,
                R.string.cover_show_author_summary,
                enabled = context.getPrefBoolean(PreferKey.coverShowName, true)
            )
        )

        add(CoverSettingEntry.Section(R.string.night))
        add(coverItem(context, PreferKey.defaultCoverDark))
        add(
            switchItem(
                context,
                PreferKey.coverShowNameN,
                R.string.cover_show_name,
                R.string.cover_show_name_summary
            )
        )
        add(
            switchItem(
                context,
                PreferKey.coverShowAuthorN,
                R.string.cover_show_author,
                R.string.cover_show_author_summary,
                enabled = context.getPrefBoolean(PreferKey.coverShowNameN, true)
            )
        )

        add(CoverSettingEntry.Section(R.string.read_record_covers))
        add(coverItem(context, PreferKey.readRecordCover))
        add(coverItem(context, PreferKey.readRecordCoverDark))
    }

    /**
     * 开关变化后的副作用，与原来的 onSharedPreferenceChanged 保持一致。
     */
    fun saveSwitch(context: Context, key: String, checked: Boolean) {
        context.putPrefBoolean(key, checked)
        when (key) {
            PreferKey.useDefaultCover,
            PreferKey.coverShowName,
            PreferKey.coverShowNameN,
            PreferKey.coverShowAuthor,
            PreferKey.coverShowAuthorN -> {
                BookCover.upDefaultCover()
                postEvent(EventBus.BOOKSHELF_REFRESH, "")
            }
        }
    }

    fun coverSummary(context: Context, key: String): String =
        context.getPrefString(key).takeUnless { it.isNullOrBlank() }
            ?: context.getString(R.string.select_image)

    private fun coverItem(context: Context, key: String): CoverSettingEntry.Item =
        CoverSettingEntry.Item(
            key = key,
            title = context.getString(coverTitleRes.getValue(key)),
            summary = coverSummary(context, key)
        )

    private fun switchItem(
        context: Context,
        key: String,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int? = null,
        enabled: Boolean = true
    ): CoverSettingEntry.Item = CoverSettingEntry.Item(
        key = key,
        title = context.getString(titleRes),
        summary = summaryRes?.let { context.getString(it) },
        kind = CoverSettingEntry.Kind.Switch,
        checked = context.getPrefBoolean(key, isDefaultTrue(key)),
        enabled = enabled
    )

    /** 原 XML 里 coverShowName/coverShowAuthor（含夜间）默认值为 true，其余开关默认 false。 */
    private fun isDefaultTrue(key: String): Boolean = when (key) {
        PreferKey.coverShowName,
        PreferKey.coverShowAuthor,
        PreferKey.coverShowNameN,
        PreferKey.coverShowAuthorN -> true

        else -> false
    }
}
