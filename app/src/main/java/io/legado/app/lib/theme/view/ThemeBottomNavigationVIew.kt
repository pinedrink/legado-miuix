package io.legado.app.lib.theme.view

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.WindowInsets
import androidx.core.view.doOnAttach
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.shape.ShapeAppearanceModel
import io.legado.app.R
import io.legado.app.databinding.ViewNavigationBadgeBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.getSecondaryTextColor
import io.legado.app.lib.theme.transparentNavBar
import io.legado.app.ui.widget.text.BadgeView
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getCompatColor

class ThemeBottomNavigationVIew(context: Context, attrs: AttributeSet) :
    BottomNavigationView(context, attrs) {

    private val themeIconTint: ColorStateList

    /** 是否允许显示 Miuix 胶囊指示器（E-Ink / 透明底栏 / 自定义图集皮肤时关闭） */
    private var miuixIndicatorAllowed = false

    /** menu id -> 默认矢量图标; applySkin(null) 时据此还原 */
    private val defaultIcons = mapOf(
        R.id.menu_bookshelf to R.drawable.ic_bottom_books,
        R.id.menu_discovery to R.drawable.ic_bottom_explore,
        R.id.menu_rss to R.drawable.ic_bottom_rss_feed,
        R.id.menu_my_config to R.drawable.ic_bottom_person,
    )

    init {
        val transparentNavBar = context.transparentNavBar
        val bgColor = if (transparentNavBar) {
            context.backgroundColor
        } else {
            // Miuix 风格：底栏与卡片同级，用 miuix_item_surface（日间 #FFFFFF / 夜间 #303030），
            // 不再使用主题里的 bottomBackground。
            context.getCompatColor(R.color.miuix_item_surface)
        }
        if (transparentNavBar) {
            setBackgroundColor(Color.TRANSPARENT)
        } else {
            // 悬浮式底栏：圆角卡片 + 投影，由 activity_main 留出四周间距
            setBackgroundResource(R.drawable.bg_miuix_floating_nav)
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = 8.dpToPx().toFloat()
            // 圆角跟随设备屏幕圆角，避免和屏幕 R 角弧度不一致
            doOnAttach {
                background = GradientDrawable().apply {
                    cornerRadius = deviceCornerRadius().toFloat()
                    setColor(bgColor)
                }
            }
        }
        val textIsDark = ColorUtils.isColorLight(bgColor)
        val textColor = context.getSecondaryTextColor(textIsDark)
        val colorStateList = Selector.colorBuild()
            .setDefaultColor(textColor)
            .setSelectedColor(ThemeStore.accentColor(context))
            .create()
        themeIconTint = colorStateList
        itemIconTintList = colorStateList
        itemTextColor = colorStateList
        miuixIndicatorAllowed = !AppConfig.isEInkMode && !transparentNavBar
        applyMiuixActiveIndicator(bgColor, miuixIndicatorAllowed)
        if (AppConfig.isEInkMode || transparentNavBar) {
            isItemHorizontalTranslationEnabled = false
            itemBackground = Color.TRANSPARENT.toDrawable()
        }

        ViewCompat.setOnApplyWindowInsetsListener(this, null)
    }

    /**
     * Miuix 风格选中项：图标背后一颗全圆角胶囊。
     *
     * 项目里没有 Miuix 的 primaryContainer 概念，这里用"底栏底色与主题强调色混合"得到
     * 一颗低饱和胶囊色，亮/暗两种底都能成立，也不会盖住图标。
     * E-Ink 与透明底栏保持原有行为（不显示胶囊）。
     */
    /** 取设备屏幕圆角半径；取不到时退回 28dp，并夹在 24~40dp 之间。 */
    private fun deviceCornerRadius(): Int {
        val fallback = 14.dpToPx()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return fallback
        val insets = rootWindowInsets ?: return fallback
        // 2 = CORNER_BOTTOM_RIGHT，3 = CORNER_BOTTOM_LEFT（WindowInsets.RoundedCorner 常量，API 31+）
        val radius = insets.getRoundedCorner(3)?.radius
            ?: insets.getRoundedCorner(2)?.radius
            ?: return fallback
        return radius.coerceIn(12.dpToPx(), 16.dpToPx())
    }

    private fun applyMiuixActiveIndicator(bgColor: Int, enabled: Boolean) {
        isItemActiveIndicatorEnabled = enabled
        if (!enabled) return
        itemActiveIndicatorColor = ColorStateList.valueOf(
            ColorUtils.blendColors(bgColor, ThemeStore.accentColor(context), 0.14f)
        )
        itemActiveIndicatorShapeAppearance = ShapeAppearanceModel.builder()
            .setAllCornerSizes(12.dpToPx().toFloat())
            .build()
        itemActiveIndicatorHeight = 32.dpToPx()
    }

    /**
     * 应用/取消底栏图集。
     * @param iconMap menu id -> StateListDrawable; 传 null 表示恢复默认主题图标
     * @param iconSizePx 有皮肤时的图标尺寸(像素)
     */
    fun applySkin(iconMap: Map<Int, StateListDrawable>?, iconSizePx: Int) {
        // 自定义图集是整块 PNG 图标，再叠 Miuix 胶囊会互相干扰，有皮肤时关闭胶囊。
        isItemActiveIndicatorEnabled = miuixIndicatorAllowed && iconMap == null
        if (iconMap == null) {
            itemIconTintList = themeIconTint
            itemIconSize = 24.dpToPx()
            defaultIcons.forEach { (id, res) ->
                menu.findItem(id)?.icon = defaultIcon(res, tinted = false)
            }
        } else {
            itemIconTintList = null
            itemIconSize = iconSizePx
            defaultIcons.keys.forEach { id ->
                menu.findItem(id)?.icon =
                    iconMap[id] ?: defaultIcon(defaultIcons.getValue(id), tinted = true)
            }
        }
    }

    private fun defaultIcon(resId: Int, tinted: Boolean) =
        ContextCompat.getDrawable(context, resId)?.mutate()?.apply {
            if (tinted) DrawableCompat.setTintList(this, themeIconTint)
        }

    fun addBadgeView(index: Int): BadgeView {
        //获取底部菜单view
        val menuView = getChildAt(0) as ViewGroup
        //获取第index个itemView
        val itemView = menuView.getChildAt(index) as ViewGroup
        val badgeBinding = ViewNavigationBadgeBinding.inflate(LayoutInflater.from(context))
        itemView.addView(badgeBinding.root)
        return badgeBinding.viewBadge
    }

}
