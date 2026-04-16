# worktree-dev

你是 Flow Engine 项目的开发助手，负责按照 git worktree 工作流启动一个开发任务。

## 工作流说明

Flow Engine 的所有代码开发必须在 git worktree 隔离环境中进行，禁止直接在主工作区（main/dev 分支）上修改代码。

## 执行步骤

### 第一步：确认任务

询问用户本次开发对应的计划阶段或任务名称（如 "import-form-plugin"、"approval-view-extension" 等）。参考 docs/todo/ 下的任务清单。

### 第二步：创建 feature 分支和 worktree

根据任务名称构造分支名 `feature/{task-name}`，然后执行：

```bash
# 确保本地 dev 分支是最新的
git fetch origin
git checkout dev 2>/dev/null || git checkout -b dev origin/dev

# 基于 dev 分支创建 feature 分支并建立 worktree
# worktree 放在主工作区同级目录，避免干扰
git worktree add ../flow-engine-{task-name} -b feature/{task-name} origin/dev
```

例如任务名为 `import-form-plugin`，则：

- 分支名：`feature/import-form-plugin`
- Worktree 路径：`../flow-engine-import-form-plugin`

### 第三步：确认 worktree 环境

```bash
# 列出当前所有 worktree，确认新建的已存在
git worktree list
```

告知用户：

- Worktree 路径（绝对路径）
- 对应分支名
- 后续所有开发文件操作必须在该 worktree 路径下进行

### 第四步：输出开发任务清单

根据用户指定的计划阶段，从 docs/plan/ 下找到对应的计划文件，提取任务清单和验收标准，展示给用户，作为本次开发的目标。

## 注意事项

- 所有代码修改只能在 worktree 目录下进行，主工作区仅用于管理文档和配置
- 开发过程中遵循 TDD：先写测试（Red），再写实现（Green），再重构（Refactor）
- 未经用户明确要求，不得执行任何 git commit 操作
- 完成开发并通过测试后，使用 /submit-pr 提交代码
