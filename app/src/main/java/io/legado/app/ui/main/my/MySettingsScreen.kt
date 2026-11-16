package io.legado.app.ui.main.my

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.compose.components.LegadoMiuixScaffold
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置中心（「我的」标签页）的 Miuix 界面。
 *
 * 只负责渲染：设置项列表、开关、主题选择对话框、"自定义我的"对话框。
 * 所有业务动作由 [MySettingsActions] 与宿主 Fragment 提供，本文件不直接读写数据库或启动服务。
 */
@Composable
fun MySettingsScreen(
    entries: List<MySettingEntry>,
    modifier: Modifier = Modifier,
    showThemePicker: Boolean = false,
    themeOptions: List<Pair<String, String>> = emptyList(),
    currentThemeValue: String = "",
    showCustomizeDialog: Boolean = false,
    customizeOptions: List<MySettingEntry> = emptyList(),
    selectedCustomizeKeys: Set<String> = emptySet(),
    onItemClick: (String) -> Unit = {},
    onItemLongClick: (String) -> Unit = {},
    onSwitchChange: (String, Boolean) -> Unit = { _, _ -> },
    onThemeOptionClick: (String) -> Unit = {},
    onDismissThemePicker: () -> Unit = {},
    onDismissCustomize: () -> Unit = {},
    onToggleCustomize: (String, Boolean) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val accentColor = remember(context) { Color(ThemeStore.accentColor(context)) }

    LegadoMiuixScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            var index = 0
            while (index < entries.size) {
                when (val entry = entries[index]) {
                    is MySettingEntry.Section -> {
                        SmallTitle(text = stringResource(entry.titleRes))
                        index++
                    }

                    is MySettingEntry.Item -> {
                        val group = mutableListOf<MySettingEntry.Item>()
                        while (index < entries.size && entries[index] is MySettingEntry.Item) {
                            group += entries[index] as MySettingEntry.Item
                            index++
                        }
                        Card {
                            group.forEachIndexed { groupIndex, item ->
                                if (groupIndex > 0) HorizontalDivider()
                                SettingRow(
                                    item = item,
                                    accentColor = accentColor,
                                    onClick = onItemClick,
                                    onLongClick = onItemLongClick,
                                    onSwitchChange = onSwitchChange,
                                )
                            }
                        }
                    }
                }
            }
        }
        // OverlayDialog 依赖 Scaffold 提供的弹窗宿主，必须放在 Scaffold 的内容里。
        if (showThemePicker) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.theme_mode),
            onDismissRequest = onDismissThemePicker,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEach { (value, label) ->
                    ThemeOptionRow(
                        label = label,
                        selected = value == currentThemeValue,
                        onClick = { onThemeOptionClick(value) },
                    )
                }
            }
        }
    }

    if (showCustomizeDialog) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.my_more_items),
            onDismissRequest = onDismissCustomize,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    customizeOptions.forEach { entry ->
                        if (entry is MySettingEntry.Item) {
                            CustomizeRow(
                                title = entry.title,
                                checked = entry.key in selectedCustomizeKeys,
                                onCheckedChange = { onToggleCustomize(entry.key, it) },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(text = stringResource(R.string.ok), onClick = onDismissCustomize)
                }
            }
        }
    }
    }
}

@Composable
private fun SettingRow(
    item: MySettingEntry.Item,
    accentColor: Color,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
) {
    val hasLongClick = item.key == MySettingsActions.KEY_WEB_SERVICE ||
            item.key == MySettingsActions.KEY_MCP_SERVICE
    top.yukonga.miuix.kmp.basic.BasicComponent(
        title = item.title,
        summary = item.summary,
        modifier = if (hasLongClick) {
            Modifier.pointerInput(item.key) {
                detectTapGestures(
                    onTap = { onClick(item.key) },
                    onLongPress = { onLongClick(item.key) },
                )
            }
        } else {
            Modifier
        },
        startAction = item.iconRes?.let { iconRes ->
            {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp),
                    tint = accentColor,
                )
            }
        },
        endActions = {
            when (item.kind) {
                MySettingEntry.Kind.Switch -> Switch(
                    checked = item.checked,
                    onCheckedChange = { onSwitchChange(item.key, it) },
                )

                MySettingEntry.Kind.List -> Text(
                    text = item.value.orEmpty(),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )

                MySettingEntry.Kind.Action -> Unit
            }
        },
        onClick = if (hasLongClick) null else ({ onClick(item.key) }),
    )
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    top.yukonga.miuix.kmp.basic.BasicComponent(
        title = label,
        onClick = onClick,
        endActions = {
            top.yukonga.miuix.kmp.basic.RadioButton(selected = selected, onClick = onClick)
        },
    )
}

@Composable
private fun CustomizeRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    top.yukonga.miuix.kmp.basic.BasicComponent(
        title = title,
        onClick = { onCheckedChange(!checked) },
        endActions = {
            top.yukonga.miuix.kmp.basic.Checkbox(
                state = if (checked) ToggleableState.On else ToggleableState.Off,
                onClick = { onCheckedChange(!checked) },
            )
        },
    )
}
