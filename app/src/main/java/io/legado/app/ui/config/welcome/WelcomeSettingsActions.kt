package io.legado.app.ui.config.welcome

import android.content.Context
import androidx.annotation.StringRes
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt

/**
 * 欢迎页样式设置的行定义与读写逻辑。
 *
 * 全部行为都从原来的 `WelcomeConfigFragment`（PreferenceFragment）迁移而来，
 * 不改变任何偏好 key、默认值与副作用。
 */
object WelcomeSettingsActions {

    /** 欢迎页显示时长，单位毫秒，0 表示不显示欢迎页。 */
    const val KEY_SHOW_TIME = PreferKey.welcomeShowTime

    /** 自定义欢迎页开关。 */
    const val KEY_CUSTOM_WELCOME = PreferKey.customWelcome

    const val KEY_IMAGE = PreferKey.welcomeImage
    const val KEY_IMAGE_DARK = PreferKey.welcomeImageDark

    private const val SHOW_TIME_DEFAULT = 500
    private const val SHOW_TIME_MAX = 800

    /** 图片类行，点击后要么选图，要么弹出“删除/重新选择”。 */
    val imageKeys = listOf(KEY_IMAGE, KEY_IMAGE_DARK)

    /** 生成页面全部行，顺序与 pref_config_welcome.xml 一致。 */
    fun entries(context: Context): List<WelcomeSettingEntry> = buildList {
        add(
            WelcomeSettingEntry.Item(
                key = KEY_SHOW_TIME,
                title = context.getString(R.string.welcome_show_time),
                summary = context.getString(R.string.welcome_show_time_summary),
                kind = WelcomeSettingEntry.Kind.Slider,
                value = showTime(context),
                valueRange = 0..SHOW_TIME_MAX,
            )
        )
        add(
            WelcomeSettingEntry.Item(
                key = KEY_CUSTOM_WELCOME,
                title = context.getString(R.string.custom_welcome),
                summary = context.getString(R.string.custom_welcome_summary),
                kind = WelcomeSettingEntry.Kind.Switch,
                checked = context.getPrefBoolean(KEY_CUSTOM_WELCOME, false),
            )
        )

        add(WelcomeSettingEntry.Section(R.string.day))
        add(imageItem(context, KEY_IMAGE))
        add(
            WelcomeSettingEntry.Item(
                key = PreferKey.welcomeShowText,
                title = context.getString(R.string.show_welcome_text),
                summary = context.getString(R.string.welcome_text),
                kind = WelcomeSettingEntry.Kind.Switch,
                checked = context.getPrefBoolean(PreferKey.welcomeShowText, true),
            )
        )
        add(
            WelcomeSettingEntry.Item(
                key = PreferKey.welcomeShowIcon,
                title = context.getString(R.string.show_icon),
                summary = context.getString(R.string.show_default_book_icon),
                kind = WelcomeSettingEntry.Kind.Switch,
                checked = context.getPrefBoolean(PreferKey.welcomeShowIcon, true),
            )
        )

        add(WelcomeSettingEntry.Section(R.string.night))
        add(imageItem(context, KEY_IMAGE_DARK))
        add(
            WelcomeSettingEntry.Item(
                key = PreferKey.welcomeShowTextDark,
                title = context.getString(R.string.show_welcome_text),
                summary = context.getString(R.string.welcome_text),
                kind = WelcomeSettingEntry.Kind.Switch,
                checked = context.getPrefBoolean(PreferKey.welcomeShowTextDark, true),
            )
        )
        add(
            WelcomeSettingEntry.Item(
                key = PreferKey.welcomeShowIconDark,
                title = context.getString(R.string.show_icon),
                summary = context.getString(R.string.show_default_book_icon),
                kind = WelcomeSettingEntry.Kind.Switch,
                checked = context.getPrefBoolean(PreferKey.welcomeShowIconDark, true),
            )
        )
    }

    /** 欢迎页停留时长。 */
    fun showTime(context: Context): Int =
        context.getPrefInt(KEY_SHOW_TIME, SHOW_TIME_DEFAULT).coerceIn(0, SHOW_TIME_MAX)

    /** 拖动滑杆：实时写偏好，取值与原 SeekBarPreference 的 0..800 一致。 */
    fun saveShowTime(context: Context, value: Int) {
        context.putPrefInt(KEY_SHOW_TIME, value.coerceIn(0, SHOW_TIME_MAX))
    }

    /** 开关行：与原 SharedPreferences 监听里的赋值一一对应。 */
    fun saveSwitch(context: Context, key: String, checked: Boolean) {
        when (key) {
            KEY_CUSTOM_WELCOME -> context.putPrefBoolean(KEY_CUSTOM_WELCOME, checked)
            PreferKey.welcomeShowText -> AppConfig.welcomeShowText = checked
            PreferKey.welcomeShowTextDark -> AppConfig.welcomeShowTextDark = checked
            PreferKey.welcomeShowIcon -> AppConfig.welcomeShowIcon = checked
            PreferKey.welcomeShowIconDark -> AppConfig.welcomeShowIconDark = checked
        }
    }

    /** 已选图片的摘要：没选过就显示“选择图片”。 */
    fun imageSummary(context: Context, key: String): String {
        val path = context.getPrefString(key).orEmpty()
        return if (path.isBlank()) context.getString(R.string.select_image) else path
    }

    private fun imageItem(context: Context, key: String): WelcomeSettingEntry.Item =
        WelcomeSettingEntry.Item(
            key = key,
            title = context.getString(R.string.background_image),
            summary = imageSummary(context, key),
        )
}
