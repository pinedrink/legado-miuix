package io.legado.app.ui.about

import android.content.Intent
import android.os.Bundle
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.databinding.ActivityAboutBinding
import io.legado.app.ui.compose.MiuixComposeActivity
import io.legado.app.ui.miuix.MiuixDemoActivity
import io.legado.app.utils.openUrl
import io.legado.app.utils.share
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 关于页（阶段 C 迁移）。
 *
 * 界面已换成 Miuix Compose（[AboutScreen]），业务动作仍走原有实现：
 * 分享/评分用 [io.legado.app.utils.share]、[io.legado.app.utils.openUrl]，
 * 检查更新、崩溃日志对话框、导出日志与堆转储分别复用
 * [checkAppUpdate]、[CrashLogsDialog]、[AboutActions]，没有复制业务逻辑。
 *
 * 原来的 ActionBar 菜单（分享/评分）已移到 Miuix TopAppBar 的动作区，
 * 行为与图标语义保持一致；Debug 构建下仍在“其他”分组里保留验证页入口。
 */
class AboutActivity : MiuixComposeActivity<ActivityAboutBinding>() {

    override val binding by viewBinding(ActivityAboutBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val openMiuixDemo: (() -> Unit)? = if (BuildConfig.DEBUG) {
            { startActivity(Intent(this, MiuixDemoActivity::class.java)) }
        } else {
            null
        }
        binding.composeView.setMiuixContent {
            AboutScreen(
                onBack = { finish() },
                onShare = {
                    share(
                        getString(R.string.app_share_description),
                        getString(R.string.app_name),
                    )
                },
                onRate = { openUrl("market://details?id=$packageName") },
                onContributors = { openUrl(getString(R.string.contributors_url)) },
                onUpdateLog = {
                    AboutActions.showMdFile(this, getString(R.string.update_log), "updateLog.md")
                },
                onCheckUpdate = { checkAppUpdate() },
                onCheckBetaUpdate = { checkAppUpdate(beta = true) },
                onCrashLog = { showDialogFragment<CrashLogsDialog>() },
                onSaveLog = AboutActions::saveLog,
                onCreateHeapDump = AboutActions::createHeapDump,
                onPrivacyPolicy = {
                    AboutActions.showMdFile(
                        this,
                        getString(R.string.privacy_policy),
                        "privacyPolicy.md",
                    )
                },
                onLicense = {
                    AboutActions.showMdFile(this, getString(R.string.license), "LICENSE.md")
                },
                onDisclaimer = {
                    AboutActions.showMdFile(this, getString(R.string.disclaimer), "disclaimer.md")
                },
                onMiuixDemo = openMiuixDemo,
            )
        }
    }
}
