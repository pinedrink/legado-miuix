# NOTICE

## 本项目与上游的关系

**Legado Miuix**（本仓库）是 [LegadoTeam/legado](https://github.com/LegadoTeam/legado) 的**修改版/衍生作品**，
并非上游官方发布，也未获得上游维护者的背书。

- 上游许可：GNU General Public License v3.0
- 本项目许可：GNU General Public License v3.0（见 [LICENSE](LICENSE)）
- 完整对应源码：本仓库（含全部修改）

## 修改声明（GPLv3 §5(a)）

依据 GPLv3 第 5 条，修改过的文件应带有「已修改」的显著声明。本项目自 2026-11 起对上游源码做 UI 层改造，
具体批次、涉及文件与验证记录见：

- [docs/ai/miuix-migration-log.md](docs/ai/miuix-migration-log.md)

主要修改范围：

1. 引入 `miuix`（`top.yukonga.miuix.kmp`，Apache-2.0）与 Jetpack Compose，替换顶栏、底栏、对话框、列表卡片、开关/单选/复选、进度条等控件；
2. 新增 `io.legado.app.ui.compose`（主题桥接、`MiuixTopBarView`、`MiuixSearchBar` 等）与 `ReadMenuBottomBar`；
3. 书籍信息页折叠顶栏、阅读器菜单与设置弹窗样式；
4. 未改动数据库结构、书源规则执行、阅读引擎、网络与备份协议。

## 第三方组件

第三方组件的版权与许可由各自权利人保留，详见：

- `app/src/main/assets/LICENSE.md`（GPLv3 全文，随 APK 分发，应用内「关于 → 开源许可」可查看）
- `app/src/main/assets/licenses/htmlunit-core-js/5.3.0-legado.4/`（改造版 htmlunit-core-js 的许可、NOTICE、源码说明与 SHA-256）
- `third_party/maven/org/htmlunit/htmlunit-core-js/5.3.0-legado.4/SOURCE.md`
- `gradle/libs.versions.toml`（所有依赖及其版本；其中 miuix 为 Apache-2.0）

分发的 APK 中保留了上述许可文本；如发现遗漏，请提交 issue。
