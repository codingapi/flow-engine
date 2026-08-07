---
name: flow-engine-framework/delay
module: flow-engine-framework
description: 延迟任务调度能力，支持延迟节点的定时触发、任务持久化与共享调度线程
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - DelayTaskManager
  - DelayTask
  - DelayStrategy
  - FlowDelayTaskRunner
  - FlowDelayTriggerService
  - DelayTaskRepository
content_hash: b97e80c36aaff10a1efb4e2819301980dddc0abb111cb4da4cd21eed85e3bede
---

## 解决什么问题

提供延迟节点的定时触发能力，解决以下问题：

- **延迟节点**：`DelayStrategy` 配置延迟时长（秒/分钟/小时/天），到达触发时间后自动执行流程流转
- **任务持久化**：`DelayTask` 记录触发时间、当前流程记录与延迟节点，通过 `DelayTaskRepository` 持久化
- **共享调度线程**：`DelayTaskManager` 使用单条共享调度线程（`delay-task-trigger`）替代每个任务一个 Timer 原生线程，避免线程泄漏
- **启动恢复**：`FlowDelayTaskRunner` 在应用启动时加载未触发的延迟任务，关闭时取消全部任务

## 如何使用

### 核心组件

| 组件 | 职责 |
|------|------|
| `DelayTaskManager` | 单例调度器，负责加载、添加、取消延迟任务 |
| `DelayTask` | 延迟任务领域对象，包含 id、createTime、triggerTime、currentRecordId、workCode、delayNodeId |
| `DelayStrategy` | 延迟节点策略，定义 `Type`（SECOND/MINUTE/HOUR/DAY）与 `time`，计算 `triggerTime` |
| `FlowDelayTaskRunner` | Spring Boot 启动器，`run()` 时启动 `DelayTaskManager`，`destroy()` 时关闭 |
| `FlowDelayTriggerService` | 触发延迟任务时执行流程流转的服务 |
| `DelayTaskRepository` | 延迟任务仓储接口 |

### 调度机制

- 应用启动时 `FlowDelayTaskRunner.run()` 调用 `DelayTaskManager.start(repositoryHolder)`，从仓储加载全部未触发任务并按触发时间调度
- 每个任务到达触发时间后由共享调度线程执行，先清理线程缓存，再通过 `FlowDelayTriggerService.trigger()` 触发流程流转，完成后删除该延迟任务
- 应用关闭时 `destroy()` 取消全部待执行任务

## 使用实例

```java
// 1. 创建延迟任务（延迟节点到达时生成）
DelayStrategy strategy = DelayStrategy.defaultStrategy(); // 默认 5 秒
DelayTask task = new DelayTask(strategy, flowRecord, delayNodeId);

// 2. 添加任务（持久化并调度）
DelayTaskManager.getInstance().addTask(task, repositoryHolder);

// 3. 应用启动时自动恢复未触发任务
// FlowDelayTaskRunner.run() -> DelayTaskManager.getInstance().start(repositoryHolder)
```