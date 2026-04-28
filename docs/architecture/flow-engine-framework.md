# flow-engine-framework

Flow Engine 核心工作流引擎框架，提供流程定义、节点执行、动作驱动、策略配置、Groovy 脚本运行时、仓储接口等基础能力。所有其他模块均依赖本模块。

## Maven 坐标

- groupId: com.codingapi.flow
- artifactId: flow-engine-framework
- version: 0.0.28
- packaging: jar

## 关联关系

> 以下关系由 `mvn dependency:tree` 指令结果生成，非人工推断。

### 我被哪些模块依赖

| 模块 | 依赖方式 |
|------|----------|
| flow-engine-starter-infra | 直接依赖 |
| flow-engine-starter | 直接依赖 |
| flow-engine-starter-api | 间接依赖（经由 starter-infra） |
| flow-engine-starter-query | 间接依赖（经由 starter-infra） |
| flow-engine-example | 间接依赖（经由 starter） |

### 我依赖哪些模块

无。本模块是 reactor 内最底层模块，不依赖其他 reactor 模块。

### 主要外部依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| org.apache.groovy:groovy | 4.0.29 | 脚本运行时引擎 |
| org.apache.groovy:groovy-json | 4.0.29 | Groovy JSON 处理 |
| org.apache.groovy:groovy-xml | 4.0.29 | Groovy XML 处理 |
| commons-io:commons-io | 2.17.0 | IO 工具库 |
| org.apache.commons:commons-lang3 | 3.20.0 | 字符串/对象工具 |
| com.codingapi.springboot:springboot-starter | 3.4.42 | 基础框架（用户体系、持久化、序列化） |
| org.springframework.boot:spring-boot | 3.5.9 | Spring Boot 核心 |
| com.fasterxml.jackson.core:jackson-databind | 2.19.4 | JSON 序列化 |
| org.projectlombok:lombok | 1.18.42 | 编译期代码生成（optional） |

## 项目结构

```
com.codingapi.flow/
├── action/                    # 动作模块 — 定义流程操作行为
│   ├── IFlowAction.java       # 动作接口：type/enable/generateRecords/run
│   ├── BaseAction.java        # 动作基类
│   ├── ActionType.java        # 动作类型枚举（8 种：SAVE/PASS/REJECT/ADD_AUDIT/DELEGATE/RETURN/TRANSFER/CUSTOM）
│   ├── ActionDisplay.java     # 动作展示控制
│   ├── actions/               # 具体动作实现
│   │   ├── PassAction.java
│   │   ├── RejectAction.java
│   │   ├── ReturnAction.java
│   │   ├── SaveAction.java
│   │   ├── AddAuditAction.java
│   │   ├── DelegateAction.java
│   │   ├── TransferAction.java
│   │   └── CustomAction.java
│   └── factory/
│       └── FlowActionFactory.java  # 动作工厂
├── builder/                   # 建造者模块 — 构建流程对象
│   ├── ActionBuilder.java
│   ├── BaseNodeBuilder.java
│   ├── FormFieldPermissionsBuilder.java
│   ├── NodeMapBuilder.java
│   ├── NodeStrategyBuilder.java
│   └── WorkflowStrategyBuilder.java
├── common/                    # 公共能力
│   ├── ICopyAbility.java      # 深拷贝接口
│   └── IMapConvertor.java     # Map 序列化/反序列化接口
├── context/                   # 上下文模块
│   ├── ActionResponseContext.java   # 动作响应上下文
│   ├── GatewayContext.java          # 网关上下文（全局单例）
│   └── RepositoryHolderContext.java # 仓储持有者上下文
├── domain/                    # 领域模型
│   ├── DelayTask.java              # 延迟任务
│   ├── DelayTaskManager.java       # 延迟任务管理器
│   └── UrgeInterval.java           # 催办间隔
├── error/
│   └── ErrorThrow.java        # 错误抛出工具
├── event/                     # 事件模块 — 流程异步事件
│   ├── IFlowEvent.java             # 事件标记接口
│   ├── FlowRecordStartEvent.java   # 流程启动事件
│   ├── FlowRecordTodoEvent.java    # 待办生成事件
│   ├── FlowRecordDoneEvent.java    # 审批完成事件
│   ├── FlowRecordFinishEvent.java  # 流程结束事件
│   └── FlowRecordUrgeEvent.java    # 催办事件
├── exception/                 # 异常体系
│   ├── FlowException.java
│   ├── FlowExecutionException.java
│   ├── FlowNotFoundException.java
│   ├── FlowPermissionException.java
│   ├── FlowStateException.java
│   └── FlowValidationException.java
├── form/                      # 表单模块 — 表单结构定义与字段权限
│   ├── FlowForm.java               # 表单领域模型（主表单+子表单）
│   ├── FlowFormBuilder.java
│   ├── FormData.java
│   ├── FormField.java              # 字段定义
│   ├── FieldAttribute.java
│   ├── DataType.java               # 字段数据类型枚举
│   ├── IValueConvertor.java
│   ├── ValueConvertorContext.java
│   ├── convertor/                  # 类型转换器（Boolean/Double/Integer/Long/String）
│   └── permission/
│       ├── FormFieldPermission.java
│       └── PermissionType.java     # 权限类型（读/写/隐藏）
├── gateway/
│   └── FlowOperatorGateway.java    # 操作者防腐层接口
├── manager/                   # 管理器模块 — 业务管理器
│   ├── ActionManager.java
│   ├── FlowNodeManager.java
│   ├── FlowNodeState.java
│   ├── NodeStrategyManager.java
│   ├── OperatorManager.java
│   └── WorkflowStrategyManager.java
├── mock/                      # Mock 支持 — 测试用模拟实现
│   ├── MockInstance.java
│   ├── MockInstanceFactory.java
│   ├── MockRepositoryHolder.java
│   ├── repository/            # Mock 仓储实现
│   └── service/
├── node/                      # 节点模块 — 19 种流程节点类型
│   ├── IFlowNode.java              # 节点顶层接口
│   ├── IBlockNode.java             # 阻塞节点接口
│   ├── IDisplayNode.java           # 展示节点接口
│   ├── BaseFlowNode.java           # 节点基类
│   ├── BaseAuditNode.java          # 审批节点基类
│   ├── NodeType.java               # 19 种节点类型枚举
│   ├── factory/
│   │   └── NodeFactory.java        # 节点工厂
│   ├── helper/
│   │   ├── BackNodeHelper.java     # 退回节点辅助
│   │   └── ParallelNodeRelationHelper.java  # 并行节点关系辅助
│   └── nodes/                      # 具体节点实现
│       ├── StartNode.java
│       ├── EndNode.java
│       ├── ApprovalNode.java
│       ├── HandleNode.java
│       ├── ConditionNode.java / ConditionBranchNode.java / ConditionElseBranchNode.java
│       ├── InclusiveNode.java / InclusiveBranchNode.java / InclusiveElseBranchNode.java
│       ├── ParallelNode.java / ParallelBranchNode.java
│       ├── ManualNode.java / ManualBranchNode.java
│       ├── DelayNode.java
│       ├── NotifyNode.java
│       ├── TriggerNode.java
│       ├── RouterNode.java
│       └── SubProcessNode.java
├── operator/
│   └── IFlowOperator.java          # 流程操作者接口（继承 IUser）
├── pojo/                      # POJO — 请求/响应数据结构
│   ├── body/                       # 请求体
│   │   └── FlowAdviceBody.java
│   ├── request/                    # 请求对象
│   │   ├── FlowCreateRequest.java
│   │   ├── FlowActionRequest.java
│   │   ├── FlowDetailRequest.java
│   │   ├── FlowRevokeRequest.java
│   │   ├── FlowUrgeRequest.java
│   │   └── FlowProcessNodeRequest.java
│   └── response/                   # 响应对象
│       ├── ActionResponse.java
│       ├── FlowContent.java
│       ├── FlowRecordContent.java
│       ├── FlowOperator.java
│       ├── NodeOption.java
│       └── ProcessNode.java
├── query/
│   └── FlowRecordQueryService.java # 流程记录查询服务接口
├── record/                    # 流程记录领域模型
│   ├── FlowRecord.java             # 流转记录（核心数据模型）
│   ├── FlowTodoMerge.java          # 待办合并记录
│   └── FlowTodoRecord.java         # 待办记录
├── repository/                # 仓储接口 — 基础设施层需实现
│   ├── WorkflowRepository.java
│   ├── WorkflowVersionRepository.java
│   ├── WorkflowRuntimeRepository.java
│   ├── FlowRecordRepository.java
│   ├── FlowTodoRecordRepository.java
│   ├── FlowTodoMergeRepository.java
│   ├── FlowOperatorAssignmentRepository.java
│   ├── DelayTaskRepository.java
│   ├── ParallelBranchRepository.java
│   └── UrgeIntervalRepository.java
├── script/                    # 脚本模块 — Groovy 脚本运行时
│   ├── runtime/
│   │   ├── ScriptRuntimeContext.java    # 脚本运行时上下文（单例，线程安全）
│   │   ├── FlowScriptContext.java       # 流程脚本上下文
│   │   └── IBeanFactory.java
│   ├── registry/
│   │   ├── IScriptRegistry.java         # 脚本注册接口
│   │   ├── DefaultScriptRegistry.java   # 默认注册实现
│   │   └── ScriptRegistryContext.java
│   ├── action/                     # 动作相关脚本
│   │   ├── CustomScript.java
│   │   └── RejectActionScript.java
│   ├── node/                       # 节点相关脚本
│   │   ├── ConditionScript.java
│   │   ├── RouterNodeScript.java
│   │   ├── OperatorLoadScript.java
│   │   ├── OperatorMatchScript.java
│   │   ├── NodeTitleScript.java
│   │   ├── TriggerScript.java
│   │   ├── ErrorTriggerScript.java
│   │   └── SubProcessScript.java
│   ├── request/                    # 脚本请求对象
│   │   ├── GroovyScriptRequest.java
│   │   ├── GroovyScriptBind.java
│   │   └── GroovyWorkflowRequest.java
│   └── ScriptDefaultConstants.java
├── service/                   # 服务模块 — 核心业务逻辑
│   ├── WorkflowService.java         # 流程设计器服务（保存/版本管理/导入导出）
│   ├── FlowRecordService.java       # 流程记录查询与持久化
│   ├── FlowRecordSaveService.java   # 记录底层保存（包级私有）
│   ├── FlowService.java             # 流程运行服务入口
│   └── impl/                        # 服务实现
│       ├── FlowCreateService.java       # 创建流程
│       ├── FlowActionService.java       # 执行动作
│       ├── FlowDetailService.java       # 流程详情
│       ├── FlowRevokeService.java       # 撤销流程
│       ├── FlowUrgeService.java         # 催办
│       ├── FlowDelayTriggerService.java # 延迟触发
│       └── FlowProcessNodeService.java  # 流程节点信息
├── session/                   # 会话模块
│   ├── FlowSession.java             # 流程会话（单次操作的完整上下文）
│   ├── FlowAdvice.java              # 审批意见
│   └── IRepositoryHolder.java       # 资源持有者接口
├── strategy/                  # 策略模块 — 可插拔的节点/工作流策略
│   ├── node/                        # 节点级策略
│   │   ├── INodeStrategy.java           # 节点策略接口
│   │   ├── BaseStrategy.java
│   │   ├── NodeStrategyFactory.java
│   │   ├── AdviceStrategy.java          # 审批意见策略
│   │   ├── DelayStrategy.java           # 延迟策略
│   │   ├── FormFieldPermissionStrategy.java  # 表单权限策略
│   │   ├── MultiOperatorAuditStrategy.java   # 多人审批策略
│   │   ├── SameOperatorAuditStrategy.java    # 相同人审批策略
│   │   ├── NodeTitleStrategy.java       # 节点标题策略
│   │   ├── OperatorLoadStrategy.java    # 操作者加载策略
│   │   ├── OperatorSelectType.java      # 操作者选择类型
│   │   ├── RecordMergeStrategy.java     # 记录合并策略
│   │   ├── ResubmitStrategy.java        # 重新提交策略
│   │   ├── RevokeStrategy.java          # 撤销策略
│   │   ├── RouterStrategy.java          # 路由策略
│   │   ├── SubProcessStrategy.java      # 子流程策略
│   │   ├── TimeoutStrategy.java         # 超时策略
│   │   ├── TriggerStrategy.java         # 触发器策略
│   │   ├── ErrorTriggerStrategy.java    # 异常触发策略
│   │   └── WorkflowStrategyFactory.java
│   └── workflow/                    # 工作流级策略
│       ├── IWorkflowStrategy.java
│       ├── BaseStrategy.java
│       ├── InterfereStrategy.java      # 干预策略
│       ├── UrgeStrategy.java           # 催办策略
│       └── WorkflowStrategyFactory.java
├── utils/                     # 工具类
│   ├── Base64Utils.java
│   ├── RandomUtils.java
│   └── Sha256Utils.java
└── workflow/                  # 工作流定义模块
    ├── Workflow.java               # 流程定义领域模型
    ├── WorkflowVersion.java        # 流程版本
    ├── WorkflowBuilder.java        # 流程构建器
    └── runtime/
        └── WorkflowRuntime.java    # 运行时配置
```

## 核心功能

### 1. 流程定义与版本管理

`Workflow` 是流程定义的核心领域模型，包含节点树、策略列表、表单定义。`WorkflowVersion` 支持多版本管理，`WorkflowRuntime` 在流程启动时创建运行时快照，隔离流程定义变更对运行中实例的影响。

关键类：`com.codingapi.flow.workflow.Workflow`、`com.codingapi.flow.service.WorkflowService`

### 2. 节点引擎

`IFlowNode` 定义了节点的完整生命周期：`handle()` 驱动流转递归，`isFinish()` 判断完成，`generateCurrentRecords()` 生成待办记录，`filterBranches()` 处理条件分支。`NodeFactory` 负责根据 `NodeType` 枚举创建节点实例。

支持 19 种节点类型，覆盖审批、条件/包容/并行网关、延迟、抄送、子流程、触发器等 BPMN 2.0 标准结构。

关键类：`com.codingapi.flow.node.IFlowNode`、`com.codingapi.flow.node.NodeType`、`com.codingapi.flow.node.factory.NodeFactory`

### 3. 动作驱动

`IFlowAction` 定义了节点上的操作行为。每种动作类型（通过/拒绝/退回/加签/委派/转办/自定义等）对应一个实现类，通过 `generateRecords()` 生成流转记录，`run()` 触发后续流程执行。

关键类：`com.codingapi.flow.action.IFlowAction`、`com.codingapi.flow.action.ActionType`、`com.codingapi.flow.action.factory.FlowActionFactory`

### 4. 策略体系

采用两层策略设计：
- **节点级策略**（`INodeStrategy`）：审批方式、表单权限、超时、延迟、触发器等
- **工作流级策略**（`IWorkflowStrategy`）：干预、催办

策略通过 `IMapConvertor` 序列化为 Map 存储，通过 `ICopyAbility` 支持深拷贝。

关键类：`com.codingapi.flow.strategy.node.INodeStrategy`、`com.codingapi.flow.strategy.work.IWorkflowStrategy`

### 5. Groovy 脚本运行时

`ScriptRuntimeContext` 提供线程安全的脚本执行环境，使用脚本哈希值进行细粒度同步锁。支持条件判断、路由选择、操作者匹配、触发器等场景的动态脚本。

关键类：`com.codingapi.flow.script.runtime.ScriptRuntimeContext`

### 6. 仓储接口

定义了 10 个仓储接口，本模块仅提供接口定义，具体实现由 `flow-engine-starter-infra` 模块提供。通过 `IRepositoryHolder` 统一聚合所有仓储和服务能力。

关键类：`com.codingapi.flow.session.IRepositoryHolder`、`com.codingapi.flow.repository.*`

### 7. 会话与事件

`FlowSession` 封装单次流程操作的完整上下文（操作者、流程定义、节点、表单、记录），是不可变对象。`IFlowEvent` 定义了流程异步事件体系（启动/待办/完成/结束/催办）。

关键类：`com.codingapi.flow.session.FlowSession`、`com.codingapi.flow.event.IFlowEvent`

## 对外 API

### IFlowNode

流程节点顶层接口，定义节点生命周期。

路径：`com.codingapi.flow.node.IFlowNode`

| 方法 | 说明 |
|------|------|
| `handle(FlowSession)` | 判断是否继续流转（true 继续递归，false 停止） |
| `isFinish(FlowSession)` | 判断节点是否已完成 |
| `generateCurrentRecords(FlowSession)` | 构建当前节点的流程记录 |
| `filterBranches(List, FlowSession)` | 根据条件过滤匹配的分支 |
| `blocks()` | 获取子节点列表 |

### IFlowAction

流程动作接口，定义节点上可执行的操作。

路径：`com.codingapi.flow.action.IFlowAction`

| 方法 | 说明 |
|------|------|
| `type()` | 动作类型 |
| `enable()` | 是否可用 |
| `generateRecords(FlowSession)` | 执行动作并生成记录 |
| `run(FlowSession)` | 触发流程执行 |

### IRepositoryHolder

资源持有者接口，统一管理运行时服务与数据访问。

路径：`com.codingapi.flow.session.IRepositoryHolder`

| 方法 | 说明 |
|------|------|
| `getWorkflowService()` | 获取流程定义服务 |
| `getFlowRecordService()` | 获取记录查询服务 |
| `getFlowOperatorGateway()` | 获取操作者网关 |
| `createFlowService()` | 创建流程运行服务 |
| `createFlowActionService()` | 创建动作执行服务 |
| `saveRecord` / `saveRecords` | 保存流程记录 |

### FlowOperatorGateway

操作者防腐层接口，解耦引擎与外部用户系统。

路径：`com.codingapi.flow.gateway.FlowOperatorGateway`

| 方法 | 说明 |
|------|------|
| `get(long id)` | 按 ID 获取操作者 |
| `findByIds(List<Long>)` | 批量查询操作者 |

## 模块规范

- 仓储接口定义在 `repository/` 包下，实现由 `flow-engine-starter-infra` 提供
- 领域模型（Workflow、FlowRecord、FlowForm 等）使用 Lombok 简化代码
- 策略类实现 `INodeStrategy` / `IWorkflowStrategy` 接口，并通过 `IMapConvertor` 支持序列化
- 脚本通过 `ScriptRuntimeContext` 单例执行，使用脚本哈希锁保证线程安全
- 事件类实现 `IFlowEvent` 接口，由 Spring 异步机制驱动

## 构建指令

```bash
mvn -pl flow-engine-framework -am compile
mvn -pl flow-engine-framework test
mvn -pl flow-engine-framework package
```
