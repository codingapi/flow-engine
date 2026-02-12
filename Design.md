# 关键设计介绍

## 一、核心类设计

### 1. 流程定义层 (Workflow Layer)

#### Workflow
- **位置**: `com.codingapi.flow.workflow.Workflow`
- **职责**: 流程定义的顶层容器，包含流程的完整定义信息
- **核心属性**:
    - `id`: 流程唯一标识
    - `code`: 流程编号
    - `title`: 流程名称
    - `form`: 流程表单定义 (`FormMeta`)
    - `nodes`: 节点列表 (`List<IFlowNode>`)
    - `operatorCreateScript`: 创建者匹配脚本
    - `strategies`: 流程策略列表 (`List<IWorkflowStrategy>`)
    - `createdOperator`: 创建者ID
    - `createdTime`: 创建时间
    - `updatedTime`: 更新时间
    - `schema`: 流程版本标识
- **核心方法**:
    - `verify()`: 验证流程定义的合法性
    - `nextNodes(IFlowNode)`: 获取指定节点的后续节点（通过FlowNodeEdgeManager遍历blocks）
    - `getStartNode()`: 获取开始节点
    - `getEndNode()`: 获取结束节点
    - `toJson()`: 序列化为JSON
    - `formJson(String)`: 从JSON反序列化（静态方法）
- **数据结构说明**:
    - 节点关系通过`blocks`属性实现层次化结构，不再使用单独的`edges`边关系
    - 流程策略（InterfereStrategy、UrgeStrategy）在流程级别配置

#### WorkflowBuilder
- **位置**: `com.codingapi.flow.workflow.WorkflowBuilder`
- **职责**: 使用Builder模式构建Workflow对象
- **设计模式**: Builder模式
- **用法**: `WorkflowBuilder.builder().title("请假流程").node(startNode).build()`

---

### 2. 节点层 (Node Layer)

#### IFlowNode
- **位置**: `com.codingapi.flow.node.IFlowNode`
- **职责**: 所有流程节点的顶层接口，定义节点生命周期方法
- **核心方法**（按执行顺序）:
    1. `verifySession(FlowSession)`: 验证会话参数（委托给ActionManager和StrategyManager）
    2. `continueTrigger(FlowSession)`: 判断是否继续执行后续节点
    3. `generateCurrentRecords(FlowSession)`: 生成当前节点的流程记录
    4. `isDone(FlowSession)`: 判断节点是否完成
    5. `fillNewRecord(FlowSession, FlowRecord)`: 填充新记录数据
    6. `blocks()`: 获取当前节点的子节点列表（块节点有子节点，其他节点返回null或空）
    7. `filterBranches()`: 过滤条件分支（块节点用于筛选执行的分支）
    8. `actionManager()`: 获取动作管理器
    9. `nodeStrategyManager()`: 获取策略管理器

#### BaseFlowNode
- **位置**: `com.codingapi.flow.node.BaseFlowNode`
- **职责**: 所有节点的抽象基类，实现IFlowNode的默认行为
- **核心属性**: `id`, `name`, `order`, `actions`, `strategies`, `blocks`（子节点列表）
- **核心方法**:
    - `isWaitParallelRecord()`: 判断是否等待并行节点汇聚
    - `verifySession()`: 委托给ActionManager和StrategyManager验证
    - `setActions()`: 支持动作的复制更新（CustomAction除外）
    - `setStrategies()`: 支持策略的复制更新
    - `blocks()`: 返回子节点列表
    - `toMap()`: 序列化为Map（包含blocks字段）
    - `fromMap()`: 从Map反序列化（从blocks字段加载子节点）

#### BaseAuditNode
- **位置**: `com.codingapi.flow.node.BaseAuditNode`
- **职责**: 审批类节点的抽象基类（ApprovalNode、HandleNode）
- **核心属性**:
    - `view`: 渲染视图
    - 继承自BaseFlowNode的actions和strategies
- **核心方法**:
    - `continueTrigger()`: 返回false（审批节点需要生成记录）
    - `fillNewRecord()`: 通过StrategyManager填充记录
    - `isDone()`: 通过StrategyManager判断多人审批完成状态
    - `generateCurrentRecords()`: 通过StrategyManager加载操作者并生成记录

#### 块节点系统 (Block Nodes System)

**数据结构变更说明**:
- 旧设计: 使用独立的`FlowEdge`对象和`edges`列表表示节点连接关系
- 新设计: 使用层次化的`blocks`属性，节点包含子节点形成树形结构

**FlowNodeState** - 节点状态分类
- **位置**: `com.codingapi.flow.manager.FlowNodeState`
- **职责**: 对节点进行分类，判断节点类型
- **节点分类**:
  - **块节点 (Block Nodes)**: `CONDITION`, `INCLUSIVE`, `PARALLEL` - 包含子节点的容器节点
  - **分支节点 (Branch Nodes)**: `CONDITION_BRANCH`, `INCLUSIVE_BRANCH`, `PARALLEL_BRANCH` - 作为块节点的子节点
- **核心方法**:
  - `isBlockNode()`: 判断是否为块节点（有blocks属性）
  - `isBranchNode()`: 判断是否为分支节点
  - `getBlocks()`: 获取节点的子节点列表
  - `getFirstBlocks()`: 获取第一个子节点（用于分支节点）

**FlowNodeEdgeManager** - 节点关系管理器
- **位置**: `com.codingapi.flow.manager.FlowNodeEdgeManager`
- **职责**: 管理节点之间的连接关系，通过遍历blocks实现
- **核心方法**:
  - `getNextNodes(IFlowNode)`: 获取指定节点的后续节点
  - `loadNextNodes()`: 递归加载下一节点（遍历blocks）
- **遍历逻辑**:
  1. 如果当前节点是块节点，返回其blocks
  2. 如果当前节点是分支节点，返回其第一个block
  3. 否则返回节点列表中的下一个节点
  4. 递归处理嵌套的blocks

**NodeMapBuilder** - 节点映射构建器
- **位置**: `com.codingapi.flow.builder.NodeMapBuilder`
- **职责**: 从Map数据构建节点对象
- **核心方法**:
  - `loadNodes(Map)`: 从Map的"blocks"键加载子节点列表
  - `loadActions(Map)`: 加载节点动作
  - `loadNodeStrategies(Map)`: 加载节点策略

**节点关系示例**:
```
Workflow
├── StartNode
├── ApprovalNode
├── ConditionNode (块节点)
│   ├── blocks:
│   │   ├── ConditionBranchNode (order=1)
│   │   │   └── blocks: [NotifyNode, EndNode]
│   │   └── ConditionBranchNode (order=2)
│   │       └── blocks: [HandleNode, EndNode]
└── EndNode
```

#### 节点类型一览 (15种)

**基础节点 (9种)**:

| 节点类型 | 类名 | NODE_TYPE | 说明 |
|---------|------|-----------|------|
| 开始节点 | StartNode | `START` | 流程起点 |
| 结束节点 | EndNode | `END` | 流程终点 |
| 审批节点 | ApprovalNode | `APPROVAL` | 需要审批的任务节点 |
| 办理节点 | HandleNode | `HANDLE` | 需要办理的任务节点 |
| 路由分支 | RouterNode | `ROUTER` | 普通路由节点 |
| 通知节点 | NotifyNode | `NOTIFY` | 发送通知 |
| 延迟节点 | DelayNode | `DELAY` | 延迟执行 |
| 触发节点 | TriggerNode | `TRIGGER` | 事件触发 |
| 子流程节点 | SubProcessNode | `SUB_PROCESS` | 嵌套子流程 |

**块节点/容器节点 (3种)** - 包含子节点(blocks):

| 节点类型 | 类名 | NODE_TYPE | 说明 |
|---------|------|-----------|------|
| 条件控制 | ConditionNode | `CONDITION` | 条件分支容器，包含多个ConditionBranchNode |
| 并行控制 | ParallelNode | `PARALLEL` | 并行分支容器，包含多个ParallelBranchNode |
| 包容控制 | InclusiveNode | `INCLUSIVE` | 包容分支容器，包含多个InclusiveBranchNode |

**分支节点 (3种)** - 作为块节点的子节点:

| 节点类型 | 类名 | NODE_TYPE | 说明 |
|---------|------|-----------|------|
| 条件分支 | ConditionBranchNode | `CONDITION_BRANCH` | 条件控制下的分支，包含条件脚本 |
| 并行分支 | ParallelBranchNode | `PARALLEL_BRANCH` | 并行控制下的分支 |
| 包容分支 | InclusiveBranchNode | `INCLUSIVE_BRANCH` | 包容控制下的分支 |

---

### 3. 动作层 (Action Layer)

#### IFlowAction
- **位置**: `com.codingapi.flow.action.IFlowAction`
- **职责**: 节点上可执行的动作接口
- **核心方法**:
    - `type()`: 动作类型
    - `run(FlowSession)`: 执行动作的主入口
    - `generateRecords()`: 生成流程记录
    - `copy(IFlowAction)`: 复制动作属性

#### BaseAction
- **位置**: `com.codingapi.flow.action.BaseAction`
- **职责**: 动作的抽象基类
- **核心方法**:
    - `triggerNode()`: 递归触发后续节点
    - `equals()`: 基于id判断相等

#### 动作类型 (ActionType枚举)

| 动作类 | ActionType | 说明 |
|-------|------------|------|
| SaveAction | `SAVE` | 保存 |
| PassAction | `PASS` | 通过，流程继续往下流转 |
| RejectAction | `REJECT` | 拒绝，根据配置退回上级/指定节点或终止流程 |
| AddAuditAction | `ADD_AUDIT` | 加签，指定其他人一块审批（会签模式） |
| DelegateAction | `DELEGATE` | 委派，委派给其他人员审批，完成后流程返回自己 |
| ReturnAction | `RETURN` | 退回，需要设置退回的节点 |
| TransferAction | `TRANSFER` | 转办，将流程转移给指定用户审批 |
| CustomAction | `CUSTOM` | 自定义，需要配置脚本 |

---

### 4. 流程记录层 (Record Layer)

#### FlowRecord
- **位置**: `com.codingapi.flow.record.FlowRecord`
- **职责**: 流程执行过程中的每一条记录
- **状态定义**:
    - `SATE_RECORD_TODO = 0`: 待办
    - `SATE_RECORD_DONE = 1`: 已办
    - `SATE_FLOW_RUNNING = 0`: 运行中
    - `SATE_FLOW_DONE = 1`: 已完成
    - `SATE_FLOW_FINISH = 2`: 终止
    - `SATE_FLOW_ERROR = 3`: 异常
    - `SATE_FLOW_DELETE = 4`: 删除
- **核心属性**:
    - `processId`: 流程实例ID（每次启动生成）
    - `nodeId`: 当前节点ID
    - `currentOperatorId`: 当前审批人ID
    - `formData`: 表单数据
    - `parallelId`: 并行分支ID
    - `parallelBranchNodeId`: 并行分支节点ID
    - `parallelBranchTotal`: 并行分支总数

---

### 5. 会话层 (Session Layer)

#### FlowSession
- **位置**: `com.codingapi.flow.session.FlowSession`
- **职责**: 流程执行的上下文会话对象
- **核心属性**:
    - `currentOperator`: 当前操作者
    - `workflow`: 流程定义
    - `currentNode`: 当前节点
    - `currentAction`: 当前动作
    - `currentRecord`: 当前记录
    - `formData`: 表单数据
    - `advice`: 审批意见
- **核心方法**:
    - `matchNextNodes()`: 匹配后续节点
    - `updateSession()`: 更新会话

#### FlowAdvice
- **位置**: `com.codingapi.flow.session.FlowAdvice`
- **职责**: 封装审批操作的相关参数
- **核心属性**:
    - `advice`: 审批意见
    - `signKey`: 签名
    - `action`: 动作类型
    - `backNode`: 退回节点（类型为 `IFlowNode`，节点对象引用）
    - `transferOperators`: 转办人员列表

---

### 6. 管理器层 (Manager Layer)

#### ActionManager
- **位置**: `com.codingapi.flow.node.manager.ActionManager`
- **职责**: 管理节点的动作列表
- **核心方法**:
    - `getActionById(String)`: 根据id获取动作
    - `getAction(Class)`: 根据类型获取动作
    - `verifySession()`: 验证会话参数
    - `verify()`: 验证动作配置

#### OperatorManager
- **位置**: `com.codingapi.flow.node.manager.OperatorManager`
- **职责**: 管理节点的操作者列表
- **核心方法**:
    - `match(IFlowOperator)`: 判断操作者是否匹配

#### StrategyManager
- **位置**: `com.codingapi.flow.node.manager.StrategyManager`
- **职责**: 管理节点策略（多人审批、超时、退回等）
- **核心方法**:
    - `getTimeoutTime()`: 获取超时时间
    - `isMergeable()`: 是否可合并
    - `isEnableAdvice()`: 审批意见是否必须填写
    - `isEnableSignable()`: 是否可签名
    - `isResume()`: 是否恢复到退回节点
    - `getMultiOperatorAuditStrategyType()`: 获取多人审批类型
    - `getMultiOperatorAuditMergePercent()`: 获取并签比例
    - `generateTitle()`: 生成节点标题
    - `loadOperators()`: 加载操作者
    - `verifySession()`: 验证会话参数
    - `getStrategy(Class)`: 获取指定策略

---

### 7. 策略层 (Strategy Layer)

#### INodeStrategy
- **位置**: `com.codingapi.flow.strategy.node.INodeStrategy`
- **职责**: 节点策略的顶层接口
- **核心方法**:
    - `toMap()`: 转换为Map
    - `getId()`: 获取策略ID
    - `strategyType()`: 获取策略类型
    - `copy(INodeStrategy)`: 复制策略属性

#### BaseStrategy
- **位置**: `com.codingapi.flow.strategy.node.BaseStrategy`
- **职责**: 策略的抽象基类
- **核心方法**:
    - `equals()`: 基于id判断相等
    - `fromMap()`: 从Map构建策略对象

#### 策略类型

**节点策略 (16种)**:

| 策略类 | 说明 |
|-------|------|
| MultiOperatorAuditStrategy | 多人审批策略（SEQUENCE顺序/MERGE并签/ANY或签/RANDOM_ONE随机） |
| TimeoutStrategy | 超时策略 |
| SameOperatorAuditStrategy | 同一操作者审批策略 |
| RecordMergeStrategy | 记录合并策略 |
| ResubmitStrategy | 重新提交策略（是否恢复到退回节点） |
| AdviceStrategy | 审批意见策略（是否必须填写、是否可签名） |
| OperatorLoadStrategy | 操作者加载策略 |
| ErrorTriggerStrategy | 异常触发策略 |
| NodeTitleStrategy | 节点标题策略 |
| FormFieldPermissionStrategy | 表单字段权限策略 |
| DelayStrategy | 延迟策略 |
| TriggerStrategy | 触发策略 |
| RouterStrategy | 路由策略 |
| SubProcessStrategy | 子流程策略 |
| RevokeStrategy | 撤回策略（REVOKE_NEXT撤回下级/REVOKE_CURRENT撤回到当前节点） |

**工作流策略 (2种)**:

| 策略类 | 说明 |
|-------|------|
| InterfereStrategy | 干预策略，用于控制流程是否允许管理员干预 |
| UrgeStrategy | 催办策略，支持配置催办间隔时间（默认60秒），通过UrgeInterval判断是否需要催办 |

---

### 8. 脚本层 (Script Layer)

#### ScriptRuntimeContext
- **位置**: `com.codingapi.flow.script.runtime.ScriptRuntimeContext`
- **职责**: Groovy脚本运行时环境，支持线程安全的脚本执行和自动资源清理
- **设计特点**:
  - **线程安全**: 使用脚本哈希值进行细粒度同步，不同脚本可并发执行
  - **自动清理**: 双重清理机制（阈值触发 + 定时清理）
  - **资源管理**: 单例自动管理定时任务生命周期，注册JVM关闭钩子
- **核心属性**:
  - `SCRIPT_LOCKS`: 脚本锁映射表（ConcurrentHashMap），键为脚本哈希值
  - `EXECUTION_COUNTER`: 脚本执行计数器（AtomicInteger）
  - `maxLockCacheSize`: 最大锁缓存阈值（默认1000）
  - `CLEANUP_INTERVAL_SECONDS`: 定时清理间隔（默认300秒，可通过系统属性`flow.script.cleanup.interval`覆盖）
- **核心方法**:
  - `run(String script, Class<T> returnType, Object... args)`: 执行脚本
  - `execute(String method, String script, Class<T> returnType, Object... args)`: 执行指定方法
  - `clearLockCache()`: 手动清理锁缓存
  - `setMaxLockCacheSize(int)`: 设置最大锁缓存阈值
  - `getLockCacheSize()`: 获取当前锁缓存大小
  - `getExecutionCount()`: 获取脚本执行总次数
  - `enableAutoCleanup()`: 启用自动清理
  - `disableAutoCleanup()`: 禁用自动清理
  - `getCleanupIntervalSeconds()`: 获取清理间隔

#### 自动清理机制

```
┌─────────────────────────────────────────────────────────────────┐
│                    ScriptRuntimeContext                         │
│                    自动清理双重机制                              │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐       ┌─────────────────────────┐
│   阈值触发清理           │       │   定时清理任务           │
│   (Threshold-based)      │       │   (Scheduled)           │
└─────────────────────────┘       └─────────────────────────┘
              │                               │
              ▼                               ▼
┌─────────────────────────┐       ┌─────────────────────────┐
│ 触发条件：               │       │ 触发条件：               │
│ SCRIPT_LOCKS.size() >    │       │ 每隔 CLEANUP_INTERVAL_  │
│ maxLockCacheSize         │       │ SECONDS 秒执行一次       │
└─────────────────────────┘       └─────────────────────────┘
              │                               │
              ▼                               ▼
┌─────────────────────────┐       ┌─────────────────────────┐
│ 清理动作：               │       │ 清理动作：               │
│ SCRIPT_LOCKS.clear()    │       │ SCRIPT_LOCKS.clear()    │
│ EXECUTION_COUNTER.set(0)│       │ EXECUTION_COUNTER.set(0)│
└─────────────────────────┘       └─────────────────────────┘
```

**配置方式**:
- **阈值配置**: `ScriptRuntimeContext.setMaxLockCacheSize(500)`
- **定时配置**: JVM启动参数 `-Dflow.script.cleanup.interval=300`
- **动态控制**: `enableAutoCleanup()` / `disableAutoCleanup()`

**生命周期管理**:
1. 单例初始化时自动启动定时清理任务（守护线程）
2. 注册JVM关闭钩子确保资源释放
3. 支持运行时动态启用/禁用自动清理

#### 脚本类型

| 脚本类 | 说明 |
|-------|------|
| OperatorMatchScript | 操作者匹配脚本 |
| OperatorLoadScript | 操作者加载脚本 |
| NodeTitleScript | 节点标题脚本 |
| ConditionScript | 条件判断脚本 |
| RouterNodeScript | 路由节点脚本 |
| SubProcessScript | 子流程脚本 |
| TriggerScript | 触发器脚本 |
| ErrorTriggerScript | 异常触发脚本 |
| RejectActionScript | 拒绝动作脚本 |
| CustomScript | 自定义动作脚本 |

---

## 二、流程生命周期

### 1. 流程创建阶段 (FlowCreateService)

```
┌─────────────────────────────────────────────────────────────────┐
│                         FlowCreateService                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 1. 验证请求参数   │
                    │    request.verify() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 2. 获取流程定义   │
                    │  workflowRepository.get() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 3. 验证流程定义   │
                    │   workflow.verify() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 4. 创建/获取备份  │
                    │   WorkflowBackup │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 5. 验证创建者权限 │
                    │ matchCreatedOperator() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 6. 构建表单数据   │
                    │   new FormData() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 7. 创建开始会话   │
                    │ FlowSession.startSession() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 8. 验证会话参数   │
                    │ verifySession()  │
                    │ (委托给ActionManager和StrategyManager) │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 9. 生成流程记录   │
                    │ generateCurrentRecords() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 10. 保存记录      │
                    │ flowRecordRepository.saveAll() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 11. 推送事件      │
                    │ FlowRecordStartEvent │
                    │ FlowRecordTodoEvent │
                    └─────────────────┘
```

---

### 2. 流程执行阶段 (FlowActionService)

```
┌─────────────────────────────────────────────────────────────────┐
│                         FlowActionService                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 1. 验证请求参数   │
                    │   request.verify() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 2. 验证操作者     │
                    │ flowOperatorGateway.get() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 3. 获取流程记录   │
                    │ flowRecordRepository.get() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 4. 验证记录状态   │
                    │   isTodo()       │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 5. 加载流程定义   │
                    │ workflowBackup.toWorkflow() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 6. 获取当前节点   │
                    │ workflow.getFlowNode() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 7. 获取动作对象   │
                    │ actionManager.getActionById() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 8. 构建表单数据   │
                    │   new FormData() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 9. 创建执行会话   │
                    │   new FlowSession() │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 10. 验证会话参数  │
                    │ verifySession()  │
                    │ (委托给ActionManager和StrategyManager) │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ 11. 执行动作      │
                    │ flowAction.run(session) │
                    └─────────────────┘
```

---

### 3. 节点生命周期 (Node Lifecycle)

```
                    ┌─────────────────────────────────────────┐
                    │            节点生命周期                   │
                    └─────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         1. verifySession(session)         │
        │         验证会话参数是否满足节点要求        │
        │         委托给ActionManager和StrategyManager │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         2. continueTrigger(session)        │
        │         判断是否继续执行后续节点            │
        │         true: 继续执行下一节点              │
        │         false: 执行步骤3                   │
        └───────────────────────────────────────────┘
                         │                │
                    true │                │ false
                         ▼                 ▼
        ┌─────────────────────┐   ┌─────────────────────────┐
        │   递归执行下一节点    │   │ 3. generateCurrentRecords() │
        │                      │   │ 生成当前节点的流程记录     │
        └─────────────────────┘   └─────────────────────────┘
                                          │
                                          ▼
        ┌───────────────────────────────────────────┐
        │         4. fillNewRecord(session, record) │
        │         填充新记录的数据                    │
        │         通过StrategyManager填充            │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         5. isDone(session)                │
        │         判断节点是否完成                    │
        │         通过StrategyManager判断多人审批进度 │
        └───────────────────────────────────────────┘
                         │                │
                     done │            │ not done
                         ▼                 ▼
        ┌─────────────────────┐   ┌─────────────────────┐
        │   继续执行下一节点    │   │  节点未完成，等待    │
        │                      │   │  下一次操作         │
        └─────────────────────┘   └─────────────────────┘
```

---

### 4. 节点执行流程 (以PassAction为例)

```
                    ┌─────────────────────────────────────────┐
                    │            PassAction.run()             │
                    └─────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         1. 判断节点是否完成                 │
        │         currentNode.isDone(session)        │
        │         通过StrategyManager判断            │
        └───────────────────────────────────────────┘
                         │                │
                     done │            │ not done
                         ▼                 ▼
        ┌─────────────────────┐   ┌─────────────────────┐
        │ 更新当前记录为已办    │   │  更新当前记录，      │
        │ record.update(...)  │   │  保持待办状态        │
        └─────────────────────┘   └─────────────────────┘
                         │
                         ▼
        ┌───────────────────────────────────────────┐
        │         2. 生成后续记录                    │
        │         generateRecords(session)           │
        │         判断是否为退回重新提交              │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         3. 触发后续节点                    │
        │         triggerNode(session, callback)     │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         4. 获取下一节点列表                 │
        │         session.matchNextNodes()           │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         5. 遍历下一节点                     │
        │         for (IFlowNode node : nextNodes)  │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         6. 更新会话到下一节点               │
        │         session.updateSession(node)        │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         7. continueTrigger()              │
        │         判断是否继续执行                    │
        └───────────────────────────────────────────┘
                         │                │
                     true │                │ false
                         ▼                 ▼
        ┌─────────────────────┐   ┌─────────────────────┐
        │  递归执行下一节点     │   │  生成当前节点记录    │
        │  triggerNode()      │   │  callback()        │
        └─────────────────────┘   └─────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         8. 保存所有记录                    │
        │         RepositoryContext.saveRecords()   │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         9. 推送事件                        │
        │         FlowRecordDoneEvent               │
        │         FlowRecordTodoEvent               │
        └───────────────────────────────────────────┘
```

---

### 5. 并行分支执行流程

```
                    ┌─────────────────────────────────────────┐
                    │         遇到ParallelBranchNode          │
                    └─────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         1. filterBranches()               │
        │         分析并行分支的结束汇聚节点          │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         2. 记录并行信息                    │
        │         flowRecord.parallelBranchNode()    │
        │         - parallelBranchNodeId: 汇聚节点ID │
        │         - parallelBranchTotal: 分支总数    │
        │         - parallelId: 并行实例ID           │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         3. 同时执行所有并行分支             │
        │         为每个分支生成流程记录              │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         4. 分支执行中...                   │
        │         每个分支独立执行                   │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         5. 到达汇聚节点                    │
        │         isWaitParallelRecord()            │
        └───────────────────────────────────────────┘
                         │                │
                     等待 │              │ 全部到达
                         ▼                 ▼
        ┌─────────────────────┐   ┌─────────────────────┐
        │  增加触发计数        │   │  清空并行信息       │
        │  等待其他分支完成    │   │  继续执行后续流程   │
        └─────────────────────┘   └─────────────────────┘
```

---

### 6. 催办流程 (Urge Flow)

```
                    ┌─────────────────────────────────────────┐
                    │         催办检查流程                       │
                    └─────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         1. 获取催办策略                     │
        │         UrgeStrategy                       │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         2. 检查是否启用催办                 │
        │         urgeStrategy.enable                │
        └───────────────────────────────────────────┘
                         │                │
                     启用 │              │ 未启用
                         ▼                 ▼
        ┌─────────────────────┐   ┌─────────────────────┐
        │  检查催办间隔         │   │  不执行催办         │
        └─────────────────────┘   └─────────────────────┘
                         │
                         ▼
        ┌───────────────────────────────────────────┐
        │         3. 检查是否需要催办                 │
        │         urgeStrategy.hasUrge(urgeInterval) │
        └───────────────────────────────────────────┘
                         │                │
                     需要催办 │          │ 不需要
                         ▼                 ▼
        ┌─────────────────────┐   ┌─────────────────────┐
        │  推送催办事件         │   │  等待下次检查       │
        │  FlowRecordUrgeEvent │   │                     │
        └─────────────────────┘   └─────────────────────┘
```

**UrgeInterval**: 记录催办时间间隔
- `createTime`: 创建时间
- `processId`: 流程实例ID
- `nodeId`: 节点ID

**催办策略配置**:
- `enable`: 是否启用催办
- `interval`: 催办间隔秒数（默认60秒）

---

### 7. 撤回流程 (Revoke Flow)

```
                    ┌─────────────────────────────────────────┐
                    │         撤回操作流程                       │
                    └─────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         1. 获取撤回策略                     │
        │         RevokeStrategy                     │
        └───────────────────────────────────────────┘
                                    │
                                    ▼
        ┌───────────────────────────────────────────┐
        │         2. 检查撤回类型                     │
        │         revokeStrategy.type                │
        └───────────────────────────────────────────┘
                         │                            │
         REVOKE_NEXT     │                            │ REVOKE_CURRENT
                         ▼                             ▼
        ┌─────────────────────────┐   ┌─────────────────────────┐
        │  撤回下级审批             │   │  撤回到当前节点           │
        │  删除后续待办记录         │   │  恢复当前节点为待办       │
        │  流程退回到当前节点       │   │  清除已完成状态           │
        └─────────────────────────┘   └─────────────────────────┘
```

**撤回策略类型**:
- `REVOKE_NEXT`: 撤回下级审批，删除后续待办记录
- `REVOKE_CURRENT`: 撤回到当前节点，恢复当前节点为待办状态

---

## 三、核心设计模式

### 1. 建造者模式 (Builder Pattern)
- `WorkflowBuilder`: 构建流程定义
- `BaseNodeBuilder`: 构建节点对象
- `ActionBuilder`: 构建动作对象
- `NodeStrategyBuilder`: 构建策略对象

### 2. 工厂模式 (Factory Pattern)
- `NodeFactory`: 创建不同类型的节点（通过反射调用静态formMap方法）
- `NodeStrategyFactory`: 创建不同类型的策略
- `FlowActionFactory`: 创建不同类型的动作

### 3. 策略模式 (Strategy Pattern)
- `INodeStrategy`: 节点策略接口
- `StrategyManager`: 管理多种策略，通过策略类型执行不同逻辑
- `MultiOperatorAuditStrategy`: 多人审批策略（SEQUENCE/MERGE/ANY/RANDOM_ONE）
- `TimeoutStrategy`: 超时策略
- `AdviceStrategy`: 审批意见策略

### 4. 模板方法模式 (Template Method Pattern)
- `BaseFlowNode`: 定义节点生命周期模板，子类实现特定行为
- `BaseAction`: 定义动作执行模板，子类实现具体动作逻辑
- `BaseStrategy`: 定义策略模板，子类实现具体策略逻辑

### 5. 单例模式 (Singleton Pattern)
- `NodeFactory.getInstance()`: 节点工厂单例
- `ScriptRuntimeContext.getInstance()`: 脚本运行时单例
- `RepositoryContext.getInstance()`: 仓储上下文单例
- `GatewayContext.getInstance()`: 网关上下文单例

### 6. 责任链模式 (Chain of Responsibility Pattern)
- `triggerNode()`: 递归触发后续节点，形成责任链
- `StrategyManager`: 遍历策略列表查找匹配策略

### 7. 组合模式 (Composite Pattern)
- 节点与策略的组合：每个节点包含多个策略
- 节点与动作的组合：每个节点包含多个动作

### 8. 复制模式 (Copy Pattern)
- `INodeStrategy.copy()`: 策略属性复制
- `IFlowAction.copy()`: 动作属性复制
- `BaseFlowNode.setActions()`: 动作复制更新
- `BaseFlowNode.setStrategies()`: 策略复制更新

---

## 四、扩展点

### 1. 自定义节点
继承 `BaseFlowNode` 或 `BaseAuditNode`，实现 `IFlowNode` 接口

### 2. 自定义动作
继承 `BaseAction`，实现 `IFlowAction` 接口

### 3. 自定义策略
继承 `BaseStrategy`，实现 `INodeStrategy` 接口

### 4. 自定义脚本
使用 `ScriptRuntimeContext` 执行 Groovy 脚本

### 5. 事件扩展
监听 `FlowRecordStartEvent`, `FlowRecordTodoEvent`, `FlowRecordDoneEvent`, `FlowRecordFinishEvent`, `FlowRecordUrgeEvent`

---

## 五、关键实现细节

### 1. 策略驱动的节点配置
- 所有节点配置（操作者、标题、超时、权限等）都通过策略实现
- `StrategyManager`统一管理所有策略的访问和执行
- 策略支持复制更新，便于动态修改节点配置

### 2. 多人审批实现
- 通过`MultiOperatorAuditStrategy`的type属性控制审批模式
- SEQUENCE: 顺序审批，隐藏后续记录
- MERGE: 并签，根据完成比例判断
- ANY: 或签，任意一人完成即通过
- RANDOM_ONE: 随机一人审批

### 3. 并行分支同步机制
- 通过`parallelId`标识同一并行流程
- 通过`parallelBranchNodeId`标记汇聚节点
- 通过`parallelBranchTotal`记录分支总数
- `RepositoryContext`维护并行触发计数
- 汇聚节点检查所有分支是否到达后继续执行

### 4. 动作和策略的复制更新机制
- 节点的`setActions()`和`setStrategies()`方法支持增量更新
- 根据类型匹配现有对象，调用`copy()`方法复制属性
- `CustomAction`特殊处理，支持动态添加

### 5. 脚本运行时线程安全设计
- **问题**: GroovyShell和GroovyClassLoader都不是线程安全的
- **方案**: 每次执行创建独立的GroovyClassLoader和GroovyShell实例
- **细粒度同步**: 使用脚本哈希值作为锁键，相同脚本串行执行，不同脚本并发执行
- **自动清理**: 阈值触发 + 定时清理双重机制，避免内存泄漏

### 6. 框架异常体系

**异常编码规则**:
所有框架异常使用字符串形式的错误码，格式为 `category.subcategory.errorType`

**异常类型**:
- `FlowException`: 所有框架异常的基类（RuntimeException）
- `FlowValidationException`: 参数验证异常（`validation.field.required`、`validation.field.notEmpty`）
- `FlowNotFoundException`: 资源未找到异常（`notFound.workflow.definition`、`notFound.record.id`）
- `FlowStateException`: 状态异常（`state.record.alreadyDone`、`state.operator.notMatch`）
- `FlowPermissionException`: 权限异常（`permission.field.readOnly`、`permission.access.denied`）
- `FlowConfigException`: 配置异常（`config.node.strategies.required`、`config.edge.error`）
- `FlowExecutionException`: 执行异常（`execution.script.error`、`execution.node.error`）

**异常使用规范**:
- 必须使用异常类的静态工厂方法创建异常，不直接使用 `new` 构造
- 不使用 `ERROR_CODE_PREFIX` 等常量前缀
- 所有异常信息使用英文

### 7. 流程催办机制

- **UrgeStrategy**: 工作流级别的催办策略
- **催办间隔**: 默认60秒，可通过 `UrgeStrategy.interval` 配置
- **UrgeInterval**: 记录催办时间间隔的领域对象
  - `createTime`: 创建时间
  - `processId`: 流程实例ID
  - `nodeId`: 节点ID
- **催办判断**: `UrgeStrategy.hasUrge(urgeInterval)` 比较当前时间与创建时间的差值是否超过配置间隔
- **催办事件**: 触发 `FlowRecordUrgeEvent` 通知相关系统

### 8. 流程干预机制

- **InterfereStrategy**: 工作流级别的干预策略
- **干预控制**: 通过 `enable` 属性控制是否允许管理员干预流程
- **应用场景**: 管理员强制干预、异常流程修正、跳节点处理

### 9. 节点撤回机制

- **RevokeStrategy**: 节点级别的撤回策略
- **撤回类型**:
  - `REVOKE_NEXT`: 撤回下级审批，删除后续待办记录，流程退回到当前节点
  - `REVOKE_CURRENT`: 撤回到当前节点，恢复当前节点为待办状态
- **撤回权限**: 通过 `enable` 属性控制节点是否允许撤回
- **与退回的区别**:
  - 退回 (ReturnAction): 由当前审批人执行，退回到之前已完成的节点
  - 撤回 (RevokeStrategy): 由已审批的人员执行，撤回下级或恢复当前节点
