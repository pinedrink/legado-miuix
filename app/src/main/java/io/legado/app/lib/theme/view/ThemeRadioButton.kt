package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import io.legado.app.R
import io.legado.app.utils.dpToPx
import io.legado.app.lib.theme.accentColor

class ThemeRadioButton(context: Context, attrs: AttributeSet) :
    AppCompatRadioButton(context, attrs) {

    private var isUserAction = false

    init {
        if (!isInEditMode) {
            setButtonDrawable(null)
            androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_miuix_radio)?.let {
                setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
            }
            compoundDrawablePadding = 10.dpToPx()
        }
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
