package io.legado.app.ui.config.other

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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 其它设置的 Compose 版界面。
 *
 * 只负责渲染与事件上抛；偏好读写和副作用都在 [OtherSettingsActions] 与 Fragment 里，
 * 各类弹窗（User-Agent、自定义 hosts、端口/线程数、清理缓存等）仍复用原有实现。
 */
@Composable
fun OtherSettingsScreen(
    entries: List<OtherSettingEntry>,
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
                    is OtherGroup.SectionHeader -> SmallTitle(text = stringResource(group.titleRes))
                    is OtherGroup.Rows -> Card(modifier = Modifier.fillMaxWidth()) {
                        group.items.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider()
                            }
                            OtherSettingRow(item, onItemClick, onSwitchChange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtherSettingRow(
    item: OtherSettingEntry.Item,
    onItemClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
) {
    BasicComponent(
        title = item.title,
        summary = item.summary,
        enabled = item.enabled,
        onClick = if (item.kind == OtherSettingEntry.Kind.Switch) null else {
            { onItemClick(item.key) }
        },
        endActions = {
            when (item.kind) {
                OtherSettingEntry.Kind.Switch -> Switch(
                    checked = item.checked,
                    onCheckedChange = { onSwitchChange(item.key, it) },
                    enabled = item.enabled,
                )

                OtherSettingEntry.Kind.List -> item.value?.let {
                    Text(
                        text = it,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                OtherSettingEntry.Kind.Action -> Unit
            }
        },
    )
}

private sealed interface OtherGroup {
    val key: String

    data class SectionHeader(val titleRes: Int) : OtherGroup {
        override val key: String get() = "section_$titleRes"
    }

    data class Rows(val items: List<OtherSettingEntry.Item>) : OtherGroup {
        override val key: String get() = "rows_${items.first().key}"
    }
}

private fun List<OtherSettingEntry>.toGroups(): List<OtherGroup> {
    val groups = mutableListOf<OtherGroup>()
    var pending = mutableListOf<OtherSettingEntry.Item>()
    fun flush() {
        if (pending.isNotEmpty()) {
            groups += OtherGroup.Rows(pending)
            pending = mutableListOf()
        }
    }
    forEach { entry ->
        when (entry) {
            is OtherSettingEntry.Section -> {
                flush()
                groups += OtherGroup.SectionHeader(entry.titleRes)
            }

            is OtherSettingEntry.Item -> pending += entry
        }
    }
    flush()
    return groups
}
