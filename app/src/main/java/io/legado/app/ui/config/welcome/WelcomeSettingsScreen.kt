package io.legado.app.ui.config.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 欢迎页样式设置的 Compose 列表。
 *
 * 只做展示与回调，读写偏好全部走 [WelcomeSettingsActions]。
 */
@Composable
fun WelcomeSettingsScreen(
    entries: List<WelcomeSettingEntry>,
    listState: LazyListState,
    onItemClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
    onShowTimeChange: (Int) -> Unit,
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
            items(groups) { group ->
                when (group) {
                    is WelcomeSettingGroup.SectionHeader -> {
                        SmallTitle(text = stringResource(group.titleRes))
                    }

                    is WelcomeSettingGroup.Rows -> {
                        Card {
                            group.items.forEachIndexed { index, item ->
                                if (index > 0) {
                                    HorizontalDivider()
                                }
                                WelcomeSettingRow(
                                    item = item,
                                    onClick = { onItemClick(item.key) },
                                    onSwitchChange = { onSwitchChange(item.key, it) },
                                    onShowTimeChange = onShowTimeChange,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeSettingRow(
    item: WelcomeSettingEntry.Item,
    onClick: () -> Unit,
    onSwitchChange: (Boolean) -> Unit,
    onShowTimeChange: (Int) -> Unit,
) {
    when (item.kind) {
        WelcomeSettingEntry.Kind.Switch -> {
            BasicComponent(
                title = item.title,
                summary = item.summary,
                enabled = item.enabled,
                onClick = null,
                endActions = {
                    Switch(
                        checked = item.checked,
                        onCheckedChange = onSwitchChange,
                        enabled = item.enabled,
                    )
                },
            )
        }

        WelcomeSettingEntry.Kind.Slider -> {
            BasicComponent(
                title = item.title,
                summary = item.summary,
                enabled = item.enabled,
                onClick = null,
                endActions = {
                    Text(
                        text = item.value.toString(),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                },
                bottomAction = {
                    Slider(
                        value = item.value.toFloat(),
                        onValueChange = { onShowTimeChange(it.roundToInt()) },
                        enabled = item.enabled,
                        valueRange = item.valueRange.first.toFloat()..item.valueRange.last.toFloat(),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                },
            )
        }

        WelcomeSettingEntry.Kind.Action -> {
            BasicComponent(
                title = item.title,
                summary = item.summary,
                enabled = item.enabled,
                onClick = onClick,
            )
        }
    }
}

private sealed interface WelcomeSettingGroup {
    data class SectionHeader(val titleRes: Int) : WelcomeSettingGroup
    data class Rows(val items: List<WelcomeSettingEntry.Item>) : WelcomeSettingGroup
}

/** 把连续的 Item 聚成一张卡片，Section 单独成行。 */
private fun List<WelcomeSettingEntry>.toGroups(): List<WelcomeSettingGroup> {
    val groups = mutableListOf<WelcomeSettingGroup>()
    val rows = mutableListOf<WelcomeSettingEntry.Item>()
    fun flush() {
        if (rows.isNotEmpty()) {
            groups += WelcomeSettingGroup.Rows(rows.toList())
            rows.clear()
        }
    }
    forEach { entry ->
        when (entry) {
            is WelcomeSettingEntry.Section -> {
                flush()
                groups += WelcomeSettingGroup.SectionHeader(entry.titleRes)
            }

            is WelcomeSettingEntry.Item -> rows += entry
        }
    }
    flush()
    return groups
}
