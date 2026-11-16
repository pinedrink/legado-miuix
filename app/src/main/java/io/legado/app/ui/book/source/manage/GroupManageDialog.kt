package io.legado.app.ui.book.source.manage

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.appDb
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogGroupManageBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.compose.theme.LegadoMiuixTheme
import io.legado.app.utils.requestInputMethod
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * 分组管理对话框。
 *
 * 内容区改为 Compose（[GroupManageScreen]），分组的新增/重命名/删除仍走
 * [BookSourceViewModel] 与 `appDb.bookSourceDao`，交互逻辑没有变化。
 */
class GroupManageDialog : BaseDialogFragment(R.layout.dialog_group_manage) {

    private val viewModel: BookSourceViewModel by activityViewModels()
    private val binding by viewBinding(DialogGroupManageBinding::bind)

    private var groups by mutableStateOf<List<String>>(emptyList())

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            LegadoMiuixTheme {
                GroupManageScreen(
                    groups = groups,
                    onAdd = ::addGroup,
                    onEdit = ::editGroup,
                    onDelete = { viewModel.delGroup(it) },
                )
            }
        }
        lifecycleScope.launch {
            appDb.bookSourceDao.flowGroups().conflate().collect {
                groups = it
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun addGroup() {
        android.util.Log.d("GroupManage", "addGroup clicked")
        alert(title = getString(R.string.add_group)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotBlank()) {
                        viewModel.addGroup(it)
                    }
                }
            }
            cancelButton()
        }.requestInputMethod()
    }

    @SuppressLint("InflateParams")
    private fun editGroup(group: String) {
        alert(title = getString(R.string.group_edit)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setText(group)
            }
            customView { alertBinding.root }
            okButton {
                viewModel.upGroup(group, alertBinding.editView.text?.toString())
            }
            cancelButton()
        }.requestInputMethod()
    }

}
