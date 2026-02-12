# Flow Engine 移动端应用

Flow Engine 的移动端流程管理应用（开发中）。

## 简介

`app-mobile` 是 Flow Engine 的移动端 Web 应用，将提供简化的移动操作界面。

> **注意**: 此应用目前处于开发阶段，功能尚未完整实现。

### 核心依赖

- `@flow-engine/flow-core` - 核心 API 库
- `@flow-engine/flow-types` - TypeScript 类型定义
- `antd` - Ant Design 组件库（移动端适配）
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

## 计划功能

### 移动端优化

- 响应式设计适配移动设备
- 触摸优化的交互体验
- 简化的流程处理界面

### 核心功能

- 待办任务列表
- 流程审批操作
- 流程进度查看
- 消息通知

## Learn more

- [Rsbuild documentation](https://rsbuild.rs) - Rsbuild 特性和 API
- [Rspack documentation](https://rspack.rs) - 底层打包工具
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
