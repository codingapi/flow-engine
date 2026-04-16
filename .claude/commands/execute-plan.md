# execute-plan

你是 Flow Engine 项目的开发助手，负责根据计划编码执行完整的开发流程。

## 用法

`/execute-plan {计划编码}`，例如 `/execute-plan 000001`

## 权限说明

本 skill 执行过程中，所有文件读写、命令执行、测试运行均为自动执行，无需用户逐一授权。用户通过调用本 skill 即视为对整个执行过程的授权。

## 执行流程

### 第一步：加载计划

根据用户提供的 6 位计划编码，在 `docs/plan/` 目录下查找对应的计划文件：

```bash
ls docs/plan/{编码}-*.md
```

如果找不到匹配的文件，终止并提示用户检查编码。

找到后，完整阅读该计划文件，提取以下信息：

- 计划名称和概述
- 文件变更清单（新增/修改的文件列表）
- 实施步骤
- 验收标准

向用户简要展示计划概述，然后立即开始执行。

### 第二步：创建 worktree 开发环境

基于计划名称构造分支名和 worktree：

```bash
# 确保本地 dev 分支是最新的
git fetch origin
git checkout dev 2>/dev/null || git checkout -b dev origin/dev

# 基于 dev 分支创建 feature 分支并建立 worktree
# 分支名格式：feature/{编码}-{计划名称}
git worktree add ../flow-engine-{编码}-{计划名称} -b feature/{编码}-{计划名称} origin/dev
```

例如编码为 `000001`，计划名称为 `import-form-plugin`，则：

- 分支名：`feature/000001-import-form-plugin`
- Worktree 路径：`../flow-engine-000001-import-form-plugin`

创建完成后，后续所有文件操作必须在该 worktree 路径下进行。

确认 worktree 环境：

```bash
git worktree list
```

### 第三步：安装依赖

在 worktree 目录下安装依赖：

```bash
cd {worktree路径}
pnpm install
```

### 第四步：按计划实施步骤逐步执行

严格按照计划文件中「7. 实施步骤」的顺序逐步执行。每个步骤需要：

1. **创建/修改文件**：按照计划中的文件变更清单和代码设计创建或修改文件
2. **编写测试（如步骤要求）**：按照计划中的测试设计编写测试代码
3. **运行测试（TDD 验证）**：

```bash
# 在 worktree 目录下运行
pnpm --filter {package-name} run test
```

确认结果：

- Red 阶段：确认测试失败（预期行为）
- Green 阶段：确认测试通过

如果测试未按预期运行，分析原因并修复，不中断流程

### 第五步：全量验证

所有实施步骤完成后，运行全量验证：

```bash
# 类型检查
pnpm -r run type-check

# 全部单元测试
pnpm -r run test
```

所有验证必须通过。如有失败，分析原因并修复。

### 第六步：更新计划状态

验证全部通过后，更新 `docs/plan/000000-index.md`（计划索引文件）中该计划的状态为「已完成」。

### 第七步：提交代码（需用户确认）

在 worktree 中完成提交前，必须向用户展示变更内容并获得确认：

**7-1：展示变更内容**

```bash
git status --porcelain
git diff --stat HEAD
```

向用户展示变更摘要，明确询问：「代码已完成并通过全部验证，确认提交并创建 PR（base: dev）吗？」

等待用户回复"确认"或"yes"后继续。如用户拒绝，告知用户可稍后自行提交。

**7-2：提交代码（如用户确认）**

```bash
# 精确 add 涉及的文件（不要 git add -A）
git add {具体变更文件}

# commit message 格式
git commit -m "$(cat <<'EOF'
feat: {计划名称的功能描述}

- {要点1}
- {要点2}
- {要点3}

计划编码: {编码}

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"

# 推送 feature 分支
git push -u origin feature/{编码}-{计划名称}
```

### 第八步：创建 PR（如用户确认）

```bash
gh pr create \
  --base dev \
  --head feature/{编码}-{计划名称} \
  --title "feat: {简洁描述}" \
  --body "$(cat <<'EOF'
## 变更摘要

{列出本次实现的主要功能点}

## 对应计划

计划编码：`{编码}`
计划文件：`docs/plan/{编码}-{计划名称}.md`

## 测试验证

- [x] `pnpm -r run type-check` 通过
- [x] `pnpm -r run test` 单测全绿

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

### 第九步：输出结果

展示给用户：

- PR 链接
- 变更文件列表
- 测试通过情况
- worktree 路径（用户可进入查看代码）

## 异常处理

- **依赖安装失败**：检查 package.json 和 pnpm-workspace.yaml 配置
- **测试持续失败**：在尝试修复 3 次后，保留当前进度，向用户报告失败原因和已完成的步骤
- **类型检查失败**：修复类型错误后重新运行
- **计划文件信息不足**：参考 docs/architecture/ 下的架构文档补充上下文

## 注意事项

- 所有代码修改只能在 worktree 目录下进行，主工作区的计划状态更新除外
- 严格遵循 TDD 流程：先测试后实现
- 遵循 CLAUDE.md 中的全部工程规范
- **严禁向 main 分支提交**，PR 的 `--base` 固定为 dev
- **未经用户明确确认，不得自动执行 commit 和 PR 创建**
