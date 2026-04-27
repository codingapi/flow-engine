# flow-engine-starter-api

REST API 层，提供流程引擎的 HTTP 接口。包含流程定义管理（增删改查、导入导出、版本管理）和流程运行操作（创建、审批、撤销、催办）两大类 API。

## Maven 坐标

- groupId: com.codingapi.flow
- artifactId: flow-engine-starter-api
- version: 0.0.26
- packaging: jar

## 关联关系

> 以下关系由 `mvn dependency:tree` 指令结果生成，非人工推断。

### 我被哪些模块依赖

| 模块 | 依赖方式 |
|------|----------|
| flow-engine-starter | 直接依赖 |
| flow-engine-example | 间接依赖（经由 starter） |

### 我依赖哪些模块

| 模块 | 说明 |
|------|------|
| flow-engine-starter-infra | 持久化基础设施层（间接获得 framework 能力） |

### 主要外部依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| org.springframework.boot:spring-boot-starter-web | 3.5.9 | REST API 支持 |

## 项目结构

```
com.codingapi.flow.api/
├── AutoConfiguration.java          # Spring Boot 自动配置（@ComponentScan 扫描本包）
├── controller/                     # REST 控制器
│   ├── WorkflowController.java          # 流程定义管理 API
│   └── FlowRecordController.java        # 流程运行操作 API
└── pojo/                           # 请求/响应数据结构
    ├── BackNodeRequest.java              # 退回节点请求
    ├── NodeCreateRequest.java            # 创建节点请求
    ├── WorkflowMeta.java                 # 流程元数据响应（含动作选项列表）
    └── WorkflowUpdateVersionNameRequest.java  # 更新版本名称请求
```

## 核心功能

### 1. 流程定义管理 API

`WorkflowController` 提供流程设计器的完整 CRUD 操作。

路径前缀：`/api/cmd/workflow`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/create` | POST | 创建空白流程定义 |
| `/save` | POST | 保存流程定义（含版本保存） |
| `/load` | GET | 加载流程定义 JSON |
| `/remove` | POST | 删除流程 |
| `/changeState` | POST | 切换启用/禁用状态 |
| `/meta` | GET | 获取流程元数据（表单+动作选项） |
| `/import` | POST | 导入流程定义 |
| `/export` | GET | 导出流程定义为 JSON 文件 |
| `/create-node` | POST | 创建指定类型的节点 |
| `/changeVersion` | POST | 切换版本 |
| `/deleteVersion` | POST | 删除版本 |
| `/updateVersionName` | POST | 更新版本名称 |
| `/mock` | POST | 创建 Mock 实例（用于测试） |
| `/cleanMock` | POST | 清理 Mock 实例 |

关键类：`com.codingapi.flow.api.controller.WorkflowController`

### 2. 流程运行操作 API

`FlowRecordController` 提供流程实例的运行时操作。

路径前缀：`/api/cmd/record`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/create` | POST | 创建流程实例 |
| `/action` | POST | 执行审批动作 |
| `/detail` | GET | 查看流程详情 |
| `/processNodes` | POST | 获取流程节点信息 |
| `/revoke` | POST | 撤销流程 |
| `/urge` | POST | 催办 |

关键类：`com.codingapi.flow.api.controller.FlowRecordController`

## 对外 API

### WorkflowController

流程定义管理控制器，注入 `WorkflowService`、`WorkflowRepository`、`FlowOperatorGateway`。

路径：`com.codingapi.flow.api.controller.WorkflowController`

- 支持通过 `UserContext` 获取当前操作者
- Mock 模式：通过 `mockKey` 参数切换为 Mock 实例
- 导出功能直接写入 HttpServletResponse 流

### FlowRecordController

流程运行操作控制器，注入 `FlowService`。

路径：`com.codingapi.flow.api.controller.FlowRecordController`

- 支持通过 `operatorId` 请求参数或 `UserContext` 获取当前操作者
- Mock 模式：通过 `mockKey` 参数切换为 Mock 实例的 FlowService

## 模块规范

- 所有接口路径以 `/api/cmd/` 为前缀
- 统一使用 `Response` / `SingleResponse` / `MultiResponse` 封装响应
- 操作者通过 `UserContext` 或请求参数 `operatorId` 获取
- Mock 机制通过 `MockInstanceFactory` 支持，使用 `mockKey` 参数激活

## 构建指令

```bash
mvn -pl flow-engine-starter-api -am compile
mvn -pl flow-engine-starter-api test
mvn -pl flow-engine-starter-api package
```
