# @flow-engine/flow-types

Flow Engine 前端类型定义库，提供整个项目的 TypeScript 类型系统。

## 简介

`flow-types` 是 Flow Engine 的类型定义库，为整个前端项目提供统一的类型系统。

### 核心依赖

- `@flow-engine/flow-core` - 核心库类型引用

## Setup

安装依赖:

```bash
pnpm install
```

## 开发

构建库:

```bash
pnpm run build
```

监听模式构建:

```bash
pnpm run dev
```

## 核心类型

### 流程定义类型

- `Workflow` - 工作流定义
- `WorkflowMeta` - 工作流元数据

### 节点类型

- `FlowNode` - 节点基础类型
- `NodeType` - 节点类型枚举
- 节点具体类型: `StartNode`, `EndNode`, `ApprovalNode`, `HandleNode`, 等

### 策略类型

- `NodeStrategy` - 节点策略基础类型
- `WorkflowStrategy` - 工作流策略类型
- 策略具体类型: `MultiOperatorAuditStrategy`, `TimeoutStrategy`, 等

### 动作类型

- `FlowAction` - 动作基础类型
- `ActionType` - 动作类型枚举
- 动作具体类型: `PassAction`, `RejectAction`, `SaveAction`, 等

### 表单类型

- `FlowForm` - 表单定义
- `FormField` - 表单字段定义
- `FormFieldPermission` - 字段权限类型

### 记录类型

- `FlowRecord` - 流程记录类型
- `FlowState` - 流程状态枚举
- `RecordState` - 记录状态枚举

## 类型系统特点

### 层次化节点结构

使用 `blocks` 属性实现节点间的层次关系:

```typescript
interface FlowNode {
  id: string;
  name: string;
  type: NodeType;
  blocks?: FlowNode[];  // 子节点列表（块节点）
  strategies?: NodeStrategy[];
  actions?: FlowAction[];
}
```

### 节点分类

- **基础节点** (9种): START, END, APPROVAL, HANDLE, NOTIFY, ROUTER, SUB_PROCESS, DELAY, TRIGGER
- **块节点** (3种): CONDITION, PARALLEL, INCLUSIVE（包含子节点）
- **分支节点** (3种): CONDITION_BRANCH, PARALLEL_BRANCH, INCLUSIVE_BRANCH

## Learn more

- [Rslib documentation](https://lib.rsbuild.io/) - Rslib 特性和 API
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
- [CLAUDE.md](../../CLAUDE.md) - 开发指南
