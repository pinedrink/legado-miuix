package io.legado.app.ui.config

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogAutoBackupBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogLanBackupSendBinding
import io.legado.app.databinding.FragmentBackupConfigBinding
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.AppWebDav
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.BackupConfig
import io.legado.app.help.storage.ImportOldData
import io.legado.app.help.storage.LanBackupSession
import io.legado.app.help.storage.LanBackupTransfer
import io.legado.app.help.storage.Restore
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.ui.config.backup.BackupSettingEntry
import io.legado.app.ui.config.backup.BackupSettingsActions
import io.legado.app.ui.config.backup.BackupSettingsScreen
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.FileDoc
import io.legado.app.utils.QRCodeUtils
import io.legado.app.utils.applyTint
import io.legado.app.utils.checkWrite
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.launch
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx

/**
 * 备份与恢复设置页。
 *
 * 内容区已迁移到 Compose（[BackupSettingsScreen]），备份、恢复、局域网传输等流程
 * 仍然使用迁移前的实现，只是入口从 Preference 换成了行点击回调。
 */
class BackupConfigFragment : BaseFragment(R.layout.fragment_backup_config),
    MenuProvider,
    ConfigSearchable {

    private val binding by viewBinding(FragmentBackupConfigBinding::bind)
    private val viewModel by activityViewModels<ConfigViewModel>()
    private val waitDialog by lazy { WaitDialog(requireContext()) }
    private var backupJob: Job? = null
    private var manualBackupUploadWebDav = true
    private var restoreJob: Job? = null
    private var lanBackupJob: Job? = null
    private var lanBackupSession: LanBackupSession? = null
    private var lanBackupDialog: AlertDialog? = null

    private var entries by mutableStateOf<List<BackupSettingEntry>>(emptyList())
    private val listState = LazyListState()

    private val selectBackupPath = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                AppConfig.backupPath = uri.toString()
            } else {
                AppConfig.backupPath = uri.path
            }
            refreshRows()
        }
    }
    private val backupDir = registerForActivityResult(HandleFileContract()) { result ->
        result.uri?.let { uri ->
            if (uri.isContentScheme()) {
                AppConfig.backupPath = uri.toString()
                backup(uri.toString(), manualBackupUploadWebDav)
            } else {
                uri.path?.let { path ->
                    AppConfig.backupPath = path
                    backup(path, manualBackupUploadWebDav)
                }
            }
            refreshRows()
        }
    }
    private val restoreDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            waitDialog.setText("恢复中…")
            waitDialog.show()
            val task = Coroutine.async {
                Restore.restore(appCtx, uri)
            }.onFinally {
                waitDialog.dismiss()
            }
            waitDialog.setOnCancelListener {
                task.cancel()
            }
        }
    }
    private val restoreOld = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ImportOldData.importUri(appCtx, uri)
        }
    }
    private val lanBackupQr = registerForActivityResult(QrCodeResult()) {
        it?.let(::confirmLanBackupReceive)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        manualBackupUploadWebDav =
            savedInstanceState?.getBoolean(KEY_MANUAL_UPLOAD_WEB_DAV, true) ?: true
        activity?.setTitle(R.string.backup_restore)
        activity?.addMenuProvider(this, viewLifecycleOwner)
        refreshRows()
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            LegadoMiuixTheme {
                BackupSettingsScreen(
                    entries = entries,
                    listState = listState,
                    onItemClick = ::onItemClick,
                    onItemLongClick = ::onItemLongClick,
                    onSwitchChange = ::onSwitchChange,
                )
            }
        }
        if (!LocalConfig.backupHelpVersionIsLast) {
            showHelp("webDavHelp")
        }
    }

    override fun onResume() {
        super.onResume()
        // 备份路径、WebDAV 配置可能在别的页面或弹窗里改过，回到本页时重新读一遍
        refreshRows()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_MANUAL_UPLOAD_WEB_DAV, manualBackupUploadWebDav)
    }

    // region 行数据

    private fun refreshRows() {
        entries = BackupSettingsActions.entries(requireContext())
    }

    private fun onSwitchChange(key: String, checked: Boolean) {
        val needRefresh = BackupSettingsActions.saveSwitch(requireContext(), key, checked)
        if (needRefresh) {
            refreshRows()
        } else {
            refreshRows()
        }
    }

    private fun onItemLongClick(key: String) {
        if (key == BackupSettingsActions.KEY_WEB_DAV_RESTORE) {
            restoreFromLocal()
        }
    }

    private fun onItemClick(key: String) {
        when {
            BackupSettingsActions.textKind(key) != null -> showTextDialog(key)
            key == PreferKey.backupPath -> showBackupPathSelector()
            key == PreferKey.backupContent -> backupContent()
            key == PreferKey.restoreIgnore -> backupIgnore()
            key == BackupSettingsActions.KEY_WEB_DAV_BACKUP -> backup()
            key == BackupSettingsActions.KEY_LOCAL_PASSWORD -> alertLocalPassword()
            key == PreferKey.autoBackup -> configureAutoBackup()
            key == BackupSettingsActions.KEY_WEB_DAV_RESTORE -> restore()
            key == BackupSettingsActions.KEY_LAN_BACKUP_TRANSFER -> lanBackupTransfer()
        }
    }

    override fun searchConfig(query: String): List<ConfigSearchItem> {
        val keyword = query.trim()
        if (keyword.isEmpty()) return emptyList()
        return entries.filterIsInstance<BackupSettingEntry.Item>()
            .filter { item ->
                item.title.contains(keyword, ignoreCase = true) ||
                    item.summary?.contains(keyword, ignoreCase = true) == true
            }
            .map { item ->
                ConfigSearchItem(item.title) {
                    listState.requestScrollToItem(groupIndexOf(item.key))
                    onItemClick(item.key)
                }
            }
    }

    /** 行所在的卡片下标：分组标题也占一个 item，所以按可见顺序数一遍 */
    private fun groupIndexOf(key: String): Int {
        var index = 0
        var pendingRows = false
        entries.forEach { entry ->
            when (entry) {
                is BackupSettingEntry.Section -> {
                    if (pendingRows) {
                        index++
                        pendingRows = false
                    }
                    index++
                }

                is BackupSettingEntry.Item -> {
                    if (!pendingRows) {
                        pendingRows = true
                    }
                    if (entry.key == key) return index
                }
            }
        }
        return 0
    }

    // endregion

    // region 编辑框类设置

    private fun showTextDialog(key: String) {
        val kind = BackupSettingsActions.textKind(key) ?: return
        val binding = DialogEditTextBinding.inflate(layoutInflater)
        val value = BackupSettingsActions.textValue(requireContext(), key)
        binding.editView.setText(value)
        binding.editView.setSelection(binding.editView.text.length)
        when (kind) {
            BackupSettingsActions.TextKind.PASSWORD -> {
                binding.editView.hint = getString(R.string.web_dav_pw_s)
                binding.editView.inputType =
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            }

            BackupSettingsActions.TextKind.NUMBER -> {
                binding.editView.hint = "0"
                binding.editView.inputType = InputType.TYPE_CLASS_NUMBER
            }

            BackupSettingsActions.TextKind.TEXT -> Unit
        }
        AlertDialog.Builder(requireContext())
            .setTitle(titleOf(key))
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val webDavChanged =
                    BackupSettingsActions.saveText(
                        requireContext(),
                        key,
                        binding.editView.text.toString(),
                    )
                if (webDavChanged) {
                    viewModel.upWebDavConfig()
                }
                refreshRows()
            }
            .show()
    }

    private fun titleOf(key: String): String =
        entries.filterIsInstance<BackupSettingEntry.Item>()
            .firstOrNull { it.key == key }
            ?.title
            ?: key

    // endregion

    // region 菜单

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.backup_restore, menu)
        menu.applyTint(requireContext())
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_help -> {
                showHelp("webDavHelp")
                return true
            }

            R.id.menu_import_old -> {
                restoreOld.launch()
                return true
            }

            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return false
    }

    // endregion

    // region 弹窗与流程

    private fun configureAutoBackup() {
        val binding = DialogAutoBackupBinding.inflate(layoutInflater)
        binding.enabled.isChecked = AppConfig.autoBackup
        binding.destination.check(
            if (AppConfig.autoBackupWebDav) R.id.local_webdav else R.id.local_only
        )
        binding.intervalDays.setText(AppConfig.autoBackupIntervalDays.toString())
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.auto_backup_t)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val days = binding.intervalDays.text.toString().toIntOrNull()
                        if (days == null || days < 1) {
                            binding.intervalDays.error =
                                getString(R.string.auto_backup_interval_invalid)
                            return@setOnClickListener
                        }
                        requireContext().putPrefBoolean(PreferKey.autoBackup, binding.enabled.isChecked)
                        requireContext().putPrefBoolean(
                            PreferKey.autoBackupWebDav,
                            binding.destination.checkedRadioButtonId == R.id.local_webdav,
                        )
                        requireContext().putPrefInt(PreferKey.autoBackupIntervalDays, days)
                        refreshRows()
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun alertLocalPassword() {
        alert(R.string.set_local_password, R.string.set_local_password_summary) {
            val binding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "password"
                LocalConfig.password?.let { password ->
                    editView.setText(password)
                    editView.setSelection(editView.text.length)
                }
            }
            customView { binding.root }
            okButton {
                LocalConfig.password = binding.editView.text.toString()
                refreshRows()
            }
            cancelButton()
        }
    }

    private fun showBackupPathSelector() {
        requireContext().selector(
            titleSource = R.string.backup_path,
            items = listOf(
                getString(R.string.default_path),
                getString(R.string.select_folder),
            ),
        ) { _, index ->
            when (index) {
                0 -> {
                    AppConfig.backupPath = null
                    refreshRows()
                }

                1 -> selectBackupPath.launch()
            }
        }
    }

    private fun backupContent() {
        val checkedItems = BooleanArray(BackupConfig.contentKeys.size) {
            BackupConfig.contentIsEnabled(BackupConfig.contentKeys[it])
        }
        alert(R.string.backup_content) {
            multiChoiceItems(BackupConfig.contentTitles, checkedItems) { _, which, isChecked ->
                BackupConfig.ignoreConfig[BackupConfig.contentKeys[which]] = !isChecked
            }
            onDismiss {
                BackupConfig.saveIgnoreConfig()
            }
        }
    }

    private fun backupIgnore() {
        val checkedItems = BooleanArray(BackupConfig.ignoreKeys.size) {
            BackupConfig.ignoreConfig[BackupConfig.ignoreKeys[it]] ?: false
        }
        alert(R.string.restore_ignore) {
            multiChoiceItems(BackupConfig.ignoreTitle, checkedItems) { _, which, isChecked ->
                BackupConfig.ignoreConfig[BackupConfig.ignoreKeys[which]] = isChecked
            }
            onDismiss {
                BackupConfig.saveIgnoreConfig()
            }
        }
    }

    private fun lanBackupTransfer() {
        requireContext().selector(
            titleSource = R.string.lan_backup_transfer,
            items = listOf(
                getString(R.string.lan_backup_send),
                getString(R.string.lan_backup_receive),
            ),
        ) { _, index ->
            when (index) {
                0 -> confirmLanBackupSend()
                1 -> lanBackupQr.launch()
            }
        }
    }

    private fun confirmLanBackupSend() {
        alert(
            titleResource = R.string.lan_backup_send,
            messageResource = R.string.lan_backup_send_confirm,
        ) {
            okButton { sendLanBackup() }
            cancelButton()
        }
    }

    private fun sendLanBackup() {
        lanBackupDialog?.dismiss()
        lanBackupSession?.close()
        lanBackupDialog = null
        lanBackupSession = null
        lanBackupJob?.cancel()
        waitDialog.setText(R.string.lan_backup_send).show()
        lanBackupJob = viewLifecycleOwner.lifecycleScope.launch {
            var pendingSession: LanBackupSession? = null
            try {
                val prepared = withContext(IO) {
                    val backupFile = Backup.backupForLanTransferLocked(appCtx)
                    val deviceName = AppConfig.webDavDeviceName
                        ?.takeIf { it.isNotBlank() }
                        ?: "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                    val session = LanBackupTransfer.prepare(backupFile, deviceName)
                    pendingSession = session
                    val qrCode = QRCodeUtils.createQRCode(session.qrText)
                        ?: throw NoStackTraceException("生成二维码失败")
                    session to qrCode
                }
                pendingSession = prepared.first
                ensureActive()
                showLanBackupQr(prepared.first, prepared.second)
                pendingSession = null
            } catch (error: Throwable) {
                ensureActive()
                AppLog.put("局域网备份发送失败\n${error.localizedMessage}", error)
                appCtx.toastOnUi(error.localizedMessage ?: getString(R.string.backup_fail, ""))
            } finally {
                pendingSession?.close()
                waitDialog.dismiss()
            }
        }
        waitDialog.setOnCancelListener { lanBackupJob?.cancel() }
    }

    private fun showLanBackupQr(
        session: LanBackupSession,
        qrCode: android.graphics.Bitmap,
    ) {
        lanBackupDialog?.dismiss()
        lanBackupSession?.close()
        lanBackupSession = session
        val binding = DialogLanBackupSendBinding.inflate(layoutInflater).apply {
            ivQrCode.setImageBitmap(qrCode)
        }
        lateinit var dialog: AlertDialog
        val expireTask = Runnable {
            if (lanBackupSession === session) dialog.dismiss()
        }
        dialog = alert(R.string.lan_backup_send) {
            customView { binding.root }
            cancelButton()
            onDismiss {
                binding.root.removeCallbacks(expireTask)
                if (lanBackupSession === session) {
                    lanBackupSession = null
                    session.close()
                }
                if (lanBackupDialog === dialog) {
                    lanBackupDialog = null
                }
            }
        }
        lanBackupDialog = dialog
        binding.root.postDelayed(
            expireTask,
            (session.descriptor.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L),
        )
    }

    private fun confirmLanBackupReceive(qrText: String) {
        val descriptor = LanBackupTransfer.decodeDescriptor(qrText).getOrElse { error ->
            appCtx.toastOnUi(error.localizedMessage ?: getString(R.string.lan_backup_receive))
            return
        }
        alert(
            title = getString(R.string.lan_backup_receive),
            message = getString(
                R.string.lan_backup_receive_confirm,
                Formatter.formatFileSize(requireContext(), descriptor.size),
                descriptor.deviceName.ifBlank { descriptor.hosts.first() },
            ),
        ) {
            okButton { receiveLanBackup(qrText) }
            cancelButton()
        }
    }

    private fun receiveLanBackup(qrText: String) {
        lanBackupJob?.cancel()
        waitDialog.setText(R.string.lan_backup_receive).show()
        lanBackupJob = viewLifecycleOwner.lifecycleScope.launch {
            var receivedFile: java.io.File? = null
            try {
                val received = LanBackupTransfer.receive(appCtx, qrText)
                receivedFile = received.file
                waitDialog.setText(R.string.backup)
                withContext(IO) {
                    Backup.backupBeforeLanRestoreLocked(appCtx)
                    LanBackupTransfer.requireRestoreSpace(appCtx, received.uncompressedBytes)
                }
                ensureActive()
                waitDialog.setText(R.string.restore)
                withContext(IO) {
                    Restore.restoreOrThrow(
                        appCtx,
                        received.file.toUri(),
                        lanTransfer = true,
                    )
                }
            } catch (error: Throwable) {
                ensureActive()
                AppLog.put("局域网备份接收失败\n${error.localizedMessage}", error)
                appCtx.toastOnUi(error.localizedMessage ?: getString(R.string.lan_backup_receive))
            } finally {
                receivedFile?.parentFile?.deleteRecursively()
                waitDialog.dismiss()
            }
        }
        waitDialog.setOnCancelListener { lanBackupJob?.cancel() }
    }

    fun backup() {
        requireContext().selector(
            R.string.backup,
            listOf(getString(R.string.backup_local_only), getString(R.string.backup_local_webdav)),
        ) { _, index ->
            manualBackupUploadWebDav = index == 1
            startBackup(manualBackupUploadWebDav)
        }
    }

    private fun startBackup(uploadWebDav: Boolean) {
        val backupPath = AppConfig.backupPath
        if (backupPath.isNullOrEmpty()) {
            backup(null, uploadWebDav)
        } else {
            if (backupPath.isContentScheme()) {
                lifecycleScope.launch {
                    val canWrite = withContext(IO) {
                        FileDoc.fromDir(backupPath).checkWrite()
                    }
                    if (canWrite) {
                        backup(backupPath, uploadWebDav)
                    } else {
                        backupDir.launch()
                    }
                }
            } else {
                backupUsePermission(backupPath, uploadWebDav)
            }
        }
    }

    private fun backup(backupPath: String?, uploadWebDav: Boolean) {
        waitDialog.setText("备份中…")
        waitDialog.setOnCancelListener {
            backupJob?.cancel()
        }
        waitDialog.show()
        backupJob?.cancel()
        backupJob = lifecycleScope.launch {
            try {
                Backup.backupLocked(requireContext(), backupPath, uploadWebDav)
                appCtx.toastOnUi(R.string.backup_success)
            } catch (e: Throwable) {
                ensureActive()
                AppLog.put("备份出错\n${e.localizedMessage}", e)
                appCtx.toastOnUi(
                    appCtx.getString(R.string.backup_fail, e.localizedMessage)
                )
            } finally {
                ensureActive()
                waitDialog.dismiss()
            }
        }
    }

    private fun backupUsePermission(path: String, uploadWebDav: Boolean) {
        PermissionsCompat.Builder()
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                backup(path, uploadWebDav)
            }
            .request()
    }

    fun restore() {
        waitDialog.setText(R.string.loading)
        waitDialog.setOnCancelListener {
            restoreJob?.cancel()
        }
        waitDialog.show()
        Coroutine.async {
            restoreJob = coroutineContext[Job]
            showRestoreDialog(requireContext())
        }.onError {
            AppLog.put("恢复备份出错WebDavError\n${it.localizedMessage}", it)
            if (context == null) {
                return@onError
            }
            alert {
                setTitle(R.string.restore)
                setMessage("WebDavError\n${it.localizedMessage}\n将从本地备份恢复。")
                okButton {
                    restoreFromLocal()
                }
                cancelButton()
            }
        }.onFinally {
            waitDialog.dismiss()
        }
    }

    private suspend fun showRestoreDialog(context: Context) {
        val names = withContext(IO) { AppWebDav.getBackupNames() }
        if (AppWebDav.isJianGuoYun && names.size > 700) {
            context.toastOnUi("由于坚果云限制列出文件数量，部分备份可能未显示，请及时清理旧备份")
        }
        if (names.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            withContext(Main) {
                context.selector(
                    title = context.getString(R.string.select_restore_file),
                    items = names
                ) { _, index ->
                    if (index in 0 until names.size) {
                        view?.post {
                            restoreWebDav(names[index])
                        }
                    }
                }
            }
        } else {
            throw NoStackTraceException("Web dav no back up file")
        }
    }

    private fun restoreWebDav(name: String) {
        waitDialog.setText("恢复中…")
        waitDialog.show()
        val task = Coroutine.async {
            AppWebDav.restoreWebDav(name)
        }.onError {
            AppLog.put("WebDav恢复出错\n${it.localizedMessage}", it)
            appCtx.toastOnUi("WebDav恢复出错\n${it.localizedMessage}")
        }.onFinally {
            waitDialog.dismiss()
        }
        waitDialog.setOnCancelListener {
            task.cancel()
        }
    }

    private fun restoreFromLocal() {
        restoreDoc.launch {
            title = getString(R.string.select_restore_file)
            mode = HandleFileContract.FILE
            allowExtensions = arrayOf("zip")
        }
    }

    // endregion

    override fun onDestroyView() {
        lanBackupJob?.cancel()
        lanBackupDialog?.dismiss()
        lanBackupSession?.close()
        lanBackupDialog = null
        lanBackupSession = null
        super.onDestroyView()
        waitDialog.dismiss()
    }

    override fun onStop() {
        lanBackupJob?.cancel()
        lanBackupDialog?.dismiss()
        lanBackupSession?.close()
        lanBackupDialog = null
        lanBackupSession = null
        super.onStop()
    }

    private companion object {
        const val KEY_MANUAL_UPLOAD_WEB_DAV = "manualBackupUploadWebDav"
    }
}
