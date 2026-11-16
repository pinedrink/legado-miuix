package io.legado.app.ui.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.TextView
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import android.graphics.drawable.GradientDrawable
import io.legado.app.R
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getCompatColor

@Suppress("unused", "MemberVisibilityCanBePrivate")
class LabelsBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FlexboxLayout(context, attrs) {

    private val unUsedViews = arrayListOf<TextView>()
    private val usedViews = arrayListOf<TextView>()
    var textSize = 12f

    fun setLabels(
        labels: List<String>,
        onClick: ((String) -> Unit)? = null,
        onLongClick: ((String) -> Boolean)? = null
    ) {
        clear()
        labels.forEach {
            addLabel(it, onClick, onLongClick)
        }
    }

    fun clear() {
        unUsedViews.addAll(usedViews)
        usedViews.clear()
        removeAllViews()
    }

    fun addLabel(label: String, onClick: ((String) -> Unit)?, onLongClick: ((String) -> Boolean)?) {
        val tv = if (unUsedViews.isEmpty()) {
            TextView(context).apply {
                setPadding(9.dpToPx(), 5.dpToPx(), 9.dpToPx(), 5.dpToPx())
                setTextColor(context.getCompatColor(R.color.primaryText))
                background = chipBackground()
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
                val lp = FlexboxLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 6.dpToPx()
                    if (this@LabelsBar.flexWrap == FlexWrap.NOWRAP) {
                        flexShrink = 0f
                    } else {
                        bottomMargin = 6.dpToPx()
                    }
                }
                layoutParams = lp
                text = label
                usedViews.add(this)
            }
        } else {
            unUsedViews.last().apply {
                usedViews.add(this)
                unUsedViews.removeAt(unUsedViews.lastIndex)
            }
        }
        tv.textSize = textSize
        tv.text = label
        tv.setOnClickListener(null)
        tv.setOnLongClickListener(null)
        tv.isClickable = false
        tv.isLongClickable = false
        if (onClick != null) {
            tv.setOnClickListener { onClick.invoke(label) }
        }
        if (onLongClick != null) {
            tv.setOnLongClickListener { onLongClick.invoke(label) }
        }
        addView(tv)
    }

    /** 标签底：中性灰胶囊，明暗主题各自取值，不再用强调色块 */
    private fun chipBackground(): GradientDrawable {
        val isLight = ColorUtils.isColorLight(context.getCompatColor(R.color.background))
        return GradientDrawable().apply {
            cornerRadius = 8.dpToPx().toFloat()
            setColor(if (isLight) 0x11000000 else 0x22FFFFFF)
        }
    }
}
