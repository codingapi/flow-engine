---
name: flow-test-workflow
description: 流程场景测试与问题处理工作流：按 TDD 编写流程场景单元测试、定位并确认引擎问题、创建 issue 记录问题并关联提交推送。当用户要求测试某个流程场景、验证流程行为、排查流程问题，或要求将修复以 issue 关联方式提交时使用。
---

# 流程场景测试与问题处理工作流

完整的闭环流程：**研究机制 → 编写场景测试（Red）→ 定位确认问题 → 最小化修复（Green）→ 回归验证 → issue 记录与关联提交**。

## 阶段一：编码前准备（必须）

1. **查阅 PKR 知识**（CLAUDE.md 强制要求，按优先级）：
   - `docs/conventions/index.md` — 开发规范，最高优先级
   - `docs/capabilities/index.md` — 已有能力，**必须复用，禁止重复实现**
2. **研究现有测试模式**（`flow-engine-framework/src/test/java/com/codingapi/flow/`）：
   - `factory/MyFlowServiceFactory` — 内存仓储 + FlowService 的装配方式
   - `service/OperatorSelectTest` — 发起人设定 / 审批人设定（operatorSelectMap）用法
   - `service/FlowSampleServiceTest` — 拒绝、退回、并行等综合场景
   - `service/FlowDetailServiceTest` — processNodes 流程节点展示断言
3. **逐一确认场景依赖的机制**，常用对照：

   | 场景需求 | 对应机制 |
   |---|---|
   | 发起人指定审批人 | `OperatorLoadStrategy.initiatorSelectStrategy()` + `operatorSelectMap` |
   | 审批人指定审批人 | `OperatorLoadStrategy.approverSelectStrategy()` + `operatorSelectMap` |
   | 脚本动态加载审批人 | `OperatorLoadStrategy(脚本key)`（SCRIPT 模式） |
   | 多人均需审批 | `MultiOperatorAuditStrategy(MERGE, 1.0f)` 并签；默认 SEQUENCE 顺序审批 |
   | 拒绝退回指定节点 | `RejectAction.setScript(FlowGroovyScriptFactory.createActionRejectScript("def run(request){return '节点id'}").getKey())`（注意传脚本 key 而非脚本内容） |
   | 显式退回 | `ReturnAction` + `advice.setBackNodeId(节点id)` |

## 阶段二：编写场景测试（Red）

- 位置与命名：`src/test/java/` 同包目录，`*Test.java`，JUnit 5。
- **按交互步骤拆分私有助手方法**（如 `startFlowToBNode()` / `passBNodeToCNode()` / `passCNodeToDNode()`），正常与回退场景复用，断言写在各自测试方法中，避免把记录数量等场景相关断言写进助手方法。
- 每一步的断言要点：
  - 谁的待办：`flowRecordRepository.findTodoByOperator(userId)` 的数量与 `record.getNodeId()`
  - 流程实例一致性：回退重走后 `processId` 不变
  - 结束判定：`findProcessRecords(processId)` 全部 `isFinish()`（注意：结束节点记录不持久化，记录数不含结束记录）
  - 展示类验证点：`flowService.processNodes(new FlowProcessNodeRequest(recordId, operatorId, data))`，断言 `ProcessNode` 的 `approveState`、`operatorStrategy`、`operators`
- 涉及 Groovy 脚本时，脚本以文本块编写，经 `FlowGroovyScriptFactory.createXxxScript(script).getKey()` 注册后配置到节点/动作。
- **运行测试确认 Red**：`./mvnw test -pl flow-engine-framework -Dtest=测试类`。失败原因应能指向真实缺口（如 `MissingMethodException: GroovyScriptRequest.getCurrentRecord()` 说明脚本 API 缺失）。

## 阶段三：定位与确认问题（Red → Green）

1. **精读失败信息**，区分缺口类型：
   - `MissingMethodException` / 编译错误 → API 能力缺失
   - 断言数值不符 → 运行时状态与预期不一致，需要追踪
2. **加临时诊断**（验证后必须移除）：
   - Groovy 脚本内 `println` 打印上下文（currentRecord、nodeRecords 等）
   - 测试中打印记录明细：`id / nodeId / currentOperatorId / fromId / nodeOrder / hidden / isTodo`
   - 注意 `findProcessRecords` 会过滤 hidden 记录，隐藏记录可用 `findDoneByOperator` 等辅助观察
3. **沿调用链追到根因**，不要停在表象。示例（本项目真实案例）：
   - D 节点审批人顺序与 C 节点不一致 → 根因是 `FlowOperatorLocalThreadCache.find` 合并缓存时已缓存人员排前、未缓存追加在后，未按入参顺序返回
   - 预览展示异常 → 区分运行时会话与预览会话（`FlowProcessNodeService.buildFlowSession`）的语义差异
4. **最小化修复**原则：
   - 业务能力放在领域层（如 `FlowSession`），脚本请求对象（`GroovyScriptRequest`）只做透传
   - 定向查询优于全量查询：`findCurrentNodeRecords(fromId, nodeId)` 优于 `findProcessRecords(processId)` 全量过滤
   - 注意预览与运行态两种上下文的行为差异（预览时 currentRecord 不会随节点遍历前进，必要时加"直接前驱"等防护）
5. 每处修复后先定向重跑失败用例，再跑模块全量。

## 阶段四：回归验证

```bash
./mvnw test -pl flow-engine-framework   # 模块测试
./mvnw clean install                    # 全项目编译 + 全部测试
```

全部通过后才可进入提交环节；确认临时诊断代码已移除（`grep println` 自查）。

## 阶段五：issue 记录与关联提交（仅在用户明确要求时执行）

1. **先创建 issue 记录问题**（在提交之前）：

```bash
gh issue create --repo codingapi/flow-engine \
  --title "<问题标题>" \
  --body "## 场景说明
<流程结构与交互描述>

## 发现的问题
### 1. <问题一>
<现象与影响>

## 期望实现
<修复方案>

## 验证场景
<测试覆盖的场景清单>"
```

2. **提交并关联 issue**（提交信息以 `closes #<编号>` 或 `#<编号>` 引用，结尾附 Co-Authored-By）：

```bash
git add <改动文件>
git commit -m "feat/fix: <概述>

- <要点一>
- <要点二>

closes #<编号>

Co-Authored-By: Claude <noreply@anthropic.com>"
```

3. **推送并确认**：

```bash
git push origin <分支>
git status -sb                      # 确认与远程同步
gh issue view <编号> --repo codingapi/flow-engine   # 确认 issue 状态与关联
```

说明：`closes #N` 在提交合入默认分支（main）时才会自动关闭 issue，推送到 dev 阶段 issue 保持 OPEN 属正常现象。

## 前端代码处理（submodule）

本工程通过 **git submodule** 集成前端代码（`flow-frontend/` → `codingapi/flow-frontend` 仓库，main 分支）：

- 涉及前端的改动（审批界面、流程设计器、待办列表等交互/展示）**必须进入 `flow-frontend/` 子模块目录内处理**，在前端仓库中提交与推送
- **不能**把前端源码改动直接提交到当前后端仓库；后端仓库只记录子模块指针，如需更新指针，须在前端仓库提交推送后，再于后端仓库 `git add flow-frontend` 更新引用
- 后端仅提供前端所需的数据/配置（如节点策略、详情接口字段），前后端联动的问题需同时在两个仓库处理时，分别提交、分别关联 issue

## 项目红线（CLAUDE.md）

- **未经用户明确要求，禁止执行 `git commit` / `git push` / `git merge`**
- 与用户沟通及文档内容必须使用中文
- 每次修改代码后必须执行本地编译验证
- 计划/实现前必须查阅 PKR（规范 > 能力复用 > 新增能力登记 `/pkr-add`）
