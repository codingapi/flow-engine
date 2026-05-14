# 修复计划: processNodes 接口返回数据异常

> 编码: 107562 | 日期: 2026-05-14 | 类型: Bug 修复 | 来源: bugfix | 基于: docs/bugs/107562-processNodes-节点路径错误.md

## 根因分析

问题位于 `FlowProcessNodeService.NextNodeLoader.fetchNextNode()`（`flow-engine-framework/src/main/java/com/codingapi/flow/service/impl/FlowProcessNodeService.java:170-192`），关键代码在第 189-190 行：

```java
List<IFlowNode> nextNodes = workflow.nextNodes(flowNode);
this.fetchNextNode(flowSession.updateSession(flowNode), nextNodes);
```

`workflow.nextNodes(flowNode)`（`Workflow.java:346`）只负责从节点定义中获取所有下一节点，不做条件过滤。运行时分支选择是由 `FlowSession.matchNextNodes()`（`FlowSession.java:286-293`）通过调用 `nextNode.filterBranches(nodeList, this)` 来完成的。

但 `NextNodeLoader.fetchNextNode()` 是 `processNodes` 接口的预览路径，**完全没有调用 `filterBranches()`**：当 `flowNode` 是 `ConditionNode` / `InclusiveNode` 时，`workflow.nextNodes()` 直接返回该控制节点下的**所有分支节点**（多个 `ConditionBranchNode` + 一个 `ConditionElseBranchNode`），随后递归全部遍历，导致接口返回了所有可能路径的节点集合。

期望行为是：**调用 `filterBranches()` 让分支节点自身按条件脚本决策**——
- 若 if 分支的条件脚本能依据表单参数返回 `true`，则走匹配的 if 分支
- 若 if 全部不匹配（或表单数据缺失导致脚本异常），则 fallback 到 else 分支

这一行为在 `ConditionBranchNode.filterBranches`（`ConditionBranchNode.java:54-67`）中已经实现：它按 `node.handle(flowSession)` 收集匹配的分支并取 order 最小的；而 `ConditionElseBranchNode.handle`（`ConditionElseBranchNode.java:41-43`）恒返回 `true`，天然兜底。问题只在于 `processNodes` 路径根本没去调它。

证据：
- `FlowProcessNodeService.java:170-192` 递归未做分支过滤
- `FlowSession.matchNextNodes`（`FlowSession.java:286-293`）是正确调用 `filterBranches()` 的对照实现
- 前一次提交 `a9ffb2b refactor(024982): processNodes 数据返回修复` 只处理了历史节点合并，未处理分支选择问题

## 影响面评估

- 直接修改的 Maven 模块: `flow-engine-framework`
- 可能回归的场景: 
  - 含条件分支（`ConditionNode`）的流程预览
  - 含包容分支（`InclusiveNode`）的流程预览
  - 已运行流程的"未来节点"预览（包含分支跨越的情况）
  - 普通无分支流程（应当无任何变化）
- 是否需要数据迁移/配置变更: 否

## 新增文件

| 文件路径 | 用途说明 |
|----------|----------|
| `flow-engine-framework/src/test/java/com/codingapi/flow/service/FlowProcessNodeBranchTest.java` | 针对条件分支与包容分支默认走 else 的回归用例 |

## 修改文件

| 文件路径 | 修改内容 |
|----------|----------|
| `flow-engine-framework/src/main/java/com/codingapi/flow/service/impl/FlowProcessNodeService.java` | 在 `NextNodeLoader.fetchNextNode()` 中获取 `nextNodes` 之后，对条件 / 包容分支节点集合调用 `filterBranches(nextNodes, flowSession)` 进行决策；条件脚本执行异常（表单数据不完整等）时降级到 else 分支。运行时分支选择逻辑无变化 |

## 关键代码修改

`NextNodeLoader.fetchNextNode()` 内将原本：

```java
List<IFlowNode> nextNodes = workflow.nextNodes(flowNode);
this.fetchNextNode(flowSession.updateSession(flowNode), nextNodes);
```

改为先调用新增私有方法 `selectBranchNodes(...)` 过滤之后再递归：

```java
List<IFlowNode> nextNodes = workflow.nextNodes(flowNode);
List<IFlowNode> selected = selectBranchNodes(nextNodes, flowSession.updateSession(flowNode));
this.fetchNextNode(flowSession.updateSession(flowNode), selected);
```

新增方法的设计要点：

```java
private List<IFlowNode> selectBranchNodes(List<IFlowNode> nextNodes, FlowSession flowSession) {
    if (nextNodes == null || nextNodes.isEmpty()) {
        return nextNodes;
    }
    IFlowNode first = nextNodes.get(0);
    String type = first.getType();
    boolean isBranchControl =
            ConditionBranchNode.NODE_TYPE.equals(type)
                    || ConditionElseBranchNode.NODE_TYPE.equals(type)
                    || InclusiveBranchNode.NODE_TYPE.equals(type)
                    || InclusiveElseBranchNode.NODE_TYPE.equals(type);
    if (!isBranchControl) {
        return nextNodes;
    }
    try {
        // 优先按 if 条件脚本匹配；脚本天然 fallback else（else.handle() 恒为 true）
        List<IFlowNode> matched = first.filterBranches(nextNodes, flowSession);
        if (matched != null && !matched.isEmpty()) {
            return matched;
        }
    } catch (Exception ignored) {
        // 表单数据缺失导致条件脚本异常 —— 降级到 else
    }
    List<IFlowNode> elseOnly = nextNodes.stream()
            .filter(n -> ConditionElseBranchNode.NODE_TYPE.equals(n.getType())
                    || InclusiveElseBranchNode.NODE_TYPE.equals(n.getType()))
            .toList();
    return elseOnly.isEmpty() ? nextNodes : elseOnly;
}
```

要点说明：
1. **复用 `filterBranches()` 现有逻辑**，与运行时（`FlowSession.matchNextNodes`）行为一致：若表单参数能让 if 条件脚本返回 `true`，则走匹配的 if 分支；若 if 不匹配，由 `ConditionElseBranchNode.handle()` 恒 `true` 的特性自然走 else
2. **异常降级**：预览阶段表单数据可能为空 / 缺字段，条件脚本访问未定义字段会抛 `groovy.lang.MissingPropertyException` 等；通过 try-catch 将异常降级为"走 else"
3. **兜底**：当 `filterBranches()` 返回空且无 else 分支时，保留原始列表（兼容历史数据，不让分支节点凭空消失）
4. 不影响 `ParallelNode`（并行节点）、`ManualNode`、`RouterNode` —— 它们各自的 `filterBranches()` 行为不变，由现有递归路径处理

## 回归验证

| 验证项 | 说明 |
|--------|------|
| 表单数据不足走 else | 构造含 `ConditionNode` 的流程，预览时不提供条件字段，验证返回的是 else 分支路径 |
| 表单数据匹配 if 走 if | 构造含 `ConditionNode` 的流程，提供能让 if 条件脚本返回 `true` 的表单参数，验证返回 if 分支路径 |
| 多条件分支场景 | 构造含两个嵌套 `ConditionNode` 的流程，验证按各自表单条件分别走 if 或 else，最终路径唯一 |
| 包容分支场景 | 构造含 `InclusiveNode` 的流程，验证按条件匹配（无数据时走 `InclusiveElseBranchNode`）|
| 无分支场景 | 普通直线流程（Start → Approval → End），验证 `processNodes` 行为无变化 |
| 关联用例 | 运行 `flow-engine-framework` 既有测试（`FlowDetailServiceTest` 等），保证未引入回归 |
| 新增用例 | 新建 `FlowProcessNodeBranchTest` 覆盖上述四类场景 |
| 编译验证 | `mvn -pl flow-engine-framework -am compile` |

具体验证命令：

```bash
mvn -pl flow-engine-framework -am compile
mvn -pl flow-engine-framework test -Dtest=FlowProcessNodeBranchTest
mvn -pl flow-engine-framework test
```

## 执行顺序

1. 创建 git worktree（按项目规范，所有开发必须在 worktree 中进行）
2. 修改 `flow-engine-framework/src/main/java/com/codingapi/flow/service/impl/FlowProcessNodeService.java`：
   - 引入 `ConditionBranchNode` / `ConditionElseBranchNode` / `InclusiveBranchNode` / `InclusiveElseBranchNode` 的 import
   - 新增私有方法 `selectDefaultBranchNodes(List<IFlowNode>)`
   - 修改 `NextNodeLoader.fetchNextNode()` 在递归前调用过滤方法
3. 创建 `flow-engine-framework/src/test/java/com/codingapi/flow/service/FlowProcessNodeBranchTest.java` 覆盖五类场景：
   - 表单缺失条件字段 → 走 else 分支
   - 表单提供匹配条件 → 走对应 if 分支
   - 多嵌套条件分支 → 按各自数据决策
   - 包容分支按表单匹配 / 缺失走 else
   - 无分支流程不受影响
4. 执行 `mvn -pl flow-engine-framework -am compile` 编译验证
5. 执行 `mvn -pl flow-engine-framework test` 完整回归
6. 人工审核后提交 PR（目标分支：dev）

## 注意事项

- 严格遵循 CLAUDE.md 中的开发规范：本变更必须在 git worktree 中进行，PR 目标为 `dev` 分支
- 不修改 `FlowSession.matchNextNodes()` 等运行时路径，避免影响实际审批流转
- 修复仅作用于预览路径（`FlowProcessNodeService`），运行时分支选择仍由 `filterBranches()` 在 `FlowActionService` 等动作触发时使用

## 变更说明

- 2026-05-14: 根据用户澄清调整 —— 条件分支决策不应一刀切走 else，而应在 if 条件参数有值且匹配时走 if，仅在数据缺失 / 不匹配时 fallback 到 else。实现方式从"强制 else"改为"调用 `filterBranches()` + 异常降级 else"，与运行时 `FlowSession.matchNextNodes` 行为一致。
