# 13 - API 接口设计

## 概述

Flow Engine 提供 RESTful API 接口，分为命令型接口(CMD)和查询型接口(Query)。

## API 分层

| 层 | 前缀 | 说明 |
|----|------|------|
| 命令层 | `/api/cmd/` | 写操作接口 |
| 查询层 | `/api/query/` | 读操作接口 |

## 命令接口 (CMD)

### 工作流命令接口

**Controller**: `WorkflowController`
**基础路径**: `/api/cmd/workflow`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/create` | 创建工作流 |
| POST | `/save` | 保存工作流 |
| GET | `/load` | 加载工作流 |
| POST | `/remove` | 删除工作流 |
| POST | `/updateVersionName` | 更新版本名称 |
| GET | `/meta` | 获取工作流元信息 |
| POST | `/changeVersion` | 切换版本 |
| POST | `/deleteVersion` | 删除版本 |
| POST | `/changeState` | 启用/禁用工作流 |
| POST | `/import` | 导入工作流 |
| GET | `/export` | 导出工作流 |
| POST | `/create-node` | 创建节点 |
| POST | `/mock` | 创建Mock实例 |
| POST | `/cleanMock` | 清理Mock |

### 流程记录命令接口

**Controller**: `FlowRecordController`
**基础路径**: `/api/cmd/record`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/create` | 创建流程 |
| POST | `/action` | 执行动作 |
| GET | `/detail` | 获取流程详情 |
| POST | `/processNodes` | 获取流程节点列表 |
| POST | `/urge` | 催办 |
| POST | `/revoke` | 撤销 |

### 请求/响应示例

**创建工作流**

```
POST /api/cmd/workflow/create
Content-Type: application/json

{
  "name": "请假流程",
  "description": "员工请假申请流程",
  "formMeta": { "fields": [...] },
  "nodes": [...]
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "请假流程",
    "version": "1.0"
  }
}
```

**执行动作**

```
POST /api/cmd/record/action
Content-Type: application/json

{
  "recordId": 1,
  "actionType": "Pass",
  "formData": { "comment": "同意" }
}
```

## 查询接口 (Query)

### 工作流查询接口

**Controller**: `WorkflowQueryController`
**基础路径**: `/api/query/workflow`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 工作流列表查询 |
| GET | `/options` | 工作流选项列表 |
| GET | `/versions` | 版本列表 |

### 流程记录查询接口

**Controller**: `FlowRecordQueryController`
**基础路径**: `/api/query/record`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/todo` | 待办记录查询 |
| GET | `/done` | 已办记录查询 |
| GET | `/cc` | 抄送记录查询 |
| GET | `/initiated` | 我发起的记录 |

## 统一响应格式

### 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

### 错误响应

```json
{
  "code": -1,
  "message": "error message",
  "data": null
}
```

### 响应码说明

| 响应码 | 说明 |
|--------|------|
| 0 | 成功 |
| -1 | 系统错误 |
| 1001 | 参数错误 |
| 1002 | 权限错误 |
| 1003 | 业务校验失败 |
| 2001 | 工作流不存在 |
| 2002 | 流程记录不存在 |
| 2003 | 节点不存在 |
| 2004 | 动作不支持 |

## 认证机制

API 认证采用 **JWT Token** 机制。

### 请求头

```
Authorization: Bearer <token>
```

## API 调用关系

```
┌─────────────────────────────────────────────────────────────┐
│                        Client                                │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│WorkflowCtrl   │  │FlowRecordCtrl│  │FlowRecordQry │
│/api/cmd/      │  │/api/cmd/     │  │/api/query/   │
│workflow/*     │  │record/*      │  │record/*      │
└───────────────┘  └───────────────┘  └───────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│WorkflowService│  │ FlowService   │  │FlowRecord    │
│               │  │               │  │QueryService  │
└───────────────┘  └───────────────┘  └───────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                  flow-engine-framework                       │
│     Repository  │  Manager  │  Node  │  Action  │  Script   │
└─────────────────────────────────────────────────────────────┘
```

## 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│              flow-engine-starter-api                         │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  WorkflowController  │  FlowRecordController        │   │
│   │  WorkflowQueryController  │  FlowRecordQueryCtrl   │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    flow-engine-framework                     │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  FlowService  │  WorkflowService  │  各 Manager     │   │
│   │  Node模块  │  Action模块  │  Strategy模块  │  Script模块 │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                 flow-engine-starter-infra                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  Repository Impl  │  JPA Repository  │  Convertor  │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```
