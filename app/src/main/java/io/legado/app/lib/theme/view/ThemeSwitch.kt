package io.legado.app.lib.theme.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.CompoundButton
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getCompatColor

/**
 * Miuix 风格的开关。
 *
 * 只负责绘制与动画，状态/监听仍走 [CompoundButton]，所以原有 `isChecked`、
 * `setOnCheckedChangeListener`、`setOnUserCheckedChangeListener` 用法不变。
 */
class ThemeSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.checkboxStyle,
) : CompoundButton(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val trackRect = RectF()

    private val switchWidth = 44.dpToPx()
    private val switchHeight = 26.dpToPx()
    private val thumbSize = 22.dpToPx()
    private val thumbInset = 2.dpToPx()
    private val gap = 8.dpToPx()

    private val reservedEnd = switchWidth + gap
    private var basePaddingEnd = 0
    private var fraction = if (isChecked) 1f else 0f
    private var animator: ValueAnimator? = null

    private val onTrackColor: Int
        get() = context.primaryColor

    private val offTrackColor: Int
        get() = if (ColorUtils.isColorLight(context.getCompatColor(io.legado.app.R.color.background))) {
            0x4D000000
        } else {
            0x4DFFFFFF
        }

    private var isUserAction = false

    init {
        // CompoundButton 默认会画一个 checkbox 方块，这里只要胶囊开关
        setButtonDrawable(null)
        basePaddingEnd = paddingEnd
        setPaddingRelative(paddingStart, paddingTop, basePaddingEnd + reservedEnd, paddingBottom)
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minHeight = switchHeight + paddingTop + paddingBottom
        if (measuredHeight < minHeight) {
            setMeasuredDimension(measuredWidth, minHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val right = (width - basePaddingEnd).toFloat()
        val left = right - switchWidth
        val top = (height - switchHeight) / 2f
        val bottom = top + switchHeight
        val radius = switchHeight / 2f

        val enabledAlpha = if (isEnabled) 0xFF else 0x66
        trackPaint.color = if (fraction > 0f) {
            blend(offTrackColor, onTrackColor, fraction).let { applyAlpha(it, enabledAlpha) }
        } else {
            applyAlpha(offTrackColor, enabledAlpha)
        }
        trackRect.set(left, top, right, bottom)
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        thumbPaint.alpha = if (isEnabled) 0xFF else 0x99
        val thumbCx = left + thumbInset + thumbSize / 2f +
            (switchWidth - thumbSize - thumbInset * 2) * fraction
        canvas.drawCircle(thumbCx, (top + bottom) / 2f, thumbSize / 2f, thumbPaint)
    }

    override fun setChecked(checked: Boolean) {
        val changed = checked != isChecked
        super.setChecked(checked)
        if (changed) {
            animateTo(if (checked) 1f else 0f)
        }
    }

    fun jumpDrawableToCurrentState() {
        animator?.cancel()
        fraction = if (isChecked) 1f else 0f
        invalidate()
    }

    override fun performClick(): Boolean {
        isUserAction = true
        val result = super.performClick()
        isUserAction = false
        return result
    }

    fun setOnUserCheckedChangeListener(listener: ((Boolean) -> Unit)?) {
        if (listener == null) {
            super.setOnCheckedChangeListener(null)
            return
        }
        super.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) {
                listener.invoke(isChecked)
            }
        }
    }

    private fun animateTo(target: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(fraction, target).apply {
            duration = 160
            addUpdateListener {
                fraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun booleanToFraction(checked: Boolean): Float = if (checked) 1f else 0f

    private fun blend(from: Int, to: Int, ratio: Float): Int {
        val startA = (from ushr 24) and 0xFF
        val startR = (from shr 16) and 0xFF
        val startG = (from shr 8) and 0xFF
        val startB = from and 0xFF
        val endA = (to ushr 24) and 0xFF
        val endR = (to shr 16) and 0xFF
        val endG = (to shr 8) and 0xFF
        val endB = to and 0xFF
        val a = (startA + (endA - startA) * ratio).toInt()
        val r = (startR + (endR - startR) * ratio).toInt()
        val g = (startG + (endG - startG) * ratio).toInt()
        val b = (startB + (endB - startB) * ratio).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun applyAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or ((color ushr 24) * alpha / 0xFF shl 24)
    }
}
