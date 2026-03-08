# Flow Engine

> 企业级流程引擎 - 可视化流程设计、动态表单配置、多节点类型流转

## 简介

Flow Engine 是一个基于 Java 17 和 Spring Boot 3.5.9 构建的企业级工作流引擎，提供完整的流程管理能力，包括可视化流程设计、动态表单配置、多节点类型流转和脚本扩展。采用前后端分离架构，同时支持 PC 端和移动端。

### 核心特性

- **15 种节点类型** - 开始、结束、审批、办理、通知、条件控制、条件分支、并行控制、并行分支、路由、包容控制、包容分支、子流程、延迟、触发节点
- **8 种动作类型** - 通过、拒绝、保存、加签、委派、退回、转办、自定义
- **策略驱动配置** - 所有关键配置通过策略实现，支持动态扩展
- **Groovy 脚本扩展** - 支持发起人动态匹配、审批人加载、条件判断、自定义操作等
- **多人审批模式** - 顺序审批、会签审批（可配置比例）、或签审批、随机审批
- **层次化节点结构** - 通过blocks属性实现节点间的层次关系，不再使用独立的边关系
- **线程安全** - 脚本运行时使用细粒度同步锁，支持不同脚本并发执行
- **自动资源清理** - 双重清理机制（阈值触发 + 定时清理）避免内存泄漏
- **完善的异常体系** - 基于 RuntimeException 的框架异常层次结构

## 项目结构

```
flow-engine
├── flow-engine-framework         # 核心流程引擎框架
│   └── src/main/java/com/codingapi/flow
│       ├── action                # 动作层
│       │   ├── actions           # 动作实现（8个类）
│       │   │   ├── PassAction    # 通过动作
│       │   │   ├── RejectAction  # 拒绝动作
│       │   │   ├── SaveAction    # 保存动作
│       │   │   ├── ReturnAction  # 退回动作
│       │   │   ├── TransferAction # 转办动作
│       │   │   ├── AddAuditAction # 加签动作
│       │   │   ├── DelegateAction # 委派动作
│       │   │   └── CustomAction  # 自定义动作
│       │   ├── factory           # FlowActionFactory 动作工厂
│       │   ├── ActionDisplay     # 动作显示元数据
│       │   ├── ActionType        # 动作类型枚举（8种）
│       │   ├── BaseAction        # 动作抽象基类
│       │   └── IFlowAction       # 动作接口
│       ├── backup                # 流程备份
│       │   └── WorkflowBackup    # 流程备份管理
│       ├── builder               # 构建器（6种）
│       │   ├── ActionBuilder             # 动作构建器
│       │   ├── BaseNodeBuilder            # 节点构建器基类
│       │   ├── FormFieldPermissionsBuilder # 字段权限构建器
│       │   ├── NodeMapBuilder             # 节点映射构建器
│       │   ├── NodeStrategyBuilder        # 节点策略构建器
│       │   └── WorkflowStrategyBuilder    # 工作流策略构建器
│       ├── common                # 通用接口
│       │   ├── ICopyAbility      # 复制能力接口
│       │   └── IMapConvertor     # Map转换接口
│       ├── context               # 上下文
│       │   ├── GatewayContext           # 网关上下文
│       │   └── RepositoryHolderContext  # 仓储持有者上下文
│       ├── domain                # 领域对象
│       │   ├── DelayTask         # 延迟任务
│       │   ├── DelayTaskManager  # 延迟任务管理器
│       │   └── UrgeInterval      # 催办间隔
│       ├── error                 # 错误处理
│       │   └── ErrorThrow        # 错误抛出器
│       ├── event                 # 事件系统（5种）
│       │   ├── IFlowEvent                # 事件接口
│       │   ├── FlowRecordStartEvent      # 流程开始事件
│       │   ├── FlowRecordTodoEvent       # 待办事件
│       │   ├── FlowRecordDoneEvent       # 已办事件
│       │   ├── FlowRecordFinishEvent     # 流程完成事件
│       │   └── FlowRecordUrgeEvent       # 催办事件
│       ├── exception             # 异常系统（6种）
│       │   ├── FlowException             # 异常基类
│       │   ├── FlowValidationException   # 参数验证异常
│       │   ├── FlowNotFoundException     # 资源未找到异常
│       │   ├── FlowStateException        # 状态异常
│       │   ├── FlowPermissionException   # 权限异常
│       │   ├── FlowConfigException       # 配置异常
│       │   └── FlowExecutionException    # 执行异常
│       ├── form                  # 表单系统
│       │   ├── permission        # 字段权限
│       │   │   ├── FormFieldPermission # 字段权限实体
│       │   │   └── PermissionType       # 权限类型枚举
│       │   ├── FormData          # 表单数据容器
│       │   ├── FormFieldMeta     # 字段元数据
│       │   ├── FormMeta          # 表单元数据
│       │   └── FormMetaBuilder   # 表单构建器
│       ├── gateway               # 网关接口防腐层
│       │   └── FlowOperatorGateway # 操作者网关
│       ├── manager               # 管理器层
│       │   ├── ActionManager     # 动作管理器
│       │   ├── NodeStrategyManager # 节点策略管理器
│       │   ├── OperatorManager   # 操作者管理器
│       │   ├── FlowNodeManager  # 节点管理器
│       │   ├── FlowNodeState    # 节点状态分类
│       │   └── WorkflowStrategyManager # 工作流策略管理器
│       ├── node                  # 节点层
│       │   ├── nodes             # 节点实现（15种）
│       │   │   ├── StartNode     # 开始节点
│       │   │   ├── EndNode       # 结束节点
│       │   │   ├── ApprovalNode  # 审批节点
│       │   │   ├── HandleNode    # 办理节点
│       │   │   ├── NotifyNode    # 通知节点
│       │   │   ├── ConditionNode # 条件控制节点（块节点）
│       │   │   ├── ConditionBranchNode # 条件分支节点
│       │   │   ├── ParallelNode  # 并行控制节点（块节点）
│       │   │   ├── ParallelBranchNode    # 并行分支节点
│       │   │   ├── RouterNode    # 路由节点
│       │   │   ├── InclusiveNode # 包容控制节点（块节点）
│       │   │   ├── InclusiveBranchNode  # 包容分支节点
│       │   │   ├── SubProcessNode # 子流程节点
│       │   │   ├── DelayNode     # 延迟节点
│       │   │   └── TriggerNode   # 触发节点
│       │   ├── factory           # NodeFactory 节点工厂
│       │   ├── helper            # 节点助手
│       │   │   ├── BackNodeHelper           # 退回节点助手
│       │   │   └── ParallelNodeRelationHelper # 并行关系助手
│       │   ├── IBlockNode       # 块节点接口
│       │   ├── BaseFlowNode      # 节点抽象基类
│       │   ├── BaseAuditNode     # 审批节点抽象基类
│       │   ├── IFlowNode         # 节点接口
│       │   └── NodeType          # 节点类型枚举
│       ├── operator              # 操作者接口
│       │   └── IFlowOperator     # 操作者接口
│       ├── pojo                  # 数据对象
│       │   ├── body              # FlowAdviceBody 请求体
│       │   ├── request           # 请求对象
│       │   │   ├── FlowActionRequest  # 动作请求
│       │   │   ├── FlowCreateRequest  # 创建请求
│       │   │   ├── FlowRevokeRequest  # 撤回请求
│       │   │   └── FlowUrgeRequest    # 催办请求
│       │   └── response          # 响应对象
│       │       ├── FlowOperator   # 流程操作者
│       │       └── FlowContent   # 流程内容
│       ├── record                # 流程记录
│       │   ├── FlowRecord        # 执行记录（TODO/DONE状态）
│       │   ├── FlowTodoRecord    # 待办记录
│       │   └── FlowTodoMerge     # 待办合并记录
│       ├── repository            # 仓储接口（持久化抽象）
│       │   ├── WorkflowRepository
│       │   ├── FlowRecordRepository
│       │   ├── WorkflowBackupRepository
│       │   ├── ParallelBranchRepository
│       │   ├── DelayTaskRepository
│       │   ├── UrgeIntervalRepository
│       │   ├── FlowTodoRecordRepository
│       │   └── FlowTodoMergeRepository
│       ├── script                # 脚本系统
│       │   ├── node              # 节点脚本（8种）
│       │   │   ├── OperatorMatchScript  # 发起人匹配脚本
│       │   │   ├── OperatorLoadScript   # 审批人加载脚本
│       │   │   ├── NodeTitleScript      # 节点标题脚本
│       │   │   ├── ConditionScript      # 条件判断脚本
│       │   │   ├── RouterNodeScript     # 路由脚本
│       │   │   ├── SubProcessScript     # 子流程脚本
│       │   │   ├── TriggerScript        # 触发脚本
│       │   │   └── ErrorTriggerScript   # 异常触发脚本
│       │   ├── runtime           # 脚本运行时
│       │   │   ├── ScriptRuntimeContext # Groovy脚本执行环境
│       │   │   ├── FlowScriptContext    # 脚本上下文
│       │   │   └── IBeanFactory         # Bean工厂接口
│       │   └── action            # 动作脚本（2种）
│       │       ├── RejectActionScript   # 拒绝动作脚本
│       │       └── CustomScript         # 自定义动作脚本
│       ├── service               # 服务层
│       │   ├── impl              # 服务实现
│       │   │   ├── FlowCreateService    # 流程创建服务
│       │   │   ├── FlowActionService    # 流程动作服务
│       │   │   ├── FlowDelayTriggerService # 延迟触发服务
│       │   │   ├── FlowRevokeService    # 流程撤回服务
│       │   │   ├── FlowUrgeService      # 流程催办服务
│       │   │   └── FlowDetailService   # 流程详情服务
│       │   └── FlowService       # 服务接口
│       ├── session               # 会话层
│       │   ├── FlowSession       # 执行上下文
│       │   └── FlowAdvice        # 审批参数（意见、签名、退回节点等）
│       ├── strategy              # 策略层（18种：16种节点策略 + 2种工作流策略）
│       │   ├── node                # 节点策略
│       │   │   ├── MultiOperatorAuditStrategy  # 多人审批策略
│       │   │   ├── TimeoutStrategy          # 超时策略
│       │   │   ├── SameOperatorAuditStrategy # 同一操作者审批策略
│       │   │   ├── RecordMergeStrategy      # 记录合并策略
│       │   │   ├── ResubmitStrategy         # 重新提交策略
│       │   │   ├── AdviceStrategy           # 审批意见策略
│       │   │   ├── OperatorLoadStrategy     # 审批人加载策略
│       │   │   ├── ErrorTriggerStrategy     # 异常触发策略
│       │   │   ├── NodeTitleStrategy        # 节点标题策略
│       │   │   ├── FormFieldPermissionStrategy # 字段权限策略
│       │   │   ├── DelayStrategy            # 延迟策略
│       │   │   ├── TriggerStrategy          # 触发策略
│       │   │   ├── RouterStrategy          # 路由策略
│       │   │   ├── SubProcessStrategy       # 子流程策略
│       │   │   ├── RevokeStrategy           # 撤回策略
│       │   │   ├── NodeStrategyFactory      # 节点策略工厂
│       │   │   ├── BaseStrategy             # 节点策略抽象基类
│       │   │   └── INodeStrategy            # 节点策略接口
│       │   └── workflow            # 工作流策略
│       │       ├── InterfereStrategy        # 干预策略
│       │       ├── UrgeStrategy             # 催办策略
│       │       ├── WorkflowStrategyFactory  # 工作流策略工厂
│       │       ├── BaseStrategy             # 工作流策略抽象基类
│       │       └── IWorkflowStrategy        # 工作流策略接口
│       ├── utils                 # 工具类
│       │   ├── RandomUtils       # 随机工具
│       │   └── Sha256Utils       # SHA256加密工具
│       └── workflow              # 流程层
│           ├── Workflow          # 流程对象
│           └── WorkflowBuilder   # 流程构建器
│   └── src/test/java             # 测试代码
├── flow-engine-starter           # Spring Boot Starter
├── flow-engine-starter-infra     # 持久化层 Starter
├── flow-engine-example           # 示例项目
└── frontend                      # 前端项目
    ├── apps
    │   ├── app-pc                # PC端应用
    │   └── app-mobile            # 移动端应用
    └── packages
        ├── flow-core            # 核心 API 库
        ├── flow-types           # TypeScript 类型定义库
        └── flow-pc              # PC 端组件库
            ├── flow-pc-ui       # PC 端基础 UI 组件库
            ├── flow-pc-form     # PC 端表单相关组件
            ├── flow-pc-design   # PC 端流程设计器组件
            └── flow-pc-approval # PC 端审批页面组件
```

## 技术栈

### 后端

- **Java 17** - 编程语言
- **Spring Boot 3.5.9** - 应用框架
- **Groovy** - 脚本引擎
- **Lombok** - 代码简化
- **Fastjson** - JSON处理
- **Apache Commons** - 工具库

### 前端

- **React** - UI框架
- **TypeScript** - 类型安全
- **Rsbuild** - 构建工具
- **pnpm** - 包管理器

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
cd frontend

# 安装依赖
pnpm install

# 构建所有包
pnpm run build

# 构建 PC 端所有组件库
pnpm run build:flow-pc

# 构建特定组件库
pnpm run build:flow-core    # 核心 API 库
pnpm run build:flow-types   # 类型定义库
pnpm run build:flow-pc-ui   # 基础 UI 组件库
pnpm run build:flow-pc-form # 表单组件库
pnpm run build:flow-pc-design # 设计器组件库
pnpm run build:flow-pc-approval # 审批组件库

# 启动 PC 端应用
pnpm run dev:app-pc

# 启动移动端应用
pnpm run dev:app-mobile
```

## 核心架构

### 八层架构

1. **流程层** (Workflow Layer) - 流程定义层
2. **节点层** (Node Layer) - 节点层（15种节点类型）
3. **动作层** (Action Layer) - 动作层（8种动作类型）
4. **记录层** (Record Layer) - 记录层
5. **会话层** (Session Layer) - 会话层
6. **管理器层** (Manager Layer) - 管理器层
7. **策略层** (Strategy Layer) - 策略层
8. **脚本层** (Script Layer) - 脚本层

### 设计模式

- **建造者模式** (Builder Pattern)
- **工厂模式** (Factory Pattern)
- **策略模式** (Strategy Pattern)
- **模板方法模式** (Template Method Pattern)
- **单例模式** (Singleton Pattern)
- **责任链模式** (Chain of Responsibility Pattern)
- **组合模式** (Composite Pattern)

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

### 块节点/容器节点 (3种) - 包含子节点(blocks)

| 节点类型 | 描述 | NODE_TYPE |
|---------|------|-----------|
| ConditionNode | 条件控制节点 | `CONDITION` |
| ParallelNode | 并行控制节点 | `PARALLEL` |
| InclusiveNode | 包容控制节点 | `INCLUSIVE` |

### 分支节点 (3种) - 作为块节点的子节点

| 节点类型 | 描述 | NODE_TYPE |
|---------|------|-----------|
| ConditionBranchNode | 条件分支节点 | `CONDITION_BRANCH` |
| ParallelBranchNode | 并行分支节点 | `PARALLEL_BRANCH` |
| InclusiveBranchNode | 包容分支节点 | `INCLUSIVE_BRANCH` |

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
- `permission.field.readOnly` - 字段只读
- `state.record.alreadyDone` - 记录已完成
- `validation.field.required` - 必填字段为空
- `execution.script.error` - 脚本执行错误

所有异常消息使用英文。

## 文档

- [CLAUDE.md](CLAUDE.md) - Claude Code 指南

## 测试

```bash
# 运行所有测试
./mvnw test

# 运行指定模块测试
./mvnw test -pl flow-engine-framework

# 运行指定测试类
./mvnw test -Dtest=ScriptRuntimeContextTest
```

## 许可证

[LICENSE](LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request！
