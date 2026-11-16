# 文档索引

本仓库的文档分为两类：给「人」看的，和给「AI 编程助手」看的。

## 给人看

| 文件 | 内容 |
| --- | --- |
| [README.md](../README.md) | 项目简介、目标、许可与上游署名 |
| [BUILDING.md](BUILDING.md) | 从零构建、签名、常见构建错误 |
| [RELEASING.md](RELEASING.md) | 发版流程、keystore 与 GitHub Secrets |
| [api.md](api.md) | Web 服务 / API 说明 |
| [../CHANGELOG.md](../CHANGELOG.md) | 变更记录 |

## 给 AI 助手看

| 文件 | 内容 |
| --- | --- |
| [../AGENTS.md](../AGENTS.md) | 项目级硬约束：Miuix 迁移范围、验证策略（L0/L1/L2）、禁止事项 |
| [ai/android-platform-api-guidelines.md](ai/android-platform-api-guidelines.md) | Android 平台 API / 生命周期 / 兼容性规范 |
| [ai/miuix-migration-log.md](ai/miuix-migration-log.md) | Miuix 迁移逐批记录：改了什么、怎么验证的、踩过哪些坑 |

> 约定：`AGENTS.md` 保持在仓库根目录（Codex / Claude Code / Cursor 等默认只读根目录的约定文件），其余 AI 文档统一放在 `docs/ai/`。
