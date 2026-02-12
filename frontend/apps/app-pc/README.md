# Flow Engine PC 端应用

Flow Engine 的 PC 端流程管理应用。

## 简介

`app-pc` 是 Flow Engine 的 PC 端 Web 应用，提供完整的流程管理功能。

### 核心依赖

- `@flow-engine/flow-design` - 流程设计器组件库
- `@flow-engine/flow-core` - 核心 API 库
- `@flow-engine/flow-types` - TypeScript 类型定义
- `antd` - Ant Design 组件库
- `react-router` + `react-router-dom` - 路由管理

## Setup

安装依赖:

```bash
pnpm install
```

## 开发

启动开发服务器，应用将在 [http://localhost:3000](http://localhost:3000) 访问。

```bash
pnpm run dev
```

## 构建

构建生产版本:

```bash
pnpm run build
```

预览生产构建:

```bash
pnpm run preview
```

## 核心功能

### 流程设计

- 可视化流程设计器
- 15 种节点类型支持
- 节点配置面板
- 表单设计
- 高级设置

### 流程管理

- 流程定义管理
- 流程实例管理
- 待办/已办列表
- 流程历史记录

### 流程操作

- 通过/拒绝审批
- 保存草稿
- 加签/委派
- 退回/转办
- 流程撤回

## Learn more

- [Rsbuild documentation](https://rsbuild.rs) - Rsbuild 特性和 API
- [Rspack documentation](https://rspack.rs) - 底层打包工具
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
- [AGENTS.md](./AGENTS.md) - 开发指南
