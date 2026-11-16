package io.legado.app.ui.config.coverfont

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.legado.app.R
import io.legado.app.ui.compose.components.LegadoMiuixScaffold
import io.legado.app.ui.widget.image.CoverImageView
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 默认书名样式设置页。
 *
 * 前若干行是普通设置行，最后一行是封面预览：预览继续复用 View 版 [CoverImageView]，
 * 通过 [AndroidView] 内嵌，避免把封面绘制逻辑复制到 Compose。
 *
 * @param previewKey 变化时重建预览，用于样式改动后立即看到效果。
 */
@Composable
fun CoverFontSettingsScreen(
    entries: List<CoverFontSettingEntry>,
    listState: LazyListState,
    previewKey: Int,
    onItemClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = entries.filterIsInstance<CoverFontSettingEntry.Item>()
        .filter { it.kind != CoverFontSettingEntry.Kind.Preview }
    val hasPreview = entries.filterIsInstance<CoverFontSettingEntry.Item>()
        .any { it.kind == CoverFontSettingEntry.Kind.Preview }

    LegadoMiuixScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { scaffoldPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "rows") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    rows.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        CoverFontSettingRow(
                            item = item,
                            onClick = onItemClick,
                            onSwitchChange = onSwitchChange,
                        )
                    }
                }
            }
            if (hasPreview) {
                item(key = "preview") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        CoverPreviewRow(previewKey = previewKey)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverFontSettingRow(
    item: CoverFontSettingEntry.Item,
    onClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
) {
    BasicComponent(
        title = item.title,
        summary = item.summary,
        enabled = item.enabled,
        onClick = if (item.kind == CoverFontSettingEntry.Kind.Switch) {
            null
        } else if (item.enabled) {
            { onClick(item.key) }
        } else {
            null
        },
        endActions = if (item.kind == CoverFontSettingEntry.Kind.Switch) {
            {
                Switch(
                    checked = item.checked,
                    onCheckedChange = { onSwitchChange(item.key, it) },
                    enabled = item.enabled,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun CoverPreviewRow(previewKey: Int) {
    val shortDescription = stringResource(R.string.cover_preview_short)
    val longDescription = stringResource(R.string.cover_preview_long)
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CoverPreview(
            name = "开源阅读",
            author = "开源阅读LegadoTeam",
            contentDescription = shortDescription,
            previewKey = previewKey,
            modifier = Modifier.weight(1f),
        )
        CoverPreview(
            name = "开源阅读可以看小说、看漫画、听书",
            author = "开源阅读",
            contentDescription = longDescription,
            previewKey = previewKey,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CoverPreview(
    name: String,
    author: String,
    contentDescription: String,
    previewKey: Int,
    modifier: Modifier,
) {
    // 在 Composable 作用域里取色，AndroidView 的 factory/update 是非 Composable lambda
    val coverBackground = MiuixTheme.colorScheme.surface.toArgb()
    key(previewKey) {
        AndroidView(
            factory = { ctx ->
                CoverImageView(ctx).apply {
                    this.contentDescription = contentDescription
                    setBackgroundColor(coverBackground)
                }
            },
            update = { view ->
                view.contentDescription = contentDescription
                view.load(name = name, author = author)
            },
            modifier = modifier,
        )
    }
}
