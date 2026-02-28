---
name: git push command
description: git 代码自动化分析并提交
---

# git push command

根据当前代码调整内容，完成以下步骤：

## 执行步骤

1. **检查变更状态**：运行 `git status` 查看所有未跟踪和已修改的文件
2. **查看变更详情**：运行 `git diff` 查看已暂存和未暂存的变更内容
3. **查看最近提交**：运行 `git log` 查看最近的提交信息风格
4. **分析变更内容**：总结变更的性质（新功能/修复/重构/测试/文档等）
5. **添加文件**：使用 `git add` 添加相关文件（避免使用 -A 或 . 一次性添加所有文件，注意排除敏感文件如 .env、credentials 等）
6. **创建提交**：创建新提交，提交信息遵循项目风格，格式为：
   - 标题：简短描述（不超过 50 字符）
   - 正文：详细说明（如果需要）
   - 尾部：添加 Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
7. **验证提交**：运行 `git status` 确认提交成功
8. **推送到远程**：运行 `git push` 将本地提交推送到远程仓库

## 注意事项

- 永远不要使用 `git add -A` 或 `git add .`，应指定具体文件名
- 永远不要使用破坏性命令如 `git push --force`、`git reset --hard`、`git checkout .`、`git restore .`、`git clean -f`
- 永远不要跳过 hooks（--no-verify）或跳过签名（--no-gpg-sign）
- 如果 pre-commit hook 失败，修复问题后创建新的提交（不要 amend）
- 永远不要直接推送到 main/master 分支
- 如果需要推送，创建新分支后推送

## 输出要求

完成后返回 PR URL（如果有创建 PR）