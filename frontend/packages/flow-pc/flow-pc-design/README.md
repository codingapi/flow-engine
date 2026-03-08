# @flow-engine/flow-pc-design

Flow Engine PC 端流程设计器组件库，提供节点配置、属性面板、脚本配置等功能。

## 简介

`flow-pc-design` 是 Flow Engine PC 端的流程设计器组件库，提供：

- 流程设计器（可视化流程设计）
- 节点配置面板（15 种节点类型配置）
- 属性面板（节点属性编辑）
- 脚本配置器（Groovy 脚本编辑）
- 变量选择器
- 策略配置组件

### 依赖关系

- **依赖**: `@flow-engine/flow-core`, `@flow-engine/flow-types`, `@flow-engine/flow-pc-ui`

## Setup

安装依赖:

```bash
pnpm install
```

## 开发

构建组件库:

```bash
pnpm run build
```

监听模式构建:

```bash
pnpm run dev
```

## 核心功能

### 流程设计器

可视化流程设计器，支持：

- 节点拖拽添加
- 节点连线
- 节点配置
- 流程预览
- 流程校验
- 流程导入/导出

```typescript
import { WorkflowDesigner } from '@flow-engine/flow-pc-design';

<WorkflowDesigner
  workflow={workflow}
  onChange={handleWorkflowChange}
  onValidate={handleValidate}
/>
```

### 节点配置

支持 15 种节点类型的配置：

**基础节点 (9 种)**:
- `StartNode` - 开始节点
- `EndNode` - 结束节点
- `ApprovalNode` - 审批节点
- `HandleNode` - 办理节点
- `NotifyNode` - 通知节点
- `RouterNode` - 路由节点
- `SubProcessNode` - 子流程节点
- `DelayNode` - 延迟节点
- `TriggerNode` - 触发节点

**块节点 (3 种)**:
- `ConditionNode` - 条件节点
- `ParallelNode` - 并行节点
- `InclusiveNode` - 包容节点

**分支节点 (3 种)**:
- `ConditionBranchNode` - 条件分支
- `ParallelBranchNode` - 并行分支
- `InclusiveBranchNode` - 包容分支

### 属性面板

节点属性编辑：

- 基本信息配置
- 审批人配置
- 表单权限配置
- 通知配置
- 超时策略配置

### 脚本配置器

Groovy 脚本编辑支持：

- 语法高亮
- 代码补全
- 语法检查
- 变量提示
- TypeScript/Groovy 转换

```typescript
import { ScriptEditor } from '@flow-engine/flow-pc-design';

<ScriptEditor
  value={script}
  onChange={handleScriptChange}
  scriptType="CONDITION"
  variables={availableVariables}
/>
```

### 变量选择器

流程变量选择：

- 表单字段变量
- 流程实例变量
- 系统内置变量
- 自定义变量

```typescript
import { VariablePicker } from '@flow-engine/flow-pc-design';

<VariablePicker
  variables={variables}
  onSelect={handleVariableSelect}
  filterTypes={['FORM_FIELD', 'INSTANCE']}
/>
```

### 策略配置

节点策略配置：

- 多人审批策略（会签/或签）
- 超时策略
- 通知策略
- 权限策略

## 模块结构

```
flow-pc-design/
├── src/
│   ├── designer/       # 流程设计器
│   ├── node-config/    # 节点配置组件
│   ├── property-panel/ # 属性面板
│   ├── script/         # 脚本编辑器
│   ├── variable/       # 变量选择器
│   ├── strategy/       # 策略配置
│   ├── components/     # 公共组件
│   └── index.ts        # 统一导出
└── README.md
```

## 使用示例

### 流程设计器

```typescript
import { WorkflowDesigner } from '@flow-engine/flow-pc-design';
import type { Workflow } from '@flow-engine/flow-types';

const MyWorkflowDesigner = () => {
  const [workflow, setWorkflow] = useState<Workflow>({ ... });

  return (
    <WorkflowDesigner
      workflow={workflow}
      onChange={setWorkflow}
      onSave={handleSave}
      onPreview={handlePreview}
    />
  );
};
```

### 脚本编辑器

```typescript
import { ScriptEditor } from '@flow-engine/flow-pc-design';
import type { ScriptType } from '@flow-engine/flow-pc-design';

const MyScriptEditor = () => {
  const [script, setScript] = useState('');

  return (
    <ScriptEditor
      value={script}
      onChange={setScript}
      scriptType="CONDITION"
      variables={variables}
      onInsertVariable={(variable) => {
        setScript(prev => prev + variable);
      }}
    />
  );
};
```

## Learn more

- [Rslib documentation](https://lib.rsbuild.io/) - Rslib 特性和 API
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
- [CLAUDE.md](../../../CLAUDE.md) - 开发指南
