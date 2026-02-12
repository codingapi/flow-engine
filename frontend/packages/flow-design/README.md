# @flow-engine/flow-design

Flow Engine 流程设计器组件库，基于 @flowgram.ai fixed-layout-editor 构建。

## 简介

`flow-design` 是 Flow Engine 的核心前端组件库，提供可视化的流程设计能力。

### 核心依赖

- `@flowgram.ai/fixed-layout-editor` - 固定布局编辑器核心
- `@flowgram.ai/fixed-semi-materials` - Semi Design 组件物料
- `@flowgram.ai/form-materials` - 表单组件物料
- `@flowgram.ai/panel-manager-plugin` - 面板管理插件
- `@flowgram.ai/minimap-plugin` - 小地图插件
- `@flowgram.ai/export-plugin` - 导出插件
- `antd` - Ant Design 组件库
- `@reduxjs/toolkit` + `react-redux` - 状态管理

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

运行测试:

```bash
pnpm run test
```

## 核心功能

### 流程设计面板

`pages/design-panel/` 目录包含流程设计的核心组件:

- `types.ts` - TypeScript 类型定义
  - `Workflow` - 工作流定义
  - `FlowNode` - 节点定义
  - `FlowForm` - 表单定义
  - `FlowNode.blocks?: FlowNode[]` - 子节点（层次化结构）

### 节点类型

支持 15 种节点类型:

**基础节点 (9种)**: StartNode, EndNode, ApprovalNode, HandleNode, NotifyNode, RouterNode, SubProcessNode, DelayNode, TriggerNode

**块节点 (3种)**: ConditionNode, ParallelNode, InclusiveNode（包含子节点 blocks）

**分支节点 (3种)**: ConditionBranchNode, ParallelBranchNode, InclusiveBranchNode

### 数据结构

#### 层次化节点结构 (Blocks)

使用 `blocks` 属性实现节点间的层次关系:

```typescript
interface FlowNode {
  id: string;
  name: string;
  type: NodeType;
  blocks?: FlowNode[];  // 子节点列表
  // ... 其他属性
}
```

#### 节点配置

```typescript
interface FlowNode {
  strategies?: NodeStrategy[];  // 节点策略
  actions?: FlowAction[];        // 节点动作
  // ... 其他属性
}
```

## 开发指南

### 添加新节点

1. 在 `types.ts` 中定义节点类型
2. 创建对应的节点配置组件
3. 注册到设计面板

### 添加新策略

1. 扩展 `NodeStrategy` 类型
2. 创建策略配置 UI 组件
3. 集成到节点配置面板

## Learn more

- [Rslib documentation](https://lib.rsbuild.io/) - Rslib 特性和 API
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
- [CLAUDE.md](../../CLAUDE.md) - 开发指南
