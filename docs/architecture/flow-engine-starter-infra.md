# flow-engine-starter-infra

持久化基础设施层，为 flow-engine-framework 定义的 10 个仓储接口提供 JPA 实现。包含 JPA 实体、领域转换器、JPA 仓储及 Spring Boot 自动配置。

## Maven 坐标

- groupId: com.codingapi.flow
- artifactId: flow-engine-starter-infra
- version: 0.0.28
- packaging: jar

## 关联关系

> 以下关系由 `mvn dependency:tree` 指令结果生成，非人工推断。

### 我被哪些模块依赖

| 模块 | 依赖方式 |
|------|----------|
| flow-engine-starter-api | 直接依赖 |
| flow-engine-starter-query | 直接依赖 |
| flow-engine-starter | 直接依赖 |
| flow-engine-example | 间接依赖（经由 starter） |

### 我依赖哪些模块

| 模块 | 说明 |
|------|------|
| flow-engine-framework | 核心框架层，提供仓储接口定义和领域模型 |

### 主要外部依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| com.codingapi.springboot:springboot-starter-data-fast | 3.4.42 | 数据层快速开发框架 |
| org.springframework.boot:spring-boot-starter-data-jpa | 3.5.9 | JPA 持久化 |
| org.springframework.boot:spring-boot-starter-web | 3.5.9 | Web 支持 |
| com.h2database:h2 | 2.3.232 | 嵌入式数据库（runtime） |
| com.mysql:mysql-connector-j | 9.5.0 | MySQL 驱动（runtime） |
| org.postgresql:postgresql | 42.7.8 | PostgreSQL 驱动（runtime） |
| org.hibernate.orm:hibernate-core | 6.6.39.Final | ORM 框架 |

## 项目结构

```
com.codingapi.flow.infra/
├── AutoConfiguration.java          # Spring Boot 自动配置（注册 10 个仓储 Bean）
├── FlowJpaPackageRegistrar.java    # JPA 包注册器（注册 com.codingapi.flow.infra 到 JPA 扫描）
├── convert/                        # 领域转换器 — Entity <-> Domain Object
│   ├── WorkflowConvertor.java           # Workflow <-> WorkflowEntity
│   ├── WorkflowRuntimeConvertor.java    # WorkflowRuntime <-> WorkflowRuntimeEntity
│   ├── WorkflowVersionConvertor.java    # WorkflowVersion <-> WorkflowVersionEntity
│   ├── FlowRecordConvertor.java         # FlowRecord <-> FlowRecordEntity
│   ├── FlowRecordContentConvertor.java
│   ├── FlowTodoMargeConvertor.java      # FlowTodoMerge <-> FlowTodoMargeEntity
│   ├── FlowTodoRecordConvertor.java     # FlowTodoRecord <-> FlowTodoRecordEntity
│   ├── DelayTaskConvertor.java          # DelayTask <-> DelayTaskEntity
│   └── UrgeIntervalConvertor.java       # UrgeInterval <-> UrgeIntervalEntity
├── entity/                         # JPA 实体类
│   ├── WorkflowEntity.java              # t_flow_workflow — 流程定义主表
│   ├── WorkflowRuntimeEntity.java       # t_flow_workflow_runtime — 运行时快照表
│   ├── WorkflowVersionEntity.java       # t_flow_workflow_version — 流程版本表
│   ├── FlowRecordEntity.java            # t_flow_record — 审批记录表（核心数据表）
│   ├── FlowTodoMargeEntity.java         # t_flow_todo_marge — 待办合并表
│   ├── FlowTodoRecordEntity.java        # t_flow_todo_record — 待办记录表
│   ├── DelayTaskEntity.java             # t_flow_delay_task — 延迟任务表
│   ├── FlowOperatorAssignmentEntity.java # t_flow_operator_assignment — 操作人分配表
│   ├── ParallelControlEntity.java       # t_flow_parallel_control — 并行分支控制表
│   ├── UrgeIntervalEntity.java          # t_flow_urge_interval — 催办间隔表
│   └── convert/                    # JPA AttributeConverter — 单字段类型转换
│       ├── FlowNodeListConvertor.java       # List<IFlowNode> <-> JSON
│       ├── FormMetaConvertor.java           # FlowForm <-> JSON
│       ├── WorkflowStrategyListConvertor.java # List<IWorkflowStrategy> <-> JSON
│       ├── OperatorMatchScriptConvertor.java  # OperatorMatchScript <-> String
│       ├── FlowOperatorConvertor.java        # IFlowOperator <-> Long（通过 GatewayContext）
│       └── MapConvertor.java                 # Map<String,Object> <-> JSON（Fastjson）
├── jpa/                            # Spring Data JPA Repository 接口
│   ├── WorkflowEntityRepository.java
│   ├── WorkflowRuntimeEntityRepository.java
│   ├── WorkflowVersionEntityRepository.java
│   ├── FlowRecordEntityRepository.java
│   ├── FlowTodoMargeEntityRepository.java
│   ├── FlowTodoRecordEntityRepository.java
│   ├── DelayTaskEntityRepository.java
│   ├── FlowOperatorAssignmentEntityRepository.java
│   ├── ParallelControlEntityRepository.java
│   └── UrgeIntervalEntityRepository.java
├── pojo/                           # 数据传输对象
│   ├── WorkflowOption.java              # 流程下拉选项
│   └── WorkflowVersionOption.java       # 版本选项
└── repository/
    └── impl/                       # 仓储接口实现（framework 层接口的 JPA 实现）
        ├── WorkflowRepositoryImpl.java
        ├── WorkflowRuntimeRepositoryImpl.java
        ├── WorkflowVersionRepositoryImpl.java
        ├── FlowRecordRepositoryImpl.java
        ├── FlowTodoMergeRepositoryImpl.java
        ├── FlowTodoRecordRepositoryImpl.java
        ├── DelayTaskRepositoryImpl.java
        ├── FlowOperatorAssignmentRepositoryImpl.java
        ├── ParallelBranchRepositoryImpl.java
        └── UrgeIntervalRepositoryImpl.java
```

## 核心功能

### 1. 仓储接口实现

为 `flow-engine-framework` 定义的 10 个仓储接口提供 JPA 实现，统一采用「注入 JPA Repository + 领域转换器」模式，将领域模型与持久化细节隔离。`FlowRecordRepositoryImpl` 在保存后会回写 JPA 自增 ID 到领域对象。

关键类：`com.codingapi.flow.infra.repository.impl.*`

### 2. 实体与领域对象转换

两层转换设计：
- **AttributeConverter**（`entity/convert/`）：处理单个复杂字段与数据库列之间的类型转换，如节点列表、表单定义、策略列表等通过 JSON 序列化存储
- **领域转换器**（`convert/`）：组合多个 AttributeConverter，完成整个实体与领域对象的双向映射

关键类：`com.codingapi.flow.infra.convert.*`、`com.codingapi.flow.infra.entity.convert.*`

### 3. 自动配置

`AutoConfiguration` 在 JPA 类路径存在时自动激活，注册全部 10 个仓储 Bean 并通过 `FlowJpaPackageRegistrar` 将 `com.codingapi.flow.infra` 包注册到 JPA 实体扫描路径。

关键类：`com.codingapi.flow.infra.AutoConfiguration`

## 对外 API

本模块不对外暴露 API 接口，通过实现 framework 层的仓储接口供上层模块使用。

## 模块规范

- JPA 实体表名统一以 `t_flow_` 为前缀
- 复杂对象（节点列表、表单、策略）通过 `@Lob` + `AttributeConverter` 以 JSON 格式存储
- `FlowOperatorConvertor` 依赖 `GatewayContext` 单例进行用户对象恢复
- 仓储实现统一采用构造函数注入 JPA Repository
- 支持三种数据库：H2（开发/测试）、MySQL、PostgreSQL

## 构建指令

```bash
mvn -pl flow-engine-starter-infra -am compile
mvn -pl flow-engine-starter-infra test
mvn -pl flow-engine-starter-infra package
```
