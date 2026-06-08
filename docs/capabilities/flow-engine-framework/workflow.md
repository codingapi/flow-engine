---
name: flow-engine-framework/workflow
module: flow-engine-framework
description: 工作流引擎核心模型，包含流程定义、构建器、版本管理和运行时快照
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - Workflow
  - WorkflowBuilder
  - WorkflowVersion
  - WorkflowRuntime
content_hash: 29094b1005058e20724c464ed636bd78a8b1b34c9b83b14caacaf4d32d1560bb
---

## 解决什么问题

提供工作流的核心领域模型，解决流程定义、版本管理和运行时快照的问题：

- **流程定义**：`Workflow` 作为流程的核心数据结构，包含节点列表、表单、策略等完整流程配置
- **流程构建**：`WorkflowBuilder` 提供链式建造者模式，简化流程定义的创建过程
- **版本管理**：`WorkflowVersion` 支持流程的多版本管理，包括版本启用/禁用和版本间转换
- **运行时快照**：`WorkflowRuntime` 将流程配置序列化为 JSON 存储，在流程实例运行时反序列化恢复

## 如何使用

### 核心类说明

| 类 | 职责 |
|---|------|
| `Workflow` | 流程核心模型，包含 id、code、title、nodes、strategies、form 等 |
| `WorkflowBuilder` | 链式构建器，通过 `builder()` 静态方法创建 |
| `WorkflowVersion` | 流程版本，支持 `enableVersion()` / `disableVersion()` 切换 |
| `WorkflowRuntime` | 运行时快照，通过 `toJson()` / `formJson()` 实现序列化 |

### 依赖说明

- `Workflow` 依赖 `IFlowNode`（节点列表）、`FlowForm`（表单）、`IWorkflowStrategy`（策略列表）
- `WorkflowRuntime` 通过 `Workflow` 构造，将完整流程配置序列化为 JSON 字符串
- 版本切换通过 `WorkflowVersion.enableVersion()` / `disableVersion()` 实现

## 使用实例

```java
// 1. 使用 Builder 创建流程
Workflow workflow = WorkflowBuilder.builder()
    .id("flow-001")
    .code("leave-request")
    .title("请假流程")
    .description("员工请假审批流程")
    .addNode(startNode)
    .addNode(approvalNode)
    .addNode(endNode)
    .strategies(List.of(urgeStrategy))
    .build();

// 2. 创建流程版本
WorkflowVersion version = new WorkflowVersion(workflow);
version.enableVersion();   // 启用为当前版本

// 3. 创建运行时快照
WorkflowRuntime runtime = new WorkflowRuntime(workflow);
// 持久化 runtime.getWorkflow() (JSON 字符串)

// 4. 从运行时快照恢复
Workflow restored = runtime.toWorkflow();
```
