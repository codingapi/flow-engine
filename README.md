[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/codingapi/flow-engine/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.codingapi.flow/flow-engine-starter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.codingapi.flow%22%20AND%20a:%22flow-engine-starter%22)
[![Build](https://img.shields.io/github/actions/workflow/status/codingapi/flow-engine/ci.yml?label=Build&logo=github)](https://github.com/codingapi/flow-engine/actions)
[![Codecov](https://codecov.io/gh/codingapi/flow-engine/branch/main/graph/badge.svg)](https://codecov.io/gh/codingapi/flow-engine)
# Flow Engine

> 企业级流程引擎 - 可视化流程设计、动态表单配置、多节点类型流转

## 简介

Flow Engine 是一个基于 Java 17 和 Spring Boot 3.5.9 构建的企业级工作流引擎，提供完整的流程管理能力，包括可视化流程设计、动态表单配置、多节点类型流转和脚本扩展。采用前后端分离架构，同时支持 PC 端和移动端。

### 核心特性

- **19 种节点类型** - 开始、结束、审批、办理、通知、人工、条件控制、条件分支、条件Else分支、并行控制、并行分支、路由、包容控制、包容分支、包容Else分支、子流程、延迟、触发节点、人工分支
- **8 种动作类型** - 通过、拒绝、保存、加签、委派、退回、转办、自定义
- **策略驱动配置** - 所有关键配置通过策略实现，支持动态扩展
- **Groovy 脚本扩展** - 支持发起人动态匹配、审批人加载、条件判断、自定义操作等
- **多人审批模式** - 顺序审批、会签审批（可配置比例）、或签审批、随机审批
- **子流程支持** - 子流程节点挂载，支持子流程结果回传与重置机制
- **层次化节点结构** - 通过blocks属性实现节点间的层次关系，不再使用独立的边关系
- **线程安全** - 脚本运行时使用细粒度同步锁，支持不同脚本并发执行
- **自动资源清理** - 双重清理机制（阈值触发 + 定时清理）避免内存泄漏
- **完善的异常体系** - 基于 RuntimeException 的框架异常层次结构

## 项目结构

```
flow-engine
├── flow-engine-framework      # 核心流程引擎框架
├── flow-engine-starter        # Spring Boot 自动配置入口
├── flow-engine-starter-api    # REST API 层（命令操作：create / action / revoke / delete / urge / detail）
├── flow-engine-starter-infra  # 持久化层（JPA 实体、11 个仓储实现、含达梦方言）
├── flow-engine-starter-query  # 查询层（流程记录与流程定义只读 API）
├── flow-engine-example        # 示例应用（H2/达梦数据库、JWT 认证、端口 8090）
└── flow-frontend              # 前端项目（独立 Git 仓库，PC + 移动端，详见下文“前端模块架构”）
```

### 框架层包结构

`flow-engine-framework` 的包路径为 `com.codingapi.flow.*`：

| 包 | 职责 |
|------|------|
| `workflow` | 流程定义：Workflow、WorkflowVersion、运行时快照（runtime） |
| `node` | 19 种流程节点、节点工厂与节点助手 |
| `action` | 8 种流程动作与动作工厂 |
| `strategy` | 策略层：15 种节点策略 + 2 种工作流策略 |
| `script` | Groovy 脚本系统：9 种节点脚本、3 种动作脚本、运行时、注册表、工厂 |
| `transfer` | 流程定义导入导出（旧版 Schema 兼容、脚本迁移） |
| `service` | 业务服务层：FlowService 事务门面 + 各流程操作独立服务（创建 / 动作 / 撤销 / 删除 / 催办 / 详情 / 子流程重置等） |
| `query` | 流程记录查询服务 |
| `manager` | 管理器层：动作、节点、策略、操作者管理 |
| `session` | 执行上下文：FlowSession、FlowAdvice、IRepositoryHolder |
| `record` | 流程记录：FlowRecord、FlowTodoRecord、FlowTodoMerge |
| `repository` | 12 个仓储接口（持久化抽象，由 infra 模块实现） |
| `event` | 8 种流程事件：开始 / 待办 / 已办 / 完成 / 催办 / 撤销 / 删除 / 子流程重置 |
| `exception` | 框架异常体系：异常基类 + 6 种具体异常（验证 / 未找到 / 状态 / 权限 / 执行 / 导入导出） |
| `form` | 表单系统：字段定义、数据校验、字段权限、值转换 |
| `builder` | 6 种构建器（流程、节点、动作、策略、字段权限、节点映射） |
| `pojo` | 请求 / 响应对象 |
| `domain` | 领域对象：延迟任务、催办间隔、子流程上下文 |
| `context` | 各类上下文（网关、仓储持有者、动作响应、循环触发跟踪） |
| `cache` | 本地缓存（操作者线程缓存、脚本缓存、流程运行时缓存） |
| `generator` | 流程 ID 生成器网关 |
| `gateway` / `operator` | 网关接口防腐层与操作者接口 |
| `javscript` | 节点视图 JavaScript（注解扫描与缓存） |
| `mock` | Mock 模式支持（MockRepositoryHolder 及 mock 仓储 / 服务） |
| `common` / `error` / `utils` | 通用接口、错误抛出器、工具类 |

## 技术栈

### 后端

- **Java 17** - 编程语言
- **Spring Boot 3.5.9** - 应用框架
- **Spring Data JPA** - 数据持久化
- **Groovy** - 脚本引擎（动态脚本扩展）
- **Lombok** - 代码简化
- **Fastjson 2** - JSON 处理
- **JJWT** - JWT 令牌认证（示例应用）
- **Apache Commons** - 工具库（IO / Lang3 / Crypto）
- **H2 / 达梦数据库** - 数据库支持

### 前端

- **React 18** - UI 框架
- **TypeScript** - 类型安全
- **Rsbuild/Rslib** - 构建工具
- **pnpm** - 包管理器
- **Ant Design** - PC 端 UI 组件库
- **Ant Design Mobile** - 移动端 UI 组件库
- **Redux Toolkit** - 状态管理
- **Flowgram** - 流程设计器底层框架
- **CodeMirror** - 代码编辑器
- **Groovy** - 脚本语言支持

## 前端模块架构

### 模块划分原则

- **flow-core**：全局框架依赖，只包含与 UI 无关的基础能力（HTTP、状态管理、工具函数等）
- **flow-types**：全局类型定义，包含流程审批相关的业务类型（移动端和 PC 端共用）
- **flow-icons**：图标库，提供统一的图标组件
- **flow-approval-presenter**：审批展示器框架，基于 Redux 的状态管理
- **flow-design**：流程设计器功能，包含节点配置、属性面板、脚本配置等
- **flow-pc-***：PC 端专用组件库，依赖 Ant Design
- **flow-mobile-***：移动端专用组件库，依赖 Ant Design Mobile

### 前端模块依赖关系

```
flow-core (无 UI，基础框架)
    ↑       ↑
    │       └── flow-icons (图标库)
    │       └── flow-approval-presenter (审批展示器框架)
    │
flow-types (类型定义)
    ↑       ↑
    │       └── flow-pc-form
    │               ↑
    └───────→ flow-pc-ui ──→ flow-pc-approval
                        ↑
                    flow-design ──→ app-pc

flow-mobile-ui ──→ flow-mobile-form ──→ flow-mobile-approval ──→ app-mobile
```

### 前端模块说明

#### 核心模块

| 模块 | 描述 | 依赖 |
|------|------|------|
| `flow-core` | 核心框架库（HTTP、Hooks、Presenter 等），不包含 UI 组件 | 无 |
| `flow-types` | TypeScript 类型定义（流程实例、表单、审批等业务类型） | flow-core |
| `flow-icons` | 图标库 | flow-core |
| `flow-approval-presenter` | 审批展示器框架（基于 Redux 的状态管理） | flow-core, flow-types |
| `flow-design` | 流程设计器组件库（节点配置、属性面板、脚本配置等） | flow-core, flow-types, flow-icons, flow-pc-ui |

#### PC 端模块

| 模块 | 描述 | 依赖 |
|------|------|------|
| `flow-pc-ui` | PC 端基础 UI 组件库（按钮、输入框等原子组件） | flow-core |
| `flow-pc-form` | PC 端表单组件库（表单设计器、表单渲染等） | flow-core, flow-types |
| `flow-pc-approval` | PC 端审批组件库（待办/已办/审批处理等） | flow-core, flow-types, flow-icons, flow-approval-presenter, flow-pc-ui, flow-pc-form |

#### 移动端模块

| 模块 | 描述 | 依赖 |
|------|------|------|
| `flow-mobile-ui` | 移动端基础 UI 组件库 | flow-core |
| `flow-mobile-form` | 移动端表单组件库 | flow-core, flow-types |
| `flow-mobile-approval` | 移动端审批组件库 | flow-core, flow-types, flow-icons, flow-approval-presenter, flow-mobile-ui, flow-mobile-form |

## 快速开始

### 后端

```bash
# 克隆项目
git clone https://github.com/codingapi/flow-engine.git
cd flow-engine

# 构建项目
./mvnw clean install

# 运行示例项目
cd flow-engine-example
mvn spring-boot:run
```

### 前端

```bash
cd flow-frontend

# 安装依赖
pnpm install

# 构建所有包
pnpm run build

# 构建 PC 端所有组件库
pnpm run build:flow-pc

# 构建移动端所有组件库
pnpm run build:flow-mobile

# 构建特定包
pnpm run build:flow-core              # 核心框架库
pnpm run build:flow-types             # 类型定义库
pnpm run build:flow-icons             # 图标库
pnpm run build:flow-approval-presenter # 审批展示器框架
pnpm run build:flow-design            # 流程设计器组件库
pnpm run build:flow-pc-ui             # PC 端基础 UI 组件库
pnpm run build:flow-pc-form           # PC 端表单组件库
pnpm run build:flow-pc-approval       # PC 端审批组件库
pnpm run build:flow-mobile-ui         # 移动端基础 UI 组件库
pnpm run build:flow-mobile-form       # 移动端表单组件库
pnpm run build:flow-mobile-approval   # 移动端审批组件库

# 构建 PC 端应用
pnpm run build:app-pc

# 构建移动端应用
pnpm run build:app-mobile

# 启动 PC 端应用（开发模式）
pnpm run dev:app-pc

# 启动移动端应用（开发模式）
pnpm run dev:app-mobile
```

## 核心架构

### 八层架构

1. **流程层** (Workflow Layer) - 流程定义层
2. **节点层** (Node Layer) - 节点层（19种节点类型）
3. **动作层** (Action Layer) - 动作层（8种动作类型）
4. **记录层** (Record Layer) - 记录层
5. **会话层** (Session Layer) - 会话层
6. **管理器层** (Manager Layer) - 管理器层
7. **策略层** (Strategy Layer) - 策略层
8. **脚本层** (Script Layer) - 脚本层

### 设计模式

- **建造者模式** (Builder Pattern) - WorkflowBuilder、BaseNodeBuilder、FlowFormBuilder
- **工厂模式** (Factory Pattern) - FlowActionFactory、NodeFactory、NodeStrategyFactory
- **策略模式** (Strategy Pattern) - INodeStrategy、IWorkflowStrategy
- **模板方法模式** (Template Method Pattern) - BaseAction、BaseFlowNode
- **责任链模式** (Chain of Responsibility Pattern) - 动作执行链
- **组合模式** (Composite Pattern) - 带 blocks 属性的块节点层级结构

## 节点类型

### 基础节点 (9种)

| 节点类型 | 描述 | NODE_TYPE |
|---------|------|-----------|
| StartNode | 开始节点 | `START` |
| EndNode | 结束节点 | `END` |
| ApprovalNode | 审批节点 | `APPROVAL` |
| HandleNode | 办理节点 | `HANDLE` |
| NotifyNode | 通知节点 | `NOTIFY` |
| RouterNode | 路由分支 | `ROUTER` |
| SubProcessNode | 子流程节点 | `SUB_PROCESS` |
| DelayNode | 延迟节点 | `DELAY` |
| TriggerNode | 触发节点 | `TRIGGER` |

### 块节点/容器节点 (4种) - 包含子节点(blocks)

| 节点类型 | 描述 | NODE_TYPE |
|---------|------|-----------|
| ManualNode | 人工节点 | `MANUAL` |
| ConditionNode | 条件控制节点 | `CONDITION` |
| ParallelNode | 并行控制节点 | `PARALLEL` |
| InclusiveNode | 包容控制节点 | `INCLUSIVE` |

### 分支节点 (6种) - 作为块节点的子节点

| 节点类型 | 描述 | NODE_TYPE |
|---------|------|-----------|
| ManualBranchNode | 人工分支节点 | `MANUAL_BRANCH` |
| ConditionBranchNode | 条件分支节点 | `CONDITION_BRANCH` |
| ConditionElseBranchNode | 条件Else分支节点 | `CONDITION_ELSE_BRANCH` |
| ParallelBranchNode | 并行分支节点 | `PARALLEL_BRANCH` |
| InclusiveBranchNode | 包容分支节点 | `INCLUSIVE_BRANCH` |
| InclusiveElseBranchNode | 包容Else分支节点 | `INCLUSIVE_ELSE_BRANCH` |

## 动作类型

| 动作类型 | 描述 | ActionType |
|---------|------|------------|
| PassAction | 通过 | `PASS` |
| RejectAction | 拒绝 | `REJECT` |
| SaveAction | 保存 | `SAVE` |
| ReturnAction | 退回 | `RETURN` |
| TransferAction | 转办 | `TRANSFER` |
| AddAuditAction | 加签 | `ADD_AUDIT` |
| DelegateAction | 委派 | `DELEGATE` |
| CustomAction | 自定义 | `CUSTOM` |

## 多人审批模式

| 模式 | 描述 | 完成条件 |
|------|------|---------|
| SEQUENCE | 顺序审批 | 按顺序全部完成 |
| MERGE | 会签审批 | 配置比例的人员完成 |
| ANY | 或签审批 | 任意一人完成 |
| RANDOM_ONE | 随机审批 | 随机选中的人完成 |

## 异常码格式

所有框架异常使用字符串形式的错误码，格式如下：

```
category.subcategory.errorType
```

示例：
- `notFound.workflow.definition` - 流程定义未找到
- `validation.field.readOnly` - 字段只读
- `state.record.alreadyDone` - 记录已完成
- `state.record.notSupportDelete` - 记录不允许删除
- `state.node.notStart` - 记录不在开始节点
- `validation.field.required` - 必填字段为空
- `execution.script.error` - 脚本执行错误
- `workflow.transfer.schema.invalid` - 流程导入 Schema 无效

所有异常消息使用英文。

## 示例应用

示例应用（`flow-engine-example`）提供了一个开箱即用的完整演示环境：

- **默认数据库**：H2（文件数据库，默认落地 `example.db`）
- **可选数据库**：达梦（`--spring.profiles.active=dm`）
- **认证**：JWT Token 认证
- **访问端口**：8090
- **管理员账号**：启动时由 `AdminInitializer` 自动初始化

## 文档

- [CLAUDE.md](CLAUDE.md) - Claude Code 开发指南
- [docs/Integration](docs/Integration/README.md) - 集成文档（快速开始、REST API、事件、脚本、仓储与用户集成等）
- [docs/manual](docs/manual/README.md) - 使用手册
- [docs/conventions](docs/conventions/index.md) - 项目开发规范
- [docs/capabilities](docs/capabilities/index.md) - 可复用能力知识库

## 测试

### 后端测试

```bash
# 运行所有测试
./mvnw test

# 运行指定模块测试
./mvnw test -pl flow-engine-framework

# 运行指定测试类
./mvnw test -Dtest=ScriptRuntimeContextTest
```

### 前端测试

```bash
cd flow-frontend

# 运行所有测试
pnpm run test

# 运行特定包的测试
pnpm run test:flow-core              # 核心框架库测试
pnpm run test:flow-design            # 流程设计器测试
pnpm run test:flow-pc                # PC 端组件测试
pnpm run test:flow-pc-ui             # PC 端 UI 组件测试
pnpm run test:flow-pc-form           # PC 端表单组件测试
pnpm run test:flow-pc-approval       # PC 端审批组件测试
pnpm run test:flow-mobile            # 移动端组件测试
```

## 许可证

[LICENSE](LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request！
