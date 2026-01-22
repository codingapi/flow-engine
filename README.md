# Flow Engine

> 企业级流程引擎 - 可视化流程设计、动态表单配置、多节点类型流转

## 项目简介

Flow Engine 是一个基于 Java 17 和 Spring Boot 3.5.9 构建的企业级流程引擎，提供完整的流程管理能力，包括可视化流程设计、动态表单配置、多节点类型流转、脚本扩展等功能。采用前后端分离架构，支持 PC 端和移动端。

### 核心特性

- **12 种节点类型** - 支持开始、结束、审批、办理、通知、条件分支、并行分支、路由分支、包容分支、子流程、延迟、触发节点
- **9 种操作类型** - 通过、拒绝、保存、加签、委派、退回、转办、自定义操作
- **策略驱动配置** - 所有关键配置通过策略实现，支持动态扩展
- **Groovy 脚本扩展** - 支持动态发起人匹配、审批人加载、条件判断、自定义操作等
- **多人审批模式** - 顺序审批、会签（可设置比例）、或签、随机审批
- **线程安全** - 脚本运行时采用细粒度同步锁，支持并发执行不同脚本
- **自动资源清理** - 双重清理机制（阈值触发 + 定时清理），避免内存泄漏
- **完善的异常体系** - 基于 RuntimeException 的框架异常体系

## 项目结构

```
flow-engine
├── flow-engine-framework         # 流程引擎核心框架
│   └── src/main/java/com.codingapi.flow
│       ├── action                # 操作层
│       │   ├── actions           # 具体操作实现（9种）
│       │   │   ├── PassAction    # 通过操作
│       │   │   ├── RejectAction  # 拒绝操作
│       │   │   ├── SaveAction    # 保存操作
│       │   │   ├── ReturnAction  # 退回操作
│       │   │   ├── TransferAction # 转办操作
│       │   │   ├── AddAuditAction # 加签操作
│       │   │   ├── DelegateAction # 委派操作
│       │   │   └── CustomAction  # 自定义操作
│       │   ├── factory           # FlowActionFactory 操作工厂
│       │   ├── ActionType        # 操作类型枚举
│       │   ├── BaseAction        # 操作抽象基类
│       │   └── IFlowAction       # 操作接口
│       ├── backup                # 流程备份
│       │   └── WorkflowBackup    # 工作流备份管理
│       ├── builder               # 构建器（5种）
│       │   ├── ActionBuilder             # 操作构建器
│       │   ├── BaseNodeBuilder            # 节点构建器基类
│       │   ├── FormFieldPermissionsBuilder # 字段权限构建器
│       │   ├── NodeMapBuilder             # 节点映射构建器
│       │   └── NodeStrategyBuilder        # 节点策略构建器
│       ├── context               # 上下文
│       │   ├── GatewayContext           # 网关上下文
│       │   └── RepositoryHolderContext  # 仓储持有者上下文
│       ├── delay                 # 延迟任务
│       │   ├── DelayTask         # 延迟任务
│       │   └── DelayTaskManager  # 延迟任务管理器
│       ├── edge                  # 节点连线
│       │   └── FlowEdge          # 流程连线
│       ├── error                 # 错误处理
│       │   └── ErrorThrow        # 错误抛出
│       ├── event                 # 事件系统（5种）
│       │   ├── IFlowEvent                # 事件接口
│       │   ├── FlowRecordStartEvent      # 流程开始事件
│       │   ├── FlowRecordTodoEvent       # 待办事件
│       │   ├── FlowRecordDoneEvent       # 已办事件
│       │   └── FlowRecordFinishEvent     # 流程完成事件
│       ├── exception             # 异常体系（6种异常）
│       │   ├── FlowException             # 异常基类
│       │   ├── FlowValidationException   # 参数验证异常
│       │   ├── FlowNotFoundException     # 资源未找到异常
│       │   ├── FlowStateException        # 状态异常
│       │   ├── FlowPermissionException   # 权限异常
│       │   ├── FlowConfigException       # 配置异常
│       │   └── FlowExecutionException    # 执行异常
│       ├── form                  # 表单系统
│       │   ├── permission        # 字段权限（READ/WRITE/NONE）
│       │   ├── FormData          # 表单数据
│       │   ├── FormMeta          # 表单元数据
│       │   └── FormMetaBuilder   # 表单构建器
│       ├── gateway               # 网关接口防腐层
│       │   └── FlowOperatorGateway # 操作员网关
│       ├── operator              # 操作人接口
│       │   └── IFlowOperator     # 操作员接口
│       ├── node                  # 节点层
│       │   ├── nodes             # 具体节点实现（12种）
│       │   │   ├── StartNode     # 开始节点
│       │   │   ├── EndNode       # 结束节点
│       │   │   ├── ApprovalNode  # 审批节点
│       │   │   ├── HandleNode    # 办理节点
│       │   │   ├── NotifyNode    # 通知节点
│       │   │   ├── BranchNodeBranchNode # 条件分支
│       │   │   ├── ParallelBranchNode    # 并行分支
│       │   │   ├── RouterNode    # 路由节点
│       │   │   ├── InclusiveBranchNode  # 包容分支
│       │   │   ├── SubProcessNode # 子流程节点
│       │   │   ├── DelayNode     # 延迟节点
│       │   │   └── TriggerNode   # 触发节点
│       │   ├── factory           # NodeFactory 节点工厂
│       │   ├── helper            # ParallelNodeRelationHelper 并行辅助类
│       │   ├── manager           # 节点管理器
│       │   │   ├── ActionManager     # 操作管理器
│       │   │   ├── OperatorManager    # 操作员管理器
│       │   │   ├── StrategyManager    # 策略管理器
│       │   │   └── FieldPermissionManager # 字段权限管理器
│       │   ├── BaseFlowNode      # 节点抽象基类
│       │   ├── BaseAuditNode     # 审批节点抽象基类
│       │   └── IFlowNode         # 节点接口
│       ├── pojo                  # 数据对象
│       │   ├── body              # FlowAdviceBody 请求体
│       │   └── request           # FlowActionRequest, FlowCreateRequest
│       ├── record                # 流程记录
│       │   └── FlowRecord        # 执行记录（TODO/DONE状态）
│       ├── repository            # 仓储接口（数据持久化抽象）
│       │   ├── WorkflowRepository
│       │   ├── FlowRecordRepository
│       │   ├── WorkflowBackupRepository
│       │   ├── ParallelBranchRepository
│       │   └── DelayTaskRepository
│       ├── script                # 脚本系统
│       │   ├── node              # 节点脚本（6种）
│       │   │   ├── OperatorMatchScript  # 发起人匹配脚本
│       │   │   ├── OperatorLoadScript   # 审批人加载脚本
│       │   │   ├── NodeTitleScript      # 节点标题脚本
│       │   │   ├── ConditionScript      # 条件判断脚本
│       │   │   ├── RouterNodeScript     # 路由脚本
│       │   │   ├── SubProcessScript     # 子流程脚本
│       │   │   ├── TriggerScript        # 触发脚本
│       │   │   └── ErrorTriggerScript   # 错误触发脚本
│       │   ├── runtime           # 脚本运行时
│       │   │   ├── ScriptRuntimeContext # Groovy脚本执行环境
│       │   │   ├── FlowScriptContext    # 脚本上下文
│       │   │   └── IBeanFactory         # Bean工厂接口
│       │   └── action            # 操作脚本
│       ├── service               # 服务层
│       │   ├── impl              # 服务实现
│       │   │   ├── FlowCreateService    # 流程创建服务
│       │   │   ├── FlowActionService    # 流程操作服务
│       │   │   └── FlowDelayTriggerService # 延迟触发服务
│       │   └── FlowService       # 服务接口
│       ├── session               # 会话层
│       │   ├── FlowSession       # 执行上下文
│       │   └── FlowAdvice        # 审批参数（意见、签名、退回节点等）
│       ├── strategy              # 策略层（13种策略）
│       │   ├── MultiOperatorAuditStrategy  # 多人审批策略
│       │   ├── TimeoutStrategy          # 超时策略
│       │   ├── SameOperatorAuditStrategy # 相同审批人策略
│       │   ├── RecordMergeStrategy      # 记录合并策略
│       │   ├── ResubmitStrategy         # 重新提交策略
│       │   ├── AdviceStrategy           # 审批意见策略
│       │   ├── OperatorLoadStrategy     # 审批人加载策略
│       │   ├── ErrorTriggerStrategy     # 错误触发策略
│       │   ├── NodeTitleStrategy        # 节点标题策略
│       │   ├── FormFieldPermissionStrategy # 字段权限策略
│       │   ├── DelayStrategy            # 延迟策略
│       │   ├── TriggerStrategy          # 触发策略
│       │   ├── SubProcessStrategy       # 子流程策略
│       │   ├── NodeStrategyFactory      # 策略工厂
│       │   ├── BaseStrategy             # 策略抽象基类
│       │   └── INodeStrategy            # 策略接口
│       ├── utils                 # 工具类
│       │   ├── RandomUtils       # 随机数工具
│       │   └── Sha256Utils       # SHA256加密工具
│       └── workflow              # 流程层
│           ├── Workflow          # 流程对象
│           └── WorkflowBuilder   # 流程构建器
│   └── src/test/java             # 测试代码
├── flow-engine-starter           # Spring Boot 启动器
├── flow-engine-starter-infra     # 持久化层启动器
├── flow-engine-example           # 示例项目
└── frontend                      # 前端工程
    ├── apps
    │   ├── app-pc                # PC 端应用
    │   └── app-mobile            # 移动端应用
    └── packages
        ├── flow-design           # 流程设计器
        ├── flow-pc               # PC 端展示组件
        └── flow-mobile           # 移动端展示组件
```

## 技术栈

### 后端

- **Java 17** - 编程语言
- **Spring Boot 3.5.9** - 应用框架
- **Groovy** - 脚本引擎
- **Lombok** - 简化代码
- **Fastjson** - JSON 处理
- **Apache Commons** - 工具类库

### 前端

- **React** - UI 框架
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

# 构建设计库
pnpm run build:flow-engine

# 启动 PC 应用
pnpm run dev:app-pc
```

## 核心架构

### 八层架构

1. **Workflow Layer** - 流程定义层
2. **Node Layer** - 节点层（12 种节点类型）
3. **Action Layer** - 操作层（9 种操作类型）
4. **Record Layer** - 记录层
5. **Session Layer** - 会话层
6. **Manager Layer** - 管理器层
7. **Strategy Layer** - 策略层
8. **Script Layer** - 脚本层

### 设计模式

- **Builder Pattern** - 构建器模式
- **Factory Pattern** - 工厂模式
- **Strategy Pattern** - 策略模式
- **Template Method** - 模板方法模式
- **Singleton Pattern** - 单例模式
- **Chain of Responsibility** - 责任链模式
- **Composite Pattern** - 组合模式

## 节点类型

| 节点类型 | 说明 | NODE_TYPE |
|---------|------|-----------|
| StartNode | 开始节点 | `start` |
| EndNode | 结束节点 | `end` |
| ApprovalNode | 审批节点 | `approval` |
| HandleNode | 办理节点 | `handle` |
| NotifyNode | 通知节点 | `notify` |
| BranchNodeBranchNode | 条件分支 | `condition_branch` |
| ParallelBranchNode | 并行分支 | `parallel_branch` |
| RouterNode | 路由分支 | `router` |
| InclusiveBranchNode | 包容分支 | `inclusive_branch` |
| SubProcessNode | 子流程节点 | `sub_process` |
| DelayNode | 延迟节点 | `delay` |
| TriggerNode | 触发节点 | `trigger` |

## 操作类型

| 操作类型 | 说明 | ActionType |
|---------|------|------------|
| DefaultAction | 默认操作 | `DEFAULT` |
| PassAction | 通过 | `PASS` |
| RejectAction | 拒绝 | `REJECT` |
| SaveAction | 保存 | `SAVE` |
| ReturnAction | 退回 | `RETURN` |
| TransferAction | 转办 | `TRANSFER` |
| AddAuditAction | 加签 | `ADD_AUDIT` |
| DelegateAction | 委派 | `DELEGATE` |
| CustomAction | 自定义 | `CUSTOM` |

## 多人审批模式

| 模式 | 说明 | 完成条件 |
|-----|------|---------|
| SEQUENCE | 顺序审批 | 所有人按顺序完成 |
| MERGE | 会签 | 达到设定比例的人完成 |
| ANY | 或签 | 任意一人完成 |
| RANDOM_ONE | 随机审批 | 随机选中的一人完成 |

## 文档

- [PRD.md](PRD.md) - 产品需求文档
- [Design.md](Design.md) - 架构设计文档
- [AGENTS.md](AGENTS.md) - 编码规范
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
