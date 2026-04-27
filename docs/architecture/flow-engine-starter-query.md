# flow-engine-starter-query

查询层，提供流程记录和工作流定义的只读查询 API。包含流程记录的全量/待办/已办/抄送分页查询，以及工作流定义列表、选项、版本查询。

## Maven 坐标

- groupId: com.codingapi.flow
- artifactId: flow-engine-starter-query
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
com.codingapi.flow.query/
├── AutoConfiguration.java              # Spring Boot 自动配置（注册 FlowRecordQueryService Bean）
├── controller/                         # REST 查询控制器
│   ├── FlowRecordQueryController.java       # 流程记录查询 API
│   └── WorkflowQueryController.java         # 工作流定义查询 API
└── service/                            # 查询服务实现
    └── FlowRecordQueryServiceImpl.java      # FlowRecordQueryService 接口实现
```

## 核心功能

### 1. 流程记录查询 API

`FlowRecordQueryController` 提供流程记录的分页查询。

路径前缀：`/api/query/record`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | 全部流程记录（分页，按 ID 降序） |
| `/todo` | GET | 我的待办记录 |
| `/done` | GET | 我的已办记录 |
| `/notify` | GET | 我的抄送记录 |

查询服务 `FlowRecordQueryServiceImpl` 直接注入 `FlowRecordEntityRepository` 和 `FlowTodoRecordEntityRepository`，通过 `FlowRecordContentConvertor` 将 JPA 实体转换为 `FlowRecordContent` 响应对象。

关键类：`com.codingapi.flow.query.controller.FlowRecordQueryController`、`com.codingapi.flow.query.service.FlowRecordQueryServiceImpl`

### 2. 工作流定义查询 API

`WorkflowQueryController` 直接注入 JPA Repository 提供工作流定义的只读查询。

路径前缀：`/api/query/workflow`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | 工作流定义列表（分页搜索） |
| `/options` | GET | 工作流下拉选项 |
| `/versions` | GET | 指定工作流的版本列表 |

关键类：`com.codingapi.flow.query.controller.WorkflowQueryController`

## 对外 API

### FlowRecordQueryController

流程记录查询控制器，支持 Mock 模式切换。

路径：`com.codingapi.flow.query.controller.FlowRecordQueryController`

- 待办/已办/抄送接口通过 `UserContext` 或请求参数 `operatorId` 获取当前操作者
- Mock 模式通过 `mockKey` 参数激活

### WorkflowQueryController

工作流定义查询控制器，直接使用 JPA Repository。

路径：`com.codingapi.flow.query.controller.WorkflowQueryController`

## 模块规范

- 查询接口路径以 `/api/query/` 为前缀，与命令接口 `/api/cmd/` 区分
- 统一使用 `MultiResponse` 封装分页响应
- 查询服务直接注入 JPA Repository，不经过 framework 层的仓储接口
- 所有查询结果按 ID 降序排列

## 构建指令

```bash
mvn -pl flow-engine-starter-query -am compile
mvn -pl flow-engine-starter-query test
mvn -pl flow-engine-starter-query package
```
