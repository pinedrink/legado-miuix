package io.legado.app.ui.config.backup

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.ui.compose.components.LegadoMiuixScaffold
import io.legado.app.ui.config.backup.BackupSettingEntry.Item
import io.legado.app.ui.config.backup.BackupSettingEntry.Section
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch

/**
 * 「备份与恢复」页的 Compose 内容。
 *
 * 只负责展示与回调，备份/恢复等副作用全部留在 Fragment 里。
 */
@Composable
fun BackupSettingsScreen(
    entries: List<BackupSettingEntry>,
    listState: LazyListState,
    onItemClick: (String) -> Unit,
    onItemLongClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LegadoMiuixScaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            entries.toGroups().forEach { group ->
                when (group) {
                    is BackupGroup.SectionHeader -> item(key = "section_${group.titleRes}") {
                        SmallTitle(text = stringResource(group.titleRes))
                    }

                    is BackupGroup.Rows -> item(key = "group_${group.items.first().key}") {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            group.items.forEachIndexed { index, item ->
                                if (index > 0) {
                                    HorizontalDivider()
                                }
                                BackupSettingRow(
                                    item = item,
                                    onClick = { onItemClick(item.key) },
                                    onLongClick = { onItemLongClick(item.key) },
                                    onCheckedChange = { checked -> onSwitchChange(item.key, checked) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 连续的行聚成一张卡片，分组标题单独成段 */
private sealed interface BackupGroup {
    data class SectionHeader(val titleRes: Int) : BackupGroup
    data class Rows(val items: List<Item>) : BackupGroup
}

private fun List<BackupSettingEntry>.toGroups(): List<BackupGroup> {
    val groups = mutableListOf<BackupGroup>()
    var pending = mutableListOf<Item>()
    fun flush() {
        if (pending.isNotEmpty()) {
            groups += BackupGroup.Rows(pending)
            pending = mutableListOf()
        }
    }
    forEach { entry ->
        when (entry) {
            is Section -> {
                flush()
                groups += BackupGroup.SectionHeader(entry.titleRes)
            }

            is Item -> pending += entry
        }
    }
    flush()
    return groups
}

@Composable
private fun BackupSettingRow(
    item: Item,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    // 「恢复」行同时支持单击（WebDAV 恢复）与长按（本地恢复），
    // BasicComponent 只提供 onClick，因此这一行自行处理手势。
    val supportsLongClick = item.key == BackupSettingsActions.KEY_WEB_DAV_RESTORE
    BasicComponent(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.kind == BackupSettingEntry.Kind.Action && supportsLongClick) {
                    Modifier.pointerInput(item.key) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongClick() },
                        )
                    }
                } else {
                    Modifier
                }
            ),
        title = item.title,
        summary = item.summary,
        enabled = item.enabled,
        onClick = if (item.kind == BackupSettingEntry.Kind.Action && !supportsLongClick) onClick else null,
        endActions = if (item.kind == BackupSettingEntry.Kind.Switch) {
            {
                Switch(
                    checked = item.checked,
                    onCheckedChange = onCheckedChange,
                    enabled = item.enabled,
                )
            }
        } else {
            null
        },
    )
}
