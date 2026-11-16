package io.legado.app.ui.miuix

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ThemeConfig
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 阶段 A 的 Miuix 接入验证页。
 *
 * 只用于验证依赖、主题桥接、ComposeView 生命周期和基础组件表现，不承载任何业务逻辑。
 * 不读取数据库、不发起网络请求，所有状态都是页面内的临时 UI 状态。
 *
 * @param darkTheme 当前生效的亮暗状态（由 [LegadoMiuixTheme] 之外的调用方计算并传入）
 * @param darkThemeOverride 页面内临时覆盖的亮暗状态，null 表示跟随 Legado 主题
 * @param onDarkThemeOverrideChange 修改临时覆盖状态
 * @param useLegadoColors true 用 Legado 配色，false 用 Miuix 官方默认配色
 * @param onUseLegadoColorsChange 切换配色来源
 * @param onBack 返回上一页
 */
@Composable
fun MiuixDemoScreen(
    darkTheme: Boolean,
    darkThemeOverride: Boolean?,
    onDarkThemeOverrideChange: (Boolean?) -> Unit,
    useLegadoColors: Boolean,
    onUseLegadoColorsChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var switchChecked by remember { mutableStateOf(true) }
    var clickCount by remember { mutableIntStateOf(0) }
    var sliderValue by remember { mutableFloatStateOf(0.5f) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.miuix_demo_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.miuix_demo_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeCard(
                darkTheme = darkTheme,
                darkThemeOverride = darkThemeOverride,
                onDarkThemeOverrideChange = onDarkThemeOverrideChange,
                useLegadoColors = useLegadoColors,
                onUseLegadoColorsChange = onUseLegadoColorsChange,
            )
            ComponentCard(
                switchChecked = switchChecked,
                onSwitchCheckedChange = { switchChecked = it },
                clickCount = clickCount,
                onButtonClick = { clickCount++ },
                onShowDialog = { showDialog = true },
                sliderValue = sliderValue,
                onSliderValueChange = { sliderValue = it },
            )
            EnvironmentCard(darkTheme, useLegadoColors)
            Button(
                onClick = { clickCount++ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.miuix_demo_button_action))
            }
        }
        OverlayDialog(
            show = showDialog,
            title = stringResource(R.string.miuix_demo_dialog_title),
            summary = stringResource(R.string.miuix_demo_dialog_message),
            onDismissRequest = { showDialog = false },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(
                    text = stringResource(R.string.miuix_demo_cancel),
                    onClick = { showDialog = false },
                )
                TextButton(
                    text = stringResource(R.string.miuix_demo_ok),
                    onClick = { showDialog = false },
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    darkTheme: Boolean,
    darkThemeOverride: Boolean?,
    onDarkThemeOverrideChange: (Boolean?) -> Unit,
    useLegadoColors: Boolean,
    onUseLegadoColorsChange: (Boolean) -> Unit,
) {
    Card {
        BasicComponent(
            title = stringResource(R.string.miuix_demo_section_theme),
            summary = stringResource(
                R.string.miuix_demo_current_theme,
                stringResource(
                    if (darkTheme) R.string.miuix_demo_theme_dark
                    else R.string.miuix_demo_theme_light
                ),
            ),
        )
        HorizontalDivider()
        ThemeOptionRow(
            text = stringResource(R.string.miuix_demo_theme_follow),
            selected = darkThemeOverride == null,
            onClick = { onDarkThemeOverrideChange(null) },
        )
        HorizontalDivider()
        ThemeOptionRow(
            text = stringResource(R.string.miuix_demo_theme_light),
            selected = darkThemeOverride == false,
            onClick = { onDarkThemeOverrideChange(false) },
        )
        HorizontalDivider()
        ThemeOptionRow(
            text = stringResource(R.string.miuix_demo_theme_dark),
            selected = darkThemeOverride == true,
            onClick = { onDarkThemeOverrideChange(true) },
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_palette),
            summary = stringResource(R.string.miuix_demo_palette_summary),
        )
        HorizontalDivider()
        ThemeOptionRow(
            text = stringResource(R.string.miuix_demo_palette_miuix),
            selected = !useLegadoColors,
            onClick = { onUseLegadoColorsChange(false) },
        )
        HorizontalDivider()
        ThemeOptionRow(
            text = stringResource(R.string.miuix_demo_palette_legado),
            selected = useLegadoColors,
            onClick = { onUseLegadoColorsChange(true) },
        )
    }
}

@Composable
private fun ThemeOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = text,
        onClick = onClick,
        endActions = {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
        },
    )
}

@Composable
private fun ComponentCard(
    switchChecked: Boolean,
    onSwitchCheckedChange: (Boolean) -> Unit,
    clickCount: Int,
    onButtonClick: () -> Unit,
    onShowDialog: () -> Unit,
    sliderValue: Float,
    onSliderValueChange: (Float) -> Unit,
) {
    Card {
        BasicComponent(
            title = stringResource(R.string.miuix_demo_section_components),
            summary = stringResource(R.string.miuix_demo_section_components_summary),
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_switch),
            summary = stringResource(R.string.miuix_demo_switch_summary),
            endActions = {
                Switch(
                    checked = switchChecked,
                    onCheckedChange = { onSwitchCheckedChange(it) },
                )
            },
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_button),
            summary = stringResource(R.string.miuix_demo_button_summary, clickCount),
            endActions = {
                TextButton(
                    text = stringResource(R.string.miuix_demo_button_action),
                    onClick = onButtonClick,
                )
            },
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_dialog),
            summary = stringResource(R.string.miuix_demo_dialog_summary),
            endActions = {
                TextButton(
                    text = stringResource(R.string.miuix_demo_dialog_show),
                    onClick = onShowDialog,
                )
            },
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_slider),
            summary = stringResource(
                R.string.miuix_demo_slider_summary,
                (sliderValue * 100).toInt(),
            ),
            bottomAction = {
                Slider(
                    value = sliderValue,
                    onValueChange = onSliderValueChange,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            },
        )
    }
}

@Composable
private fun EnvironmentCard(darkTheme: Boolean, useLegadoColors: Boolean) {
    val yes = stringResource(R.string.miuix_demo_value_yes)
    val no = stringResource(R.string.miuix_demo_value_no)
    Card {
        BasicComponent(
            title = stringResource(R.string.miuix_demo_section_environment),
            summary = stringResource(R.string.miuix_demo_section_environment_summary),
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_env_legado_dark),
            summary = if (AppConfig.isNightTheme) yes else no,
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_env_system_dark),
            summary = if (isSystemInDarkTheme()) yes else no,
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_env_theme),
            summary = ThemeConfig.getTheme().name,
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_env_eink),
            summary = if (AppConfig.isEInkMode) yes else no,
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_env_rendering),
            summary = stringResource(
                R.string.miuix_demo_env_rendering_summary,
                if (darkTheme) stringResource(R.string.miuix_demo_theme_dark)
                else stringResource(R.string.miuix_demo_theme_light),
            ),
        )
        HorizontalDivider()
        BasicComponent(
            title = stringResource(R.string.miuix_demo_palette),
            summary = stringResource(
                if (useLegadoColors) R.string.miuix_demo_palette_legado
                else R.string.miuix_demo_palette_miuix
            ),
        )
    }
}
