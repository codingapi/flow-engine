# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本项目中工作时提供指导。

## 项目概述

Flow Engine 是一个企业级工作流引擎，基于 Java 17 和 Spring Boot 3.5.9 构建（当前版本 0.0.18）。

### 核心模块

| 模块 | 说明 |
|------|------|
| `flow-engine-framework` | 核心工作流引擎框架（节点/动作/策略/脚本/服务） |
| `flow-engine-starter` | Spring Boot 自动配置入口 |
| `flow-engine-starter-api` | REST API 层 |
| `flow-engine-starter-infra` | 持久化层（JPA 实体、仓储实现） |
| `flow-engine-starter-query` | 查询层 |
| `flow-engine-example` | 示例应用 |

### 框架层包结构

| 包路径 | 职责 |
|--------|------|
| `com.codingapi.flow.node` | 19 种流程节点类型 |
| `com.codingapi.flow.action` | 8 种动作类型 |
| `com.codingapi.flow.strategy` | 节点策略和工作流策略 |
| `com.codingapi.flow.script` | Groovy 脚本运行时 |
| `com.codingapi.flow.repository` | 仓储接口定义 |
| `com.codingapi.flow.builder` | 建造者模式实现 |
| `com.codingapi.flow.manager` | 业务管理器 |
| `com.codingapi.flow.service` | 业务服务层 |

## 核心架构

理解以下跨文件的模式后再动手改动，可避免破坏引擎一致性。

### 服务编排模式（FlowService）

`FlowService`（`flow-engine-framework/.../service/FlowService.java`）是 `@Transactional` 业务入口。**每个流程操作对应一个独立的 `FlowXxxService`**（`FlowCreateService` / `FlowActionService` / `FlowRevokeService` / `FlowDeleteService` / `FlowUrgeService` / `FlowDetailService`），通过 `IRepositoryHolder` 注入依赖，且每次操作前调用 `FlowOperatorLocalThreadCache.getInstance().clear()` 清理线程缓存。新增流程操作应沿用此模式：新建 `FlowXxxService(request, repositoryHolder)`，并在 `FlowService` 暴露一个入口方法。

### 仓储持有者（IRepositoryHolder）

服务层不直接注入仓储，而是通过 `IRepositoryHolder` 获取 `FlowRecordService` / `WorkflowService` / `FlowOperatorGateway` 等依赖。两个实现：
- `RepositoryHolderContext` — 生产，单例，由 `flow-engine-starter` 自动配置注册
- `MockRepositoryHolder` — 测试 / Mock 场景

REST 层通过请求参数 `mockKey` 分流到 Mock 实例（见 `FlowRecordController.loadFlowService()` / `loadCurrentOperatorId()`）；`operatorId` 也可由该参数覆盖，便于无登录态测试。

### 流程记录生命周期

三类记录紧密绑定，须保持一致：
- `FlowRecord` — 流程执行记录（TODO / DONE 状态，`isTodo()` / `isDone()` / `isFinish()`）
- `FlowTodoRecord` — 待办记录
- `FlowTodoMerge` — 待办合并关系（开启记录合并 `isMergeable()` 时产生）

保存走 `FlowRecordSaveService.saveAll()`，单次调用顺序执行 `saveRecords → saveTodoMargeRecords → removeTodoMergeRecords`，保证三者一致。删除整条流程走 `FlowRecordService.deleteFlowRecord()`，按 合并关系 → 待办 → 流程记录 顺序清理。

### 节点 / 动作 / 策略三元组

19 种节点（`node/nodes/`）× 8 种动作（`action/actions/`），行为由策略（`strategy/node/`、`strategy/workflow/`）驱动而非继承扩展：
- `NodeStrategyManager` 持有节点策略（如 `RevokeStrategy`、`OperatorLoadStrategy`、`RecordMergeStrategy`）
- `ActionManager` 持有节点动作
- 节点类型常量见 `StartNode.NODE_TYPE`（= `NodeType.START.name()`），`FlowRecord.isNodeType(String)` 用于按类型判定

### 事件（IFlowEvent + EventPusher）

7 种事件：开始 / 待办 / 已办 / 完成 / 催办 / 撤销（`FlowRecordRevokeEvent`）/ 删除（`FlowRecordDeleteEvent`）。操作完成后用 `EventPusher.push(event)` 推送；事件通常携带 `FlowRecord` + `mock` 标记。外部可用 Spring `@EventListener` 订阅。

### API 模块划分

- **命令操作**：`flow-engine-starter-api` — `FlowRecordController`（`/api/cmd/record`：create / action / revoke / delete / urge / detail）与 `WorkflowController`（`/api/cmd/workflow`：流程设计 CRUD）
- **查询**：`flow-engine-starter-query` — `FlowRecordQueryController`（`/api/query/record`：list / todo / done / notify）
- **持久化**：`flow-engine-starter-infra` — JPA 实体与仓储实现（含达梦方言）

## 常用命令

### 构建与测试

```bash
# 构建整个项目
./mvnw clean install

# 运行所有测试
./mvnw test

# 运行特定模块的测试
./mvnw test -pl flow-engine-framework

# 运行特定的测试类
./mvnw test -Dtest=ScriptRuntimeContextTest

# 运行示例应用
cd flow-engine-example && mvn spring-boot:run
```

## 开发规范

### 禁止自动提交

- **未经用户明确要求，任何情况下不得执行 `git commit` / `git push` / `git merge` 操作**
- 所有代码变更需经用户审核后再提交

### 基本规范

- **与用户沟通及编写文档时，所有内容必须使用中文表述**
- **在每次修改代码以后，要执行本地化的编译验证确保代码没有错误**
- Java 代码遵循 Spring Boot 规范

### 测试规范

本项目后端采用 JUnit 5 进行单元测试。

**测试文件位置：**

- 测试文件放在 `src/test/java/` 目录下，与源代码同级的测试目录中
- 测试文件命名：`*Test.java`

**测试编写流程：**

1. **Red（红灯）**：先编写测试用例，明确预期行为
2. **Green（绿灯）**：编写最小代码使测试通过
3. **Refactor（重构）**：优化代码结构，运行全部测试确认不回归

**测试用例设计要求：**

- 每个测试用例需明确：测试目标、前置条件、执行步骤、期望断言
- 优先测试核心业务逻辑（Service、Manager、Converter、Utils 等）
- 测试应该有确定性结果，避免依赖外部状态

**测试命令：**

```bash
# 运行所有测试
./mvnw test

# 运行特定模块的测试
./mvnw test -pl flow-engine-framework

# 运行特定的测试类
./mvnw test -Dtest=ScriptRuntimeContextTest

# 运行特定包的测试（使用 Maven surefire）
./mvnw test -pl flow-engine-framework -Dtest=**/script/**
```

**示例（基于 flow-engine-framework 的脚本测试）：**

```java
// flow-engine-framework/src/test/java/com/codingapi/flow/script/ScriptRuntimeContextTest.java
package com.codingapi.flow.script;

import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScriptRuntimeContextTest {

    @Test
    void should_execute_groovy_script() {
        // given
        String script = "return 1 + 1";

        // when
        Object result = ScriptRuntimeContext.getInstance().executeScript(script, context);

        // then
        assertEquals(2, result);
    }
}
```

<!-- PKR-START -->
## PKR 知识查阅（编码前必须）

进入计划模式或实现功能前，**必须按以下优先级查阅**：

### ⚠️ 开发规范（最高优先级，必须严格遵守）

1. [docs/conventions/index.md](./docs/conventions/index.md) — 项目开发规范

**规范具有最高优先级。** 所有代码必须遵循已注册的 Convention，违反规范的代码视为缺陷。
编码前必须逐条检查相关规范，确保命名、结构、模式完全符合要求。

### 已有能力（必须复用，禁止重复实现）

2. [docs/capabilities/index.md](./docs/capabilities/index.md) — 已有可复用能力

已有能力必须复用，禁止重新实现。优先组合已有能力解决问题。

### 计划模式约束

计划方案中必须包含：
1. **遵循了哪些规范** — 列出遵守的 Convention（必须首先说明）
2. **复用了哪些已有能力** — 列出从 PKR 中找到并复用的 Capability
3. **是否有新增能力** — 如果本次开发产生了可复用的新能力，完成后通过 `/pkr-add` 注册

### 知识管理命令

| 命令 | 用途 |
|------|------|
| `/pkr-init` | 扫描项目，发现候选能力和规范（自动跳过已有文档） |
| `/pkr-sync` | 全量同步，对比代码变更 |
| `/pkr-update <module>/<name> [desc]` | 单项更新，可带描述指导更新 |
| `/pkr-add <module>/<name> <desc>` | 从代码/框架扫描注册 |
| `/pkr-add plan <module>/<name> <desc>` | 注册计划中的能力 |
| `/pkr-export <module> ...` | 导出模块文档供其他项目使用 |
<!-- PKR-END -->
