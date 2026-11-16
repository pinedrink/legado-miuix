package io.legado.app.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.graphics.ColorUtils
import io.legado.app.R
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getCompatColor

/**
 * Miuix 风格进度条：4dp 圆角轨道（未选中 12% 前景色）+ 主色进度 + 16dp 圆形滑块。
 */
class ThemeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatSeekBar(context, attrs) {

    private val trackHeight = 4.dpToPx()
    private val thumbSize = 16.dpToPx()

    init {
        if (!isInEditMode) {
            splitTrack = false
            thumbOffset = 0
            val padding = (thumbSize / 2)
            setPadding(padding, paddingTop, padding, paddingBottom)
            thumb = buildThumb()
            updateTrack(height)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!isInEditMode) updateTrack(h)
    }

    private fun buildThumb(): GradientDrawable {
        val thumb = GradientDrawable()
        thumb.shape = GradientDrawable.OVAL
        thumb.setColor(context.accentColor)
        thumb.setStroke(2.dpToPx(), Color.WHITE)
        thumb.setSize(thumbSize, thumbSize)
        return thumb
    }

    private fun updateTrack(viewHeight: Int) {
        val inactive = ColorUtils.setAlphaComponent(
            context.getCompatColor(R.color.primaryText), 30
        )
        val track = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = (trackHeight / 2).toFloat()
            setColor(inactive)
        }
        val progress = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = (trackHeight / 2).toFloat()
            setColor(context.accentColor)
        }
        val layer = LayerDrawable(arrayOf(track, progress)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
        val inset = ((viewHeight - trackHeight) / 2).coerceAtLeast(0)
        progressDrawable = InsetDrawable(layer, 0, inset, 0, inset)
    }
}
