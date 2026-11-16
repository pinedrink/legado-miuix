package io.legado.app.ui.compose.components

import android.content.Context
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 阶段 2「顶栏 Compose 化」的搜索框状态载体。
 *
 * 它本身不画 UI（SearchView 的 View 版样式由 [MiuixTopBarView] 里的 Compose 搜索行渲染），
 * 只保留页面需要的 SearchView 兼容 API，让 Activity 里 `binding.titleBar.findViewById(R.id.search_view)`
 * 的写法与原有调用（`query` / `setQuery` / `isIconified` / `setOnQueryTextListener` / `applyTint` 等）
 * 基本不用改。UI 与状态分离，方便在 Composition 里直接读取 [text] / [hint]。
 */
class MiuixSearchBar(context: Context) : View(context) {

    internal var text by mutableStateOf("")
    internal var hint by mutableStateOf<CharSequence?>(null)
    internal var focusRequested by mutableStateOf(false)

    private var queryListener: SearchView.OnQueryTextListener? = null
    private var focusListener: View.OnFocusChangeListener? = null
    private var searchClickListener: View.OnClickListener? = null

    /** 等价于 SearchView.query。 */
    var query: CharSequence
        get() = text
        set(value) {
            val newText = value.toString()
            if (text != newText) {
                text = newText
            }
        }

    /** 等价于 SearchView.queryHint。 */
    var queryHint: CharSequence?
        get() = hint
        set(value) {
            hint = value
        }

    /** SearchView 的提交按钮在胶囊搜索框里没有单独按钮，IME 的搜索键即提交。 */
    var isSubmitButtonEnabled: Boolean = true

    /**
     * 兼容字段：false 表示展开并请求焦点（页面里普遍是 `searchView.isIconified = false`），
     * true 表示收起（这里退化为清除焦点、关掉键盘）。
     */
    var isIconified: Boolean
        get() = !focusRequested
        set(value) {
            if (!value) {
                focusRequested = true
            } else {
                clearFocus()
            }
        }

    fun setQuery(query: CharSequence?, submit: Boolean = false) {
        val newText = query?.toString().orEmpty()
        if (text != newText) {
            text = newText
        }
        if (submit) {
            submitQuery()
        }
    }

    fun setOnQueryTextListener(listener: SearchView.OnQueryTextListener?) {
        queryListener = listener
    }

    fun setOnQueryTextFocusChangeListener(listener: View.OnFocusChangeListener?) {
        focusListener = listener
    }

    fun setOnSearchClickListener(listener: View.OnClickListener?) {
        searchClickListener = listener
    }

    fun onActionViewExpanded() {
        focusRequested = true
    }

    /** View 版 SearchView 的 tint 扩展；Compose 搜索行的颜色跟随顶栏前景色，这里保持 API 兼容。 */
    fun applyTint(@ColorInt color: Int) = Unit

    internal fun onTextChanged(newText: String): Boolean {
        searchClickListener?.onClick(this)
        return queryListener?.onQueryTextChange(newText) ?: false
    }

    internal fun submitQuery(): Boolean = queryListener?.onQueryTextSubmit(text) ?: false

    internal fun onFocusChanged(focused: Boolean) {
        focusListener?.onFocusChange(this, focused)
    }
}
