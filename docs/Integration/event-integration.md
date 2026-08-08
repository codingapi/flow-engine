# 流程事件机制

引擎通过事件通知下游业务系统「发生了什么」。所有流程事件为**异步事件**，业务方通过实现 `IHandler<T>` 接口订阅。引擎不提供任何内置消息发送（站内信/IM/邮件），消息推送由订阅方自行实现。

## 1. 事件体系

### 1.1 接口层级

```java
package com.codingapi.flow.event;

import com.codingapi.springboot.framework.event.IAsyncEvent;

/** 流程触发的异步事件 */
public interface IFlowEvent extends IAsyncEvent {
    boolean isMock();
}
```

`IAsyncEvent` 来自外部框架 `springboot-starter`：

```java
public interface IEvent extends Serializable {}       // 默认同步事件标记
public interface ISyncEvent extends IEvent {}         // 同步事件标记
public interface IAsyncEvent extends IEvent {}        // 异步事件标记 ← IFlowEvent 继承它
```

同步/异步由 `DomainEventContext.push` 的 `instanceof` 判定：`IAsyncEvent` → 异步（线程池执行）；`ISyncEvent`/裸 `IEvent` → 同步（发布线程内联执行）。

### 1.2 七种事件

所有事件均携带 `FlowRecord`（或当前记录）与 `boolean mock` 标记（Mock 模式为 `true`）。

| 事件类 | 触发时机 | 构造参数 | 访问器 |
|---|---|---|---|
| `FlowRecordStartEvent` | 流程发起（每条新建记录） | `FlowRecord, boolean mock` | `getFlowRecord()` / `isMock()` |
| `FlowRecordTodoEvent` | 生成待办记录 | `FlowRecord, boolean mock` | `getFlowRecord()` / `isMock()` |
| `FlowRecordDoneEvent` | 记录办结 | `FlowRecord, boolean mock` | `getFlowRecord()` / `isMock()` |
| `FlowRecordFinishEvent` | 流程**正常完成**（结束节点） | `FlowRecord, boolean mock` | `getFlowRecord()` / `isMock()` |
| `FlowRecordRevokeEvent` | 撤销（被撤销的后置记录） | `FlowRecord currentRecord, boolean mock` | `getCurrentRecord()` / `isMock()` |
| `FlowRecordUrgeEvent` | 催办（每条待办） | `FlowRecord, IFlowOperator urgeOperator, boolean mock` | `getFlowRecord()` / `getUrgeOperator()` / `isMock()` |
| `FlowRecordDeleteEvent` | 删除未流转实例（开始节点） | `FlowRecord, boolean mock` | `getFlowRecord()` / `isMock()` |

范例（`FlowRecordUrgeEvent.java`，唯一带三个字段的事件）：

```java
@Getter
@AllArgsConstructor
public class FlowRecordUrgeEvent implements IFlowEvent {
    private final FlowRecord flowRecord;
    private final IFlowOperator urgeOperator;
    private final boolean mock;
}
```

### 1.3 事件携带的 `FlowRecord` 关键内容

`FlowRecord`（`flow-engine-framework/.../record/FlowRecord.java`）提供：

- 基本信息：`getId()`、`getProcessId()`、`getWorkTitle()`、`getWorkCode()`、`getNodeId()`、`getNodeType()`、`getNodeName()`、`getTitle()`、`getFormData()`（`Map<String,Object>`）
- 操作人：`getCurrentOperatorId/Name()`、`getSubmitOperatorId/Name()`、`getCreateOperatorId/Name()`、`getForwardOperatorId/Name()`
- 状态：`isTodo()`、`isDone()`、`isFinish()`、`isAutoDone()`、`isShow()`、`isNotEndNode()`
- 抄送标记：`isNotify()`
- 动作：`getActionId()`、`getActionType()`、`getActionName()`、`getAdvice()`
- 并行：`getParallelId()`、`getParallelBranchNodeId()`、`getParallelBranchTotal()`
- 时间：`getCreateTime()`、`getUpdateTime()`、`getFinishTime()`、`getTimeoutTime()`

## 2. 推送：`EventPusher`

```java
package com.codingapi.springboot.framework.event;

/** 事件推送助手 */
public class EventPusher {

    /** 推送事件。默认自动检测循环事件，出现循环事件时抛出循环调用异常。 */
    public static void push(IEvent event) {
        push(event, false);
    }

    /** hasLoopEvent=true 时跳过循环事件检测 */
    public static void push(IEvent event, boolean hasLoopEvent) {
        DomainEventContext.getInstance().push(event, hasLoopEvent);
    }
}
```

调用方式（引擎内部统一使用单参形式）：

```java
EventPusher.push(new FlowRecordStartEvent(flowRecord, session.isMock()));
```

### 引擎内推送事件的位置

| 位置 | 推送内容 |
|---|---|
| `FlowCreateService.create()` | 每条新建记录：`StartEvent` + `TodoEvent` |
| `PassAction.run()` | 当前记录 `DoneEvent`；顺序多人激活 `TodoEvent`；或签/并签自动办结他人 `DoneEvent`；抄送记录 `DoneEvent`；委托回退 `TodoEvent`；下游节点记录 `TodoEvent`（抄送为 `DoneEvent`） |
| `RejectAction.run()` | 当前记录 `DoneEvent` + 回退新记录 `TodoEvent` |
| `ReturnAction.run()` | 当前记录 `DoneEvent` + 退回节点新记录 `TodoEvent` |
| `AddAuditAction.run()` | 加签记录 `TodoEvent` |
| `TransferAction.run()` | 当前记录 `DoneEvent` + 转交人新记录 `TodoEvent` |
| `DelegateAction.run()` | 当前记录 `DoneEvent` + 委托人新记录 `TodoEvent` |
| `EndNode.fillNewRecord()` | `FinishEvent`（流程正常完成） |
| `FlowSubProcessResultService.save()` | 子流程结果记录 `TodoEvent`（非抄送）或 `DoneEvent` |
| `FlowUrgeService.urge()` | 每条待办 `UrgeEvent` |
| `FlowRevokeService.revoke()` | 当前记录恢复待办 `TodoEvent` + 被撤销后置记录 `RevokeEvent` |
| `FlowDeleteService.delete()` | `DeleteEvent` |

> 事件在 `repositoryHolder.saveRecords(...)` **落库之后**推送，保证订阅方看到的记录已持久化。

### 结束节点虚拟记录拦截

`PassAction` 会拦截**结束节点虚拟记录**的 `TodoEvent`（结束节点不持久化、`operatorId=-1`，并非真实业务待办），避免下游收到「已完成流程的待办」：

```java
flowEvents.stream()
        .filter(event -> !isEndNodeTodoEvent(event))
        .forEach(EventPusher::push);

private boolean isEndNodeTodoEvent(IFlowEvent event) {
    return event instanceof FlowRecordTodoEvent todoEvent
            && !todoEvent.getFlowRecord().isNotEndNode();
}
```

## 3. 订阅：实现 `IHandler<T>`

业务方实现 `IHandler<T extends IEvent>` 接口并注册为 Spring Bean，即可订阅对应事件。

```java
package com.codingapi.springboot.framework.event;

public interface IHandler<T extends IEvent> {
    default int order() { return 0; }        // 同事件多订阅排序，升序执行
    void handler(T event);
    default void error(Exception e) { throw e instanceof RuntimeException r ? r : new RuntimeException(e); }  // 默认重新抛出
}
```

### 订阅示例（催办通知）

`flow-engine-example/src/main/java/com/codingapi/example/handler/MyFlowRecordUrgeEventHandler.java`：

```java
package com.codingapi.example.handler;

import com.codingapi.flow.event.FlowRecordUrgeEvent;
import com.codingapi.springboot.framework.event.IHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MyFlowRecordUrgeEventHandler implements IHandler<FlowRecordUrgeEvent> {

    @Override
    public void handler(FlowRecordUrgeEvent event) {
        log.info("催办 event:{}", event);
    }
}
```

要点：

- **泛型决定订阅的事件类型**：`IHandler<FlowRecordUrgeEvent>` 只订阅催办事件；泛型为父类型/接口时可订阅其全部子类（`isAssignableFrom` 匹配）。
- **注册方式**：`@Component` 即可，Spring 启动时自动收集注入 `SpringDefaultEventHandler`。
- **排序**：同事件多个订阅按 `order()` 升序执行。
- **异常处理**：handler 抛异常后调用其 `error(e)`；默认 `error` 重新抛出，会阻止后续 handler 执行。单个订阅失败不阻止其他订阅（除非 error 回调抛出）。

## 4. 完整分发管道

```
业务代码 → EventPusher.push(event)
  → DomainEventContext.push   （IAsyncEvent → 异步）
    → EventTraceContext 记录 trace + 循环事件检测（命中抛 EventLoopException）
    → Spring ApplicationContext.publishEvent(new DomainEvent(event, sync, traceId))
      → SpringDefaultEventHandler.@EventListener dispatch
        → 异步：executorService.execute(() -> ApplicationHandlerUtils.handler(event))
          同步：内联执行
          → 按 order() 排序，按泛型匹配执行所有 IHandler
```

### 4.1 循环事件检测

- 同一 traceId 内，同类事件再次 push 会抛 `EventLoopException`（防止事件→动作→事件的无限循环）。
- 跳过检测：`EventPusher.push(event, true)`（引擎内部未使用，保留给特殊情况）。

### 4.2 异步线程池

- 异步事件由框架自建固定线程池执行（`Executors.newFixedThreadPool`），线程池大小来自 `FrameworkProperties.handlerThreadPoolSize`，**默认 20**。
- 配置项：`codingapi.framework.handler-thread-pool-size`（以实际 `FrameworkProperties` 前缀为准）。
- 注意：异步 ≠ Spring `@Async`，是框架自建线程池，事件回调内的事务边界需自行处理。

### 4.3 事务变体（可选）

配置 `codingapi.framework.event.transaction.enable=true` 时，框架改为装配 `SpringTransactionEventHandler`：使用 `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`，在**事务提交后**才分发事件（无事务时 fallback 立即执行）；否则默认装配 `SpringDefaultEventHandler`（发布即分发）。

### 4.4 无 Spring 上下文时的行为

`DomainEventContext` 在 `ApplicationContext` 未初始化时（如纯单元测试）**静默丢弃事件**。

## 5. 通知/消息扩展点

引擎**不提供**任何消息发送/桥接抽象。与「通知」相关的能力：

- **抄送节点**（`NotifyNode`，`NodeType.NOTIFY`）：生成抄送记录（`flowRecord.notifyRecord(session)`），引擎推送 `FlowRecordDoneEvent`（抄送记录不产生待办事件）。
- **催办**：`FlowUrgeService` + `FlowRecordUrgeEvent`，通知发送由订阅方实现。
- **消息推送**（站内信/IM/邮件等）：自行实现 `IHandler<FlowRecordXxxEvent>`，在 `handler` 中对接自己的消息服务。

## 6. Mock 模式标记

- 事件携带 `mock` 标记：有 session 的场景用 `session.isMock()`（等价于 `repositoryHolder instanceof MockRepositoryHolder`）；无 session 的服务（`FlowDeleteService`/`FlowUrgeService`/`FlowRevokeService`）直接用 `repositoryHolder instanceof MockRepositoryHolder`。
- 订阅方可通过 `event.isMock()` 区分测试/演示流量与生产流量（例如不发送真实消息）。