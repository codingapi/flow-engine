# 调整计划: processNodes 数据返回修复

> 编码: 024982 | 日期: 2026-04-28 | 类型: 调整任务 | 来源: change | 基于: docs/changes/024982-processNodes-data-fix.md

## 调整目标

修复 `processNodes()` 在返回流程节点数据时的三个问题：历史记录未按 nodeId 合并、TODO 记录被错误标记为历史、equals/hashCode 不一致。

## 影响面（基于指令结果）

- 涉及 Maven 模块: `flow-engine-framework`
- 直接依赖者: `flow-engine-starter-api`（仅引用 ProcessNode 类型作为返回值，无逻辑变更）
- 外部 API / 公共组件影响: 否，REST API 接口 `POST /api/cmd/record/processNodes` 签名和返回结构不变

## 分步策略

将调整拆分为 3 步，每步可独立通过编译验证：

1. **Step 1**: 修复 `ProcessNode` 的 equals/hashCode 一致性，新增合并方法
2. **Step 2**: 修复 `FlowProcessNodeService.processNodes()` 的历史记录合并逻辑与 TODO 过滤
3. **Step 3**: 编译验证 + 现有单测通过

## 新增文件

| 文件路径 | 用途说明 |
|----------|----------|

（无新增文件）

## 修改文件

| 文件路径 | 修改内容 |
|----------|----------|
| `flow-engine-framework/src/main/java/com/codingapi/flow/pojo/response/ProcessNode.java` | 1. `hashCode()` 改为只使用 `nodeId`，与 `equals()` 一致<br>2. 新增 `addOperator(FlowOperatorBody)` 方法，支持向已有 ProcessNode 追加审批人 |
| `flow-engine-framework/src/main/java/com/codingapi/flow/service/impl/FlowProcessNodeService.java` | 1. 新增 `buildHistoryNodes(List<FlowRecord>)` 方法，按 nodeId 合并历史记录<br>2. `processNodes()` 中 `isDone()` 分支：过滤仅保留已办记录，使用合并方法<br>3. `processNodes()` 中非 `isDone()` 分支：使用合并方法处理 beforeRecords |

## 移除文件（若有）

| 文件路径 | 移除原因 |
|----------|----------|

（无移除文件）

## 兼容性与迁移

- 保留的 API / 行为: `List<ProcessNode>` 返回类型不变；`ProcessNode` 的公共字段不变；REST API 端点签名不变
- 破坏性变更: `ProcessNode.hashCode()` 返回值变化（之前包含全部字段，现在只包含 nodeId）。如果下游代码将 ProcessNode 存入 HashMap/HashSet，行为会更正（之前因 equals/hashCode 不一致可能导致数据丢失）
- 数据/配置迁移: 无

## 核验机制

| 验证项 | 说明 |
|--------|------|
| 编译 | `mvn compile` 通过 |
| 单测 | `flow-engine-framework` 模块现有测试通过 |
| 打包 | `mvn package` 通过 |
| 行为回归 | `FlowDetailServiceTest.processNodes()` 测试用例通过 |

具体验证命令：

```bash
mvn -pl flow-engine-framework -am compile
mvn -pl flow-engine-framework -am test
mvn package
```

## 执行顺序

### Step 1: 修复 ProcessNode equals/hashCode + 新增合并方法

1. **修改** `flow-engine-framework/src/main/java/com/codingapi/flow/pojo/response/ProcessNode.java`
   - `hashCode()` 方法：改为 `return Objects.hash(nodeId);`（与 equals 一致）
   - 新增 `addOperator(FlowOperatorBody operator)` 方法：向 `this.operators` 追加审批人

### Step 2: 修复 FlowProcessNodeService 历史记录合并与 TODO 过滤

2. **修改** `flow-engine-framework/src/main/java/com/codingapi/flow/service/impl/FlowProcessNodeService.java`
   - 新增私有方法 `buildHistoryNodes(List<FlowRecord> records)`：
     - 使用 `LinkedHashMap<String, ProcessNode>` 按 nodeId 分组
     - 遍历 records，nodeId 相同时调用 `addOperator()` 追加审批人
     - 返回合并后的 `List<ProcessNode>`
   - 修改 `processNodes()` 方法第 92-103 行（`isDone()` 分支）：
     - 使用 `records.stream().filter(FlowRecord::isDone)` 过滤仅保留已办记录
     - 用 `buildHistoryNodes()` 替代原来的逐条 new ProcessNode
   - 修改 `processNodes()` 方法第 106-110 行（非 `isDone()` 分支）：
     - 用 `buildHistoryNodes()` 替代原来的逐条 new ProcessNode

### Step 3: 编译验证

3. **运行** `mvn -pl flow-engine-framework -am compile` 验证编译通过
4. **运行** `mvn -pl flow-engine-framework -am test` 验证单测通过

## 现状-目标对照表

| 现状位置 | 现状内容 | 目标变更 |
|----------|----------|----------|
| `ProcessNode.java:117-119` | `hashCode` 使用 `nodeId, nodeName, nodeType, state, operators` | 改为只使用 `nodeId`，与 `equals` 一致 |
| `ProcessNode.java` | 无合并方法 | 新增 `addOperator(FlowOperatorBody)` 方法 |
| `FlowProcessNodeService.java:93-97` | `isDone()` 分支加载所有记录逐条创建 ProcessNode | 过滤仅已办记录 + 按 nodeId 合并 |
| `FlowProcessNodeService.java:106-110` | 非 `isDone()` 分支逐条创建 ProcessNode | 按 nodeId 合并 |
