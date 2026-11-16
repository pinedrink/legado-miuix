package io.legado.app.ui.config.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * 主题设置的 Compose 版界面。
 *
 * 只负责渲染与事件上抛：读写偏好、应用主题、校验颜色都在 [ThemeSettingsActions] 与 Fragment 里。
 * 颜色选择、数字选择、壁纸选择等弹窗暂时仍复用项目原有实现，避免一次性改动过多交互。
 */
@Composable
fun ThemeSettingsScreen(
    entries: List<ThemeSettingEntry>,
    onItemClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
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
                .padding(scaffoldPadding)
                .padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = groups, key = { it.key }) { group ->
                when (group) {
                    is ThemeGroup.SectionHeader -> SmallTitle(text = stringResource(group.titleRes))
                    is ThemeGroup.Rows -> Card(modifier = Modifier.fillMaxWidth()) {
                        group.items.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider()
                            }
                            ThemeSettingRow(item, onItemClick, onSwitchChange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingRow(
    item: ThemeSettingEntry.Item,
    onItemClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
) {
    BasicComponent(
        title = stringResource(item.titleRes),
        summary = item.summary,
        enabled = item.enabled,
        onClick = if (item.kind == ThemeSettingEntry.Kind.Switch) null else {
            { onItemClick(item.key) }
        },
        endActions = {
            when (item.kind) {
                ThemeSettingEntry.Kind.Switch -> Switch(
                    checked = item.checked,
                    onCheckedChange = { onSwitchChange(item.key, it) },
                    enabled = item.enabled,
                )

                ThemeSettingEntry.Kind.Color -> ColorSwatch(item.color ?: 0)

                ThemeSettingEntry.Kind.List -> item.value?.let {
                    Text(
                        text = it,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                ThemeSettingEntry.Kind.Action -> Unit
            }
        }
    )
}

@Composable
private fun ColorSwatch(color: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(color)),
    )
}

private sealed interface ThemeGroup {
    val key: String

    data class SectionHeader(@androidx.annotation.StringRes val titleRes: Int) : ThemeGroup {
        override val key: String get() = "section_$titleRes"
    }

    data class Rows(val items: List<ThemeSettingEntry.Item>) : ThemeGroup {
        override val key: String get() = "rows_${items.first().key}"
    }
}

private fun List<ThemeSettingEntry>.toGroups(): List<ThemeGroup> {
    val groups = mutableListOf<ThemeGroup>()
    var pending = mutableListOf<ThemeSettingEntry.Item>()
    fun flush() {
        if (pending.isNotEmpty()) {
            groups += ThemeGroup.Rows(pending)
            pending = mutableListOf()
        }
    }
    forEach { entry ->
        when (entry) {
            is ThemeSettingEntry.Section -> {
                flush()
                groups += ThemeGroup.SectionHeader(entry.titleRes)
            }

            is ThemeSettingEntry.Item -> pending += entry
        }
    }
    flush()
    return groups
}
