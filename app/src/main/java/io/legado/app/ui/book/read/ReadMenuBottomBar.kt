package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.R
import io.legado.app.ui.compose.theme.LegadoMiuixTheme

/**
 * 阅读器底部按钮条（目录/朗读/界面/设置/备忘录）。
 *
 * 只负责这一条 bar 的渲染与点击回调，阅读正文、翻页、手势、主题配置仍然由 ReadMenu 控制。
 * 颜色由 ReadMenu 传入（跟随阅读主题），组件自身不写死颜色。
 */
class ReadMenuBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val textColor = mutableStateOf(Color.White)
    private val showMemo = mutableStateOf(false)

    var onCatalogClick: (() -> Unit)? = null
    var onReadAloudClick: (() -> Unit)? = null
    var onReadAloudLongClick: (() -> Unit)? = null
    var onFontClick: (() -> Unit)? = null
    var onSettingClick: (() -> Unit)? = null
    var onMemoClick: (() -> Unit)? = null

    private val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            LegadoMiuixTheme {
                ReadMenuBottomBarContent(
                    textColor = textColor.value,
                    showMemo = showMemo.value,
                    onCatalogClick = onCatalogClick,
                    onReadAloudClick = onReadAloudClick,
                    onReadAloudLongClick = onReadAloudLongClick,
                    onFontClick = onFontClick,
                    onSettingClick = onSettingClick,
                    onMemoClick = onMemoClick,
                )
            }
        }
    }

    init {
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setTextColor(color: Int) {
        textColor.value = Color(color)
    }

    fun setShowMemo(show: Boolean) {
        showMemo.value = show
    }

    @Composable
    private fun ReadMenuBottomBarContent(
        textColor: Color,
        showMemo: Boolean,
        onCatalogClick: (() -> Unit)?,
        onReadAloudClick: (() -> Unit)?,
        onReadAloudLongClick: (() -> Unit)?,
        onFontClick: (() -> Unit)?,
        onSettingClick: (() -> Unit)?,
        onMemoClick: (() -> Unit)?,
    ) {
        val labelStyle = remember(textColor) {
            TextStyle(color = textColor, fontSize = 12.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MenuItem(
                icon = R.drawable.ic_toc,
                label = stringResource(R.string.chapter_list),
                textColor = textColor,
                labelStyle = labelStyle,
                onClick = onCatalogClick,
            )
            MenuItem(
                icon = R.drawable.ic_read_aloud,
                label = stringResource(R.string.read_aloud),
                textColor = textColor,
                labelStyle = labelStyle,
                onClick = onReadAloudClick,
                onLongClick = onReadAloudLongClick,
            )
            MenuItem(
                icon = R.drawable.ic_interface_setting,
                label = stringResource(R.string.interface_setting),
                textColor = textColor,
                labelStyle = labelStyle,
                onClick = onFontClick,
            )
            MenuItem(
                icon = R.drawable.ic_settings,
                label = stringResource(R.string.setting),
                textColor = textColor,
                labelStyle = labelStyle,
                onClick = onSettingClick,
            )
            if (showMemo) {
                MenuItem(
                    icon = R.drawable.ic_edit,
                    label = stringResource(R.string.book_memo),
                    textColor = textColor,
                    labelStyle = labelStyle,
                    onClick = onMemoClick,
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun androidx.compose.foundation.layout.RowScope.MenuItem(
        icon: Int,
        label: String,
        textColor: Color,
        labelStyle: TextStyle,
        onClick: (() -> Unit)?,
        onLongClick: (() -> Unit)? = null,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val pressBackground = if (pressed) {
            textColor.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .background(pressBackground, RoundedCornerShape(12.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = onClick != null,
                    onClick = { onClick?.invoke() },
                    onLongClick = if (onLongClick != null) {
                        { onLongClick.invoke() }
                    } else {
                        null
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            androidx.compose.foundation.Image(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(textColor),
                modifier = Modifier.size(22.dp),
            )
            androidx.compose.foundation.text.BasicText(
                text = label,
                style = labelStyle,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
