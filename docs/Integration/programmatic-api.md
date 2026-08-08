# 编程式 API

除 REST API 外，引擎提供完整的 Java 编程式 API：构建流程设计、发起流程、执行动作、查询记录。业务方可直接注入 `FlowService` / `WorkflowService` / `FlowRecordService` 使用。

## 1. 服务编排入口：`FlowService`

`flow-engine-framework/.../service/FlowService.java`，`@Transactional`，由 starter 注册为 Bean。每个方法内部先清理 `FlowOperatorLocalThreadCache` 与 `FlowRuntimeScriptLocalCache`，再委托对应 `FlowXxxService`：

```java
public class FlowService {
    FlowContent detail(FlowDetailRequest request);                       // 流程详情
    List<ProcessNode> processNodes(FlowProcessNodeRequest request);      // 流转节点预览
    long create(FlowCreateRequest request);                              // 发起流程 → 返回首条记录 id
    ActionResponse action(FlowActionRequest request);                    // 执行动作
    void revoke(FlowRevokeRequest request);                              // 撤销
    void delete(FlowDeleteRequest request);                              // 删除（仅未流转实例）
    void urge(FlowUrgeRequest request);                                  // 催办
}
```

### 发起流程

```java
FlowCreateRequest request = new FlowCreateRequest();
request.setWorkCode("leave_flow");
request.setOperatorId(1001L);
request.setActionId(startSaveActionId);
request.setFormData(Map.of("days", 3));
request.setOperatorSelectMap(Map.of(approvalNodeId, List.of(1002L)));
long recordId = flowService.create(request);
```

### 执行动作

```java
FlowActionRequest request = new FlowActionRequest();
request.setRecordId(recordId);
request.setOperatorId(1001L);
request.setFormData(Map.of("days", 3));

FlowAdviceBody advice = new FlowAdviceBody();
advice.setActionId(passActionId);
advice.setAdvice("同意");
advice.setSignKey("signature");
request.setAdvice(advice);

ActionResponse response = flowService.action(request);
```

## 2. 流程设计服务：`WorkflowService`

`flow-engine-framework/.../service/WorkflowService.java`，由 starter 注册为 Bean：

```java
void saveWorkflowVersion(WorkflowVersion currentVersion, boolean creatable, boolean enable);
WorkflowRuntime getWorkflowRuntime(long runtimeId);
Workflow getWorkflowByCode(String workCode);
Workflow getWorkflowById(String workId);
void deleteVersion(long versionId);
void changeVersion(long versionId);
void updateVersionName(long versionId, String versionName);
void delete(String workId);
void saveWorkflow(Workflow workflow);
void saveWorkflow(Workflow workflow, boolean enable);
void saveWorkflowRuntime(WorkflowRuntime workflowRuntime);
WorkflowRuntime getOrCreateWorkflowRuntime(Workflow workflow);   // @Transactional(REQUIRES_NEW) + lockById
WorkflowRuntime getWorkflowRuntime(String workId, long workVersion);
String importWorkflow(String body, IFlowOperator createOperator);  // 导入流程 JSON，返回 workId
```

- `saveWorkflowVersion(version, creatable, enable)`：保存版本（`creatable=true` 时创建新版本并切为当前；`enable` 控制是否启用），内部会固化脚本（`WorkflowGroovyScriptUtils.saveScripts`）。
- `getOrCreateWorkflowRuntime(workflow)`：获取/创建运行时快照。多实例部署下依赖 `WorkflowRepository.lockById` 行锁保证串行。

## 3. 流程记录服务：`FlowRecordService`

`flow-engine-framework/.../service/FlowRecordService.java`，由 starter 注册为 Bean：

```java
void saveFlowRecords(List<FlowRecord> flowRecords);
void saveFlowRecord(FlowRecord flowRecord);
List<FlowRecord> getMergeRecord(String mergeKey);
FlowRecord getFlowRecord(long id);
List<FlowRecord> findFlowRecordByIds(List<Long> list);
List<FlowRecord> findFlowRecordBeforeRecords(String processId, long recordId);
List<FlowRecord> findFlowRecordByProcessId(String processId);
List<FlowRecord> findFlowRecordAfterRecords(String processId, long recordId);
List<FlowRecord> findFlowRecordTodoRecords(String processId);
List<FlowRecord> findFlowRecordCurrentNodeRecords(long fromId, String nodeId);
void deleteFlowRecord(FlowRecord flowRecord);   // 按 合并关系 → 待办 → 流程记录 顺序清理
```

> 写入记录应使用 `FlowRecordSaveService`（服务编排三段式：records → todoMarge → removeTodoMerge），保证三类记录一致。

## 4. 查询服务：`FlowRecordQueryService`

```java
Page<FlowRecordContent> findAll(PageRequest request);                       // 全部
Page<FlowRecordContent> findTodoRecordPage(long userId, PageRequest request);   // 待办
Page<FlowRecordContent> findNotifyRecordPage(long userId, PageRequest request); // 抄送
Page<FlowRecordContent> findDoneRecordPage(long userId, PageRequest request);   // 已办
```

生产实现由 `flow-engine-starter-query` 提供（JPA）；Mock 由 `FlowRecordQueryMockService` 提供。

## 5. 构建流程设计

### 5.1 `WorkflowBuilder`

```java
Workflow workflow = WorkflowBuilder.builder()
        .title("请假审批")
        .code("leave_flow")
        .description("...")
        .form(form)
        .createdOperator(operator)
        .operatorCreateScript(scriptKeyOrContent)   // 可选 OperatorMatchScript
        .strategies(List.of(new InterfereStrategy(), new UrgeStrategy()))
        .maxNestDepth(10)                            // 子流程嵌套深度上限
        .addNode(startNode)
        .addNode(approvalNode)
        .addNode(endNode)
        .build();                                    // build() = build(true)：enable + verify
```

### 5.2 `FlowFormBuilder`

```java
FlowForm form = FlowFormBuilder.builder()
        .name("请假申请单")
        .code("leave_form")
        .addField("请假天数", "days", DataType.INTEGER)
        .addField("请假原因", "reason", DataType.STRING)
        .addField(FormField)                          // 或完整 FormField
        .addSubForm(subForm)                          // 子表单
        .build();
```

`DataType` 枚举：`STRING, LONG, INTEGER, DOUBLE, BOOLEAN`。

### 5.3 节点构建

每个节点类提供 `defaultNode()`（默认实例）、`builder()`、（反序列化 `formMap(Map)`）：

```java
StartNode start = StartNode.defaultNode();
ApprovalNode approval = ApprovalNode.builder()
        .name("部门经理审批")
        .order(1)
        .strategies(NodeStrategyBuilder.builder()
                .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy(...))  // 发起人设定
                .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0))
                .build())
        .build();
```

策略构建辅助：

```java
NodeStrategyBuilder.builder().addStrategy(strategy).build();       // 节点策略列表
ActionBuilder.builder().addAction(action).build();                 // 动作列表
WorkflowStrategyBuilder.builder().addStrategy(strategy).build();   // 流程策略列表
FormFieldPermissionsBuilder.builder()
        .addPermission("leave_form", "days", PermissionType.READ)
        .build();
```

### 5.4 保存与启用

```java
workflowService.saveWorkflow(workflow);   // 保存并启用（enable=true）+ 校验
workflowService.saveWorkflowVersion(new WorkflowVersion(workflow), true, true);  // 带版本
```

## 6. 运行时快照（`WorkflowRuntime`）

- `WorkflowRuntime(Workflow)` 构造时固化流程 JSON（`workflow.toJson()`）与全部脚本内容（`snapshotScripts()`）。
- 发起流程时引擎自动 `getOrCreateWorkflowRuntime`，**设计期修改不影响在途流程**。
- 业务方可直接调用 `workflowService.getWorkflowRuntime(runtimeId)` / `getWorkflowRuntime(workId, workVersion)` 获取。

## 7. 会话对象（`FlowSession`）

引擎内部以不可变 `FlowSession` 传递执行上下文，业务方在脚本/事件处理中可读取：

```java
session.getCurrentOperator()      // 当前审批人
session.getCreatedOperator()      // 流程创建者
session.getSubmitOperator()       // 提交人
session.getWorkflow()             // 流程设计
session.getCurrentNode()          // 当前节点
session.getCurrentAction()        // 当前动作
session.getCurrentRecord()        // 当前记录
session.getFormData("fieldCode")  // 表单字段值
session.isMock()                  // 是否 Mock
session.matchNextNodes()          // 匹配的下一节点列表
session.updateSession(node)       // 不可变式切换会话（内部流转用）
```