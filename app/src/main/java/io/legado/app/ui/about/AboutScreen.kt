package io.legado.app.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.constant.AppConst.appInfo
import io.legado.app.ui.compose.components.LegadoMiuixScaffold
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关于页的 Miuix 界面（阶段 C，第一个正式迁移页面）。
 *
 * 只负责展示与回调：分享、评分、检查更新、导出日志等动作由 `AboutActivity` 提供，
 * 页面本身不访问文件系统、网络或对话框。
 *
 * @param onMiuixDemo 仅 Debug 构建传入，用于进入阶段 A 的接入验证页。
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onRate: () -> Unit,
    onContributors: () -> Unit,
    onUpdateLog: () -> Unit,
    onCheckUpdate: () -> Unit,
    onCheckBetaUpdate: () -> Unit,
    onCrashLog: () -> Unit,
    onSaveLog: () -> Unit,
    onCreateHeapDump: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLicense: () -> Unit,
    onDisclaimer: () -> Unit,
    modifier: Modifier = Modifier,
    onMiuixDemo: (() -> Unit)? = null,
) {
    LegadoMiuixScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.about),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = MiuixIcons.Share,
                            contentDescription = stringResource(R.string.share),
                        )
                    }
                    IconButton(onClick = onRate) {
                        Icon(
                            imageVector = MiuixIcons.Favorites,
                            contentDescription = stringResource(R.string.scoring),
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
            AboutHeaderCard()
            Card(modifier = Modifier.fillMaxWidth()) {
                AboutItem(
                    title = stringResource(R.string.contributors),
                    summary = stringResource(R.string.contributors_summary),
                    onClick = onContributors,
                )
                HorizontalDivider()
                AboutItem(
                    title = stringResource(R.string.update_log),
                    summary = "${stringResource(R.string.version)} ${appInfo.versionName}",
                    onClick = onUpdateLog,
                )
                HorizontalDivider()
                AboutItem(
                    title = stringResource(R.string.check_update),
                    onClick = onCheckUpdate,
                )
                HorizontalDivider()
                AboutItem(
                    title = stringResource(R.string.check_beta_update),
                    onClick = onCheckBetaUpdate,
                )
            }
            SmallTitle(text = stringResource(R.string.other))
            Card(modifier = Modifier.fillMaxWidth()) {
                AboutItem(
                    title = stringResource(R.string.crash_log),
                    onClick = onCrashLog,
                )
                HorizontalDivider()
                AboutItem(
                    title = stringResource(R.string.save_log),
                    onClick = onSaveLog,
                )
                HorizontalDivider()
                AboutItem(
                    title = stringResource(R.string.create_heap_dump),
                    onClick = onCreateHeapDump,
                )
                HorizontalDivider()
                AboutItem(
                    title = stringResource(R.string.privacy_policy),
                    onClick = onPrivacyPolicy,
                )
                HorizontalDivider()
                AboutItem(
                    title = stringResource(R.string.license),
                    onClick = onLicense,
                )
                HorizontalDivider()
                AboutItem(
                    title = stringResource(R.string.disclaimer),
                    onClick = onDisclaimer,
                )
                if (onMiuixDemo != null) {
                    HorizontalDivider()
                    AboutItem(
                        title = stringResource(R.string.miuix_demo_title),
                        onClick = onMiuixDemo,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHeaderCard(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.about_description)
    val gzh = stringResource(R.string.legado_gzh)
    val accentColor = MiuixTheme.colorScheme.primary
    val annotatedDescription = remember(description, gzh, accentColor) {
        buildAnnotatedString {
            append(description)
            // 与原 XML 页面一致：把公众号名称用强调色标出来
            val start = description.indexOf(gzh)
            if (start >= 0) {
                addStyle(SpanStyle(color = accentColor), start, start + gzh.length)
            }
        }
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MiuixTheme.textStyles.title2,
            )
            Text(
                text = appInfo.versionName,
                modifier = Modifier.padding(top = 4.dp),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = annotatedDescription,
                modifier = Modifier.padding(top = 12.dp),
                style = MiuixTheme.textStyles.paragraph,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun AboutItem(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = summary,
        onClick = onClick,
    )
}
