package io.legado.app.utils

import android.graphics.drawable.GradientDrawable

/**
 * Miuix 风格底部弹窗背景：顶部圆角、底部直角。
 */
fun roundedTopDrawable(color: Int, radiusPx: Float): GradientDrawable {
    return GradientDrawable().apply {
        setColor(color)
        cornerRadii = floatArrayOf(
            radiusPx, radiusPx,
            radiusPx, radiusPx,
            0f, 0f,
            0f, 0f,
        )
    }
}
