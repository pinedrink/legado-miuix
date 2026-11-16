package io.legado.app.ui.config.cover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.ui.compose.components.LegadoMiuixScaffold
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch

/**
 * 封面设置的 Compose 版界面。
 *
 * 只负责渲染与事件上抛；偏好读写、默认封面刷新都在 [CoverSettingsActions] 与 Fragment 里，
 * 选图、封面规则、封面字体设置仍复用原有实现。
 */
@Composable
fun CoverSettingsScreen(
    entries: List<CoverSettingEntry>,
    onItemClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val groups = remember(entries) { entries.toGroups() }
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
            items(items = groups, key = { it.key }) { group ->
                when (group) {
                    is CoverGroup.SectionHeader -> SmallTitle(text = stringResource(group.titleRes))
                    is CoverGroup.Rows -> Card(modifier = Modifier.fillMaxWidth()) {
                        group.items.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider()
                            }
                            CoverSettingRow(item, onItemClick, onSwitchChange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverSettingRow(
    item: CoverSettingEntry.Item,
    onItemClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
) {
    BasicComponent(
        title = item.title,
        summary = item.summary,
        enabled = item.enabled,
        onClick = if (item.kind == CoverSettingEntry.Kind.Switch) null else {
            { onItemClick(item.key) }
        },
        endActions = {
            if (item.kind == CoverSettingEntry.Kind.Switch) {
                Switch(
                    checked = item.checked,
                    onCheckedChange = { onSwitchChange(item.key, it) },
                    enabled = item.enabled,
                )
            }
        },
    )
}

private sealed interface CoverGroup {
    val key: String

    data class SectionHeader(val titleRes: Int) : CoverGroup {
        override val key: String get() = "section_$titleRes"
    }

    data class Rows(val items: List<CoverSettingEntry.Item>) : CoverGroup {
        override val key: String get() = "rows_${items.first().key}"
    }
}

private fun List<CoverSettingEntry>.toGroups(): List<CoverGroup> {
    val groups = mutableListOf<CoverGroup>()
    var pending = mutableListOf<CoverSettingEntry.Item>()
    fun flush() {
        if (pending.isNotEmpty()) {
            groups += CoverGroup.Rows(pending)
            pending = mutableListOf()
        }
    }
    forEach { entry ->
        when (entry) {
            is CoverSettingEntry.Section -> {
                flush()
                groups += CoverGroup.SectionHeader(entry.titleRes)
            }

            is CoverSettingEntry.Item -> pending += entry
        }
    }
    flush()
    return groups
}
