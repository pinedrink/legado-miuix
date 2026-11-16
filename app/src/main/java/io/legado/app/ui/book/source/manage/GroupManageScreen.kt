package io.legado.app.ui.book.source.manage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme
/**
 * 分组管理对话框内容。
 *
 * 行顺序、编辑/删除行为与原来的 RecyclerView 版本一致；
 * 只是把列表换成 Miuix 卡片 + 行内图标按钮。
 */
@Composable
fun GroupManageScreen(
    groups: List<String>,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.group_manage),
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = stringResource(R.string.add_group),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (groups.isNotEmpty()) {
                item {
                    Card {
                        groups.forEachIndexed { index, group ->
                            if (index > 0) {
                                HorizontalDivider()
                            }
                            GroupRow(
                                group = group,
                                onEdit = { onEdit(group) },
                                onDelete = { onDelete(group) },
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun GroupRow(
    group: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    BasicComponent(
        title = group,
        onClick = { onEdit() },
        endActions = {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = MiuixIcons.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MiuixTheme.colorScheme.error,
                )
            }
        },
    )
}
