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
  - `edges`: 节点连接关系 (`List<FlowEdge>`)
  - `operatorCreateScript`: 创建者匹配脚本
  - `isInterfere`: 是否开启干预
  - `isRevoke`: 是否开启撤销
- **核心方法**:
  - `verify()`: 验证流程定义的合法性
  - `nextNodes(IFlowNode)`: 获取指定节点的后续节点
  - `getStartNode()`: 获取开始节点
  - `getEndNode()`: 获取结束节点
  - `toJson()`: 序列化为JSON
  - `formJson()`: 从JSON反序列化

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
  1. `verifySession(FlowSession)`: 验证会话参数
  2. `continueTrigger(FlowSession)`: 判断是否继续执行后续节点
  3. `generateCurrentRecords(FlowSession)`: 生成当前节点的流程记录
  4. `isDone(FlowSession)`: 判断节点是否完成
  5. `fillNewRecord(FlowSession, FlowRecord)`: 填充新记录数据
  6. `filterBranches()`: 过滤条件分支
  7. `actionManager()`: 获取动作管理器

#### BaseFlowNode
- **位置**: `com.codingapi.flow.node.BaseFlowNode`
- **职责**: 所有节点的抽象基类，实现IFlowNode的默认行为
- **核心属性**: `id`, `name`, `order`, `actions`
- **核心方法**:
  - `isWaitParallelRecord()`: 判断是否等待并行节点汇聚

#### BaseAuditNode
- **位置**: `com.codingapi.flow.node.BaseAuditNode`
- **职责**: 审批类节点的抽象基类（ApprovalNode、HandleNode）
- **核心属性**:
  - `operatorScript`: 审批人加载脚本
  - `nodeTitleScript`: 节点标题脚本
  - `errorTriggerScript`: 异常触发脚本
  - `formFieldPermissions`: 表单字段权限
  - `nodeStrategies`: 节点策略列表
- **核心方法**:
  - `operators()`: 获取操作者管理器
  - `strategies()`: 获取策略管理器
  - `generateTitle()`: 生成节点标题

#### 节点类型一览 (11种)

| 节点类型 | 类名 | NODE_TYPE | 说明 |
|---------|------|-----------|------|
| 开始节点 | StartNode | `start` | 流程起点 |
| 结束节点 | EndNode | `end` | 流程终点 |
| 审批节点 | ApprovalNode | `approval` | 需要审批的任务节点 |
| 办理节点 | HandleNode | `handle` | 需要办理的任务节点 |
| 条件分支 | BranchNodeBranchNode | `condition_branch` | 按条件路由 |
| 并行分支 | ParallelBranchNode | `parallel_branch` | 并行执行多个分支 |
| 路由分支 | RouterBranchNode | `router_branch` | 普通路由节点 |
| 包容分支 | InclusiveBranchNode | `inclusive_branch` | 包容性分支 |
| 通知节点 | NotifyNode | `notify` | 发送通知 |
| 延迟节点 | DelayNode | `delay` | 延迟执行 |
| 触发节点 | TriggerNode | `trigger` | 事件触发 |
| 子流程节点 | SubProcessNode | `sub_process` | 嵌套子流程 |

---

### 3. 动作层 (Action Layer)

#### IFlowAction
- **位置**: `com.codingapi.flow.action.IFlowAction`
- **职责**: 节点上可执行的动作接口
- **核心方法**:
  - `type()`: 动作类型
  - `run(FlowSession)`: 执行动作的主入口
  - `generateRecords()`: 生成流程记录

#### BaseAction
- **位置**: `com.codingapi.flow.action.BaseAction`
- **职责**: 动作的抽象基类
- **核心方法**:
  - `triggerNode()`: 递归触发后续节点

#### 动作类型

| 动作类 | ActionType | 说明 |
|-------|------------|------|
| PassAction | `PASS` | 通过 |
| RejectAction | `REJECT` | 驳回 |
| SaveAction | `SAVE` | 保存 |
| ReturnAction | `RETURN` | 退回 |
| TransferAction | `TRANSFER` | 转办 |
| AddAuditAction | `ADD_AUDIT` | 加签 |
| DelegateAction | `DELEGATE` | 委托 |
| CustomAction | `CUSTOM` | 自定义 |

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
- **核心属性**: `advice`(审批意见), `signKey`(签名), `action`(动作), `backNode`(退回节点), `transferOperators`(转办人员)

---

### 6. 管理器层 (Manager Layer)

#### ActionManager
- **位置**: `com.codingapi.flow.node.manager.ActionManager`
- **职责**: 管理节点的动作列表
- **核心方法**: `getActionById(String)`

#### OperatorManager
- **位置**: `com.codingapi.flow.node.manager.OperatorManager`
- **职责**: 管理节点的操作者列表
- **核心方法**: `match(IFlowOperator)`

#### FieldPermissionManager
- **位置**: `com.codingapi.flow.node.manager.FieldPermissionManager`
- **职责**: 管理表单字段权限

#### StrategyManager
- **位置**: `com.codingapi.flow.node.manager.StrategyManager`
- **职责**: 管理节点策略（多人审批、超时、退回等）

---

### 7. 策略层 (Strategy Layer)

#### INodeStrategy
- **位置**: `com.codingapi.flow.strategy.INodeStrategy`
- **职责**: 节点策略的顶层接口

#### 策略类型

| 策略类 | 说明 |
|-------|------|
| MultiOperatorAuditStrategy | 多人审批策略（顺序/或签/并签/随机） |
| TimeoutStrategy | 超时策略 |
| SameOperatorAuditStrategy | 同一操作者审批策略 |
| RecordMergeStrategy | 记录合并策略 |
| ResubmitStrategy | 重新提交策略 |
| AdviceStrategy | 审批意见策略 |

---

### 8. 脚本层 (Script Layer)

#### ScriptRuntimeContext
- **位置**: `com.codingapi.flow.script.runtime.ScriptRuntimeContext`
- **职责**: Groovy脚本运行时环境
- **核心方法**: `execute(String method, String script, ...)`

#### 脚本类型

| 脚本类 | 说明 |
|-------|------|
| OperatorMatchScript | 操作者匹配脚本 |
| OperatorLoadScript | 操作者加载脚本 |
| NodeTitleScript | 节点标题脚本 |
| ConditionScript | 条件判断脚本 |
| ErrorTriggerScript | 异常触发脚本 |

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
        └───────────────────────────────────────────┘
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

## 三、核心设计模式

### 1. 建造者模式 (Builder Pattern)
- `WorkflowBuilder`: 构建流程定义
- `BaseNodeBuilder`: 构建节点对象
- `AuditNodeBuilder`: 构建审批节点

### 2. 工厂模式 (Factory Pattern)
- `NodeFactory`: 创建不同类型的节点
- `FlowActionFactory`: 创建不同类型的动作

### 3. 策略模式 (Strategy Pattern)
- `INodeStrategy`: 节点策略接口
- `MultiOperatorAuditStrategy`: 多人审批策略
- `TimeoutStrategy`: 超时策略

### 4. 模板方法模式 (Template Method Pattern)
- `BaseFlowNode`: 定义节点生命周期模板
- `BaseAction`: 定义动作执行模板

### 5. 单例模式 (Singleton Pattern)
- `NodeFactory.getInstance()`: 节点工厂单例
- `ScriptRuntimeContext.getInstance()`: 脚本运行时单例
- `RepositoryContext.getInstance()`: 仓储上下文单例

### 6. 责任链模式 (Chain of Responsibility Pattern)
- `triggerNode()`: 递归触发后续节点

---

## 四、扩展点

### 1. 自定义节点
继承 `BaseFlowNode` 或 `BaseAuditNode`，实现 `IFlowNode` 接口

### 2. 自定义动作
继承 `BaseAction`，实现 `IFlowAction` 接口

### 3. 自定义策略
实现 `INodeStrategy` 接口

### 4. 自定义脚本
使用 `ScriptRuntimeContext` 执行 Groovy 脚本

### 5. 事件扩展
监听 `FlowRecordStartEvent`, `FlowRecordTodoEvent`, `FlowRecordDoneEvent`, `FlowRecordFinishEvent`
