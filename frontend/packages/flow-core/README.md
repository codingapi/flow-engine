# @flow-engine/flow-core

Flow Engine 前端核心框架库，提供 HTTP 客户端、Hooks、Presenter 等基础能力（不包含 UI 组件）。

## 简介

`flow-core` 是 Flow Engine 的核心框架库，提供与 UI 无关的基础能力：

- HTTP 客户端封装（基于 axios）
- React Hooks 工具
- Presenter 模式实现
- 通用工具函数
- 全局状态管理

### 依赖关系

- **依赖**: 无

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

### HTTP 客户端

基于 axios 封装的 HTTP 客户端，提供:

- 请求拦截器/响应拦截器
- 统一的错误处理
- 类型安全的请求/响应
- 请求取消支持

```typescript
import { httpClient } from '@flow-engine/flow-core';

// GET 请求
const workflow = await httpClient.get<Workflow>('/api/workflows/1');

// POST 请求
const result = await httpClient.post<Workflow>('/api/workflows', workflowData);
```

### React Hooks

提供常用的业务 Hooks:

- `useWorkflow` - 工作流相关操作
- `useFlowRecord` - 流程记录相关操作
- `useApproval` - 审批相关操作
- 自定义 Hooks 工具

### Presenter 模式

实现业务逻辑与 UI 分离的 Presenter 模式:

```typescript
import { Presenter } from '@flow-engine/flow-core';

class WorkflowPresenter extends Presenter {
  async loadWorkflow(id: string): Promise<Workflow> {
    // 加载工作流
  }

  async saveWorkflow(workflow: Workflow): Promise<void> {
    // 保存工作流
  }
}
```

### 工具函数

提供通用工具函数:

- 日期格式化
- 字符串处理
- 对象深拷贝
- 类型判断

## 模块结构

```
flow-core/
├── src/
│   ├── http/           # HTTP 客户端
│   ├── hooks/          # React Hooks
│   ├── presenter/      # Presenter 基类
│   ├── utils/          # 工具函数
│   └── types/          # 基础类型定义
└── README.md
```

## 使用示例

```typescript
import { httpClient, useWorkflow, WorkflowPresenter } from '@flow-engine/flow-core';

// 使用 HTTP 客户端
const workflows = await httpClient.get<Workflow[]>('/api/workflows');

// 使用 Hooks
const { data, loading, error } = useWorkflow('wf-001');

// 使用 Presenter
const presenter = new WorkflowPresenter();
await presenter.loadWorkflow('wf-001');
```

## Learn more

- [Rslib documentation](https://lib.rsbuild.io/) - Rslib 特性和 API
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
- [CLAUDE.md](../../CLAUDE.md) - 开发指南
