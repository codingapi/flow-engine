---
name: workflow/github-issue
module: workflow
description: GitHub Issue 标准处理流程 — 定位分析 → 回复 issue → worktree 修复 → 合并 dev → 提交关闭 → 推送 GitHub → 推送后回复
status: 已实现
scope: 通用
source: 项目自有
---

## 解决什么问题

不遵守此流程会导致：

- 问题分析结论只留在本地，issue 参与者无法同步进展
- 直接在主干工作区改代码，污染工作区、难以回滚和并行处理多个 issue
- 修复完成后 issue 长期悬空，无法追溯「哪个提交解决了哪个 issue」
- 分支、worktree、合并目标不统一，发布时遗漏改动

## 适用范围

`codingapi/flow-engine` 及其关联仓库（如 `codingapi/flow-frontend`）的**所有 GitHub issue**。

## 核心原则

1. **分析可以不开 worktree，改代码必须开**：定位阶段只读代码即可；一旦确定要修改，必须新建 git worktree + 特性分支。
2. **先回复，再动手**：定位到问题后，先在对应 issue 回复根因与处理方案。
3. **只合并到 dev**：特性分支完成后合并至 `dev`；是否推送 `dev` 到 GitHub 由维护者决定，助手不得擅自 `git push`。
4. **提交即关联 issue**：提交信息使用 `Closes #<id>` 关联；是否已真正关闭取决于是否进入默认分支，未自动关闭时由维护者确认后手动关闭。
5. **推送后再回复**：代码推送到 GitHub（由维护者执行或授权）之后，才在 issue 回复修复内容、提交与验证结果；**未推送前不得发布完成回复**。

## 标准步骤

### 1. 读取 issue

```bash
gh issue view <id> --repo codingapi/flow-engine \
  --json number,title,state,labels,body,comments,author,createdAt
```

### 2. 定位问题（可不建 worktree）

- 使用代码搜索、LSP、文档（`docs/conventions`、`docs/capabilities`）定位根因；
- 明确影响范围（后端 / PC 端 / 移动端）、是否涉及前端子模块 `flow-frontend`；
- 判断是否属于已有能力缺陷，优先复用既有机制而非新增。

### 3. 回复 issue（定位结论）

将以下内容整理为中文评论：

- 问题原因（引用具体类/文件/方法）；
- 影响范围（哪些动作、端、版本）；
- 处理方案（改动点、交互变化、兼容性）；
- 交付方式（分支名、合并目标 `dev`）。

```bash
gh issue comment <id> --repo codingapi/flow-engine --body-file /tmp/issue-<id>-analysis.md
```

### 4. 创建 worktree 与特性分支

**后端主仓库**（`.worktrees/` 已被 `.gitignore` 忽略）：

```bash
git worktree add .worktrees/issue-<id> -b feature/issue-<id>-<slug> dev
cd .worktrees/issue-<id>
```

**前端子模块**（`flow-frontend/.claude/worktrees/` 已被忽略）：

```bash
cd flow-frontend
git worktree add .claude/worktrees/issue-<id> -b feature/issue-<id>-<slug> dev
cd .claude/worktrees/issue-<id>
pnpm install --frozen-lockfile --prefer-offline   # worktree 无 node_modules
```

命名约定：`feature/issue-<issue编号>-<简短英文描述>`。

### 5. 修复与验证（TDD）

- 先补测试（红灯），再实现（绿灯），最后重构；
- 前端：`npx rstest run`（单包）、`pnpm run test:<package>`、`pnpm run build:<package>`；
- 后端：`./mvnw -pl <module> test`，必要时 `./mvnw clean install`；
- **修改代码后必须本地编译/测试通过**，失败项如实报告，不得声称已验证。

### 6. 合并到 dev（不推送）

```bash
git checkout dev
git merge --no-ff feature/issue-<id>-<slug>
# 不执行 git push，由维护者决定
```

- 合并后保留或清理 worktree（`git worktree remove <path>`）；
- 若前后端均有改动，两个仓库分别合并到各自 `dev`；
- 不更新主仓库记录的 `flow-frontend` 子模块指针，除非维护者明确要求。

### 7. 提交信息与 issue 关闭

提交信息格式：

```
fix: <一句话描述> (<关联范围>)

<可选的改动说明>

Closes #<id>
```

- `Closes #<id>` 只在提交进入**默认分支**（`main`）时由 GitHub 自动关闭；
- 若先合入 `dev` 且尚未发布到 `main`，issue 保持打开，待 `dev → main` 合并时自动关闭；
- 如需提前关闭，必须由维护者确认后执行 `gh issue close <id>`，不得擅自关闭。

### 8. 推送 GitHub 后回复 issue

**前置条件：代码已推送到 GitHub**（`dev` 或其他目标分支）。推送由维护者执行或明确授权，助手不得自行推送；未推送前只可说明「修复已在本地 `dev` 完成，待推送」，不得发布完成结论。

回复内容包含：

- 根因与最终方案（与步骤 3 的差异）；
- 变更文件/能力（新增公共 API、交互变化）；
- 验证结果（测试用例数、构建结果）；
- 合入分支与提交号；
- 升级/兼容提示（如有行为变更）。

## 禁止事项

- 未经用户明确要求，不得执行 `git push` / 直接向 `main` 提交；
- 不得在主工作区（非 worktree）直接修改业务代码；
- 不得跳过测试与编译验证；
- 不得在代码推送到 GitHub 前发布完成回复（定位回复不受此限）；
- 不得在代码未合入前擅自关闭 issue；
- 不得用相对路径跨 package 引用（前端规范）。

## 检查清单

- [ ] issue 已读取并确认现状
- [ ] 定位结论已在 issue 回复
- [ ] worktree + 特性分支已创建（基于 `dev`）
- [ ] 测试先行且全量通过，构建通过
- [ ] 已合并到 `dev`（未推送）
- [ ] 提交信息包含 `Closes #<id>`
- [ ] 代码已推送到 GitHub（维护者执行或确认授权）
- [ ] 推送后已在 issue 回复修复与验证结果
