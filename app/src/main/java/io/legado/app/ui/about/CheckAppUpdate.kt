package io.legado.app.ui.about

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.legado.app.help.update.AppUpdate
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx

/** Shared by About and the optional My-page update entries. */
fun Fragment.checkAppUpdate(beta: Boolean = false) {
    val waitDialog = WaitDialog(requireContext())
    waitDialog.show()
    val request = if (beta) AppUpdate.checkBeta(lifecycleScope)
        else AppUpdate.gitHubUpdate.check(lifecycleScope)
    request.onSuccess {
        if (isAdded && !childFragmentManager.isStateSaved) showDialogFragment(UpdateDialog(it))
    }.onError {
        appCtx.toastOnUi(it.localizedMessage)
    }.onFinally {
        waitDialog.dismiss()
    }
}

/**
 * 关于页迁移到 Compose 后的入口：动作由 Activity 发起，所以补一个 Activity 版本，
 * 检查逻辑与 Fragment 版本共用同一套 [AppUpdate] 调用。
 */
fun AppCompatActivity.checkAppUpdate(beta: Boolean = false) {
    val waitDialog = WaitDialog(this)
    waitDialog.show()
    val request = if (beta) AppUpdate.checkBeta(lifecycleScope)
        else AppUpdate.gitHubUpdate.check(lifecycleScope)
    request.onSuccess {
        if (!isFinishing && !isDestroyed && !supportFragmentManager.isStateSaved) {
            showDialogFragment(UpdateDialog(it))
        }
    }.onError {
        toastOnUi(it.localizedMessage)
    }.onFinally {
        waitDialog.dismiss()
    }
}
