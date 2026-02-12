# @flow-engine/flow-core

Flow Engine 前端核心库，提供与后端 API 交互的基础功能。

## 简介

`flow-core` 是 Flow Engine 的核心前端库，包含:

- HTTP 客户端封装 (基于 axios)
- API 服务接口定义
- 通用类型和工具函数

### 核心依赖

- `axios` - HTTP 客户端
- `react` + `react-dom` - React 框架

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

## 核心功能

### API 服务

提供与 Flow Engine 后端交互的 API 接口:

- 流程定义 API
- 流程实例 API
- 待办/已办 API
- 流程操作 API

### HTTP 客户端

基于 axios 封装的 HTTP 客户端，支持:

- 请求拦截
- 响应拦截
- 错误处理
- 类型安全的请求/响应

## Learn more

- [Rslib documentation](https://lib.rsbuild.io/) - Rslib 特性和 API
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
