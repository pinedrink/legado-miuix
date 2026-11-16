package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

/**
 * 上限为屏幕高度 90% 的 NestedScrollView：内容不足时按内容高度 wrap，
 * 超过时封顶并可滚动（用于阅读器底部弹窗）。
 */
class MaxHeightNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : NestedScrollView(context, attrs, defStyleAttr) {

    private val maxHeight: Int =
        (context.resources.displayMetrics.heightPixels * 0.9f).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, spec)
    }
}
