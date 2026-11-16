package io.legado.app.ui.widget.text

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.getCompatColor

class TextInputLayout(context: Context, attrs: AttributeSet?) : TextInputLayout(context, attrs) {

    init {
        if (!isInEditMode) {
            // Miuix 风格：去掉 Material 的填充框/下划线，仅保留底部细分隔线
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_NONE
            setHintTextAppearance(R.style.MiuixFormHint)
            defaultHintTextColor = ColorStateList.valueOf(context.getCompatColor(R.color.secondaryText))
            setBackgroundResource(R.drawable.bg_form_row)
        }
    }

}
