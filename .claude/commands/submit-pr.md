# submit-pr

你是 Flow Engine 项目的开发助手，负责将当前的变更按规范提交。

## 两种提交路径

Flow Engine 根据变更性质走不同流程：

### 路径 A：文档类变更 → 允许直接提交到 dev

**判定条件（必须全部满足）：**

- 所有变更文件路径前缀属于 `docs/`、`README.md`、`.claude/` 目录
- 不含 `packages/**`、`src/**`、`package.json`、`pnpm-workspace.yaml`、`tsconfig*.json` 等源码/构建文件
- 不含任何可执行脚本、CI 配置（`.github/**`）
- **不允许在 main 分支直接提交**，只允许在 dev 分支

满足条件时：允许在当前分支（仅限 dev 或 feature 分支）直接 commit & push，无需创建 worktree 或 feature 分支，无需走 PR。

### 路径 B：源码/工程类变更 → 严格走 feature → PR → dev 流程

**判定条件：** 只要变更涉及 `packages/**`、`src/**`、工程配置文件、CI、脚本等任意一项，即走此路径。

必须满足：

- 处于 /worktree-dev 创建的 worktree 中
- 当前分支名为 `feature/*`
- 通过所有单元测试
- 以 PR 形式合入 dev，严禁直接向 main 提交

## 执行步骤

### 第一步：扫描变更，判定路径

```bash
git status --porcelain
git diff --stat HEAD
```

根据变更文件列表判定是路径 A 还是路径 B：

- 若全部属于文档类 → 走路径 A
- 若任一属于源码/工程类 → 走路径 B
- **混合变更**：提示用户变更已跨越文档和源码两类，要求拆分为两次提交（先提文档走 A，再提代码走 B），不得混合提交

向用户明确声明：「本次变更判定为路径 X，原因：...」

---

### 路径 A：文档直接提交

**A-1：展示变更，等待用户确认**

```bash
git diff --stat HEAD
git diff HEAD              # 详细 diff
git branch --show-current  # 告知用户当前分支
```

向用户展示变更摘要并明确询问：「确认在 {current_branch} 分支直接提交这些文档变更吗？」等待"确认"或"yes"后继续。

**A-2：Commit & Push**

```bash
git add {具体文档文件}     # 精确 add，不要 git add .
git commit -m "$(cat <<'EOF'
docs: {简洁描述}

- {要点1}
- {要点2}

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
git push origin {current_branch}
```

**A-3：告知用户已提交**

告知 commit hash 和推送结果，路径 A 结束，无 PR。

---

### 路径 B：源码变更走 PR 流程

**B-1：确认 worktree 与分支**

```bash
git worktree list
git branch --show-current
git status
```

硬约束：

- 当前必须位于 worktree（不是主工作区）
- 当前分支必须是 `feature/*`
- 任一条件不满足 → 终止，提示用户使用 /worktree-dev 创建 worktree，或将变更迁移到已有的 feature worktree

**B-2：运行测试，必须全部通过**

```bash
pnpm -r run type-check     # 全部 package TS 通过
pnpm -r run test           # 全部 package 单元测试
```

任一步失败则终止，提示用户修复后重试，不得跳过。

**B-3：展示变更，等待用户确认**

```bash
git diff --stat HEAD
git diff HEAD
```

向用户展示变更摘要并明确询问：「确认提交并创建 PR（base: dev）吗？」等待"确认"或"yes"后继续。

**B-4：Commit（如用户确认）**

精确 add 涉及的文件（不要 `git add -A`）：

```bash
git add {具体变更文件}
git commit -m "$(cat <<'EOF'
feat: {简洁描述}

- {要点1}
- {要点2}
- {要点3}

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

**B-5：推送 feature 分支**

```bash
git push -u origin feature/{task-name}
```

**B-6：创建 PR（base=dev，严禁 base=main）**

```bash
gh pr create \
  --base dev \
  --head feature/{task-name} \
  --title "..." \
  --body "$(cat <<'EOF'
## 变更摘要

{列出本次实现的主要功能点}

## 对应计划

{引用 docs/plan/ 下对应的计划文件}

## 测试验证

- [ ] `pnpm -r run type-check` 通过
- [ ] `pnpm -r run test` 单测全绿
- [ ] 关键场景已手动验证

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

**B-7：输出 PR 链接**

将 PR 链接展示给用户，提示审核后合并到 dev。

## 通用注意事项

- **严禁向 main 分支创建 PR**，PR 的 `--base` 固定为 dev
- 路径 A 仅限 dev 或 feature 分支，**严禁在 main 分支直接提交**
- commit 只 add 本次任务相关的文件，不要 `git add -A`
- 未经用户明确确认，不得执行 commit 和 push
- 混合变更必须拆分提交，不得一次性混入
- PR 创建后由用户在 GitHub 审核，审核通过后由用户将 dev 合并到 main
