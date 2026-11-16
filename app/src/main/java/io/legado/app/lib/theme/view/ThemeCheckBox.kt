package io.legado.app.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatCheckBox
import io.legado.app.R
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getCompatColor

class ThemeCheckBox(context: Context, attrs: AttributeSet) : AppCompatCheckBox(context, attrs) {

    private var isUserAction = false
    private val boxSize = 20.dpToPx()

    init {
        if (!isInEditMode) {
            // Miuix 风格：自绘圆角方框 + 白色对勾，勾选态用主题主色
            setButtonDrawable(null)
            compoundDrawablePadding = 10.dpToPx()
            minHeight = 44.dpToPx()
            updateBox()
        }
    }

    private fun updateBox() {
        val stroke = context.getCompatColor(R.color.miuix_radio_stroke)
        val box = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6.dpToPx().toFloat()
            if (isChecked) {
                setColor(context.primaryColor)
            } else {
                setColor(Color.TRANSPARENT)
                setStroke(1.5f.dpToPx().toInt(), stroke)
            }
        }
        box.setBounds(0, 0, boxSize, boxSize)
        val layers = if (isChecked) {
            val check = AppCompatResources.getDrawable(context, R.drawable.ic_check_white)?.mutate()
            LayerDrawable(arrayOf(box, check)).apply {
                setLayerInset(1, 4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
            }
        } else {
            LayerDrawable(arrayOf(box))
        }
        layers.setBounds(0, 0, boxSize, boxSize)
        setCompoundDrawablesRelative(layers, null, null, null)
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        if (!isInEditMode) updateBox()
    }

    override fun performClick(): Boolean {
        isUserAction = true
        val result = super.performClick()
        isUserAction = false
        return result
    }

    fun setOnUserCheckedChangeListener(listener: ((Boolean) -> Unit)?) {
        if (listener == null) {
            return super.setOnCheckedChangeListener(null)
        }
        super.setOnCheckedChangeListener { _, isChecked ->
            if (isUserAction) {
                listener.invoke(isChecked)
            }
        }
    }

}
