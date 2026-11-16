package io.legado.app.utils

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import android.widget.PopupWindow
import io.legado.app.R
import io.legado.app.lib.theme.popupBackground

fun PopupWindow.applyMd3PopupStyle() {
    // Miuix 风格：透明窗口背景 + 16dp 圆角 surface 底、无阴影，与 Compose 顶栏溢出菜单一致。
    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    elevation = 0f
    contentView?.let {
        it.background = ContextCompat.getDrawable(it.context, R.drawable.bg_popup_menu)
    }
    isClippingEnabled = true
}
