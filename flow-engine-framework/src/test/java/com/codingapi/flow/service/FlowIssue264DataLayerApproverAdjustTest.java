package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #264 验证：**跳过服务层编排**，直接对仓储（等价于直接写
 * {@code t_flow_record} / {@code t_flow_todo_record} 两张表）增删审批人，
 * 观察引擎的待办列表、审批权限与节点完成判定是否按预期变化。
 *
 * <p>本类只做"机制可行性"验证，不代表推荐做法。用到的数据层操作与 SQL 的对应关系：</p>
 * <ul>
 *     <li>{@link #insertRecordRow} —— {@code INSERT INTO t_flow_record(...)}</li>
 *     <li>{@link #insertTodoRow} —— {@code INSERT INTO t_flow_todo_record(...)}</li>
 *     <li>{@link #revokeRecordRow} —— {@code UPDATE t_flow_record SET revoked = 1 WHERE id = ?}</li>
 *     <li>{@link #deleteTodoRow} —— {@code DELETE FROM t_flow_todo_record WHERE recordId = ?}</li>
 * </ul>
 *
 * <p>所有用例都刻意<strong>不</strong>调用 {@code AddAuditAction} / {@code TransferAction} 等官方动作，
 * 也不触发 {@code EventPusher}，以复现"运维手工改库"的真实路径。</p>
 *
 * <p>流程结构：A(发起) → B(多人审批) → C(c1 单人审批) → D(结束)。</p>
 */
class FlowIssue264DataLayerApproverAdjustTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private final User creator = new User(1, "user");
    private final User b1 = new User(2, "b1");
    private final User b2 = new User(3, "b2");
    private final User b3 = new User(4, "b3");
    private final User c1 = new User(5, "c1");

    // ==================== 数据层操作（等价于直接写库） ====================

    /**
     * 等价于 {@code INSERT INTO t_flow_record}：复制同节点实例的一条记录行，替换审批人与审批序号。
     *
     * @param template   同 {@code (fromId, nodeId)} 分组的模板记录
     * @param operator   新的审批人
     * @param nodeOrder  多人审批的顺序槽位
     * @param hidden     是否隐藏（SEQUENCE 下后序人员为 true）
     * @return 尚未入库的记录对象（{@code id = 0}，由仓储分配，模拟数据库序列发号）
     */
    private FlowRecord newRecordRow(FlowRecord template, User operator, int nodeOrder, boolean hidden) {
        WorkflowRuntime runtime = factory.workflowService.getWorkflowRuntime(template.getWorkRuntimeId());
        assertNotNull(runtime, "流程运行时必须存在");
        FlowRuntimeScriptLocalCache.getInstance().set(runtime.getScripts());

        Workflow workflow = runtime.toWorkflow();
        IFlowNode node = workflow.getFlowNode(template.getNodeId());
        IFlowAction action = node.actionManager().getActions().get(0);

        FormData formData = new FormData(workflow.getForm());
        formData.reset(template.getFormData());

        FlowSession session = new FlowSession(factory.repositoryHolder,
                operator,
                factory.userGateway.get(template.getCreateOperatorId()),
                factory.userGateway.get(template.getSubmitOperatorId()),
                workflow,
                node,
                action,
                formData,
                template,
                List.of(),
                template.getWorkRuntimeId(),
                FlowAdvice.nullFlowAdvice());

        FlowRecord record = template.create(session);
        // 加入同一节点实例分组（fromId 相同），并指定顺序槽位与隐藏标记
        record.resetAddAudit(template.getFromId(), nodeOrder, operator.getUserId(), hidden);
        record.cleanAction();
        record.setId(0);
        return record;
    }

    /** 等价于 {@code INSERT INTO t_flow_record}。 */
    private FlowRecord insertRecordRow(FlowRecord template, User operator, int nodeOrder, boolean hidden) {
        FlowRecord record = newRecordRow(template, operator, nodeOrder, hidden);
        factory.flowRecordRepository.save(record);
        return record;
    }

    /** 等价于 {@code INSERT INTO t_flow_todo_record}；合并模式还需配套写 {@code t_flow_todo_marge}。 */
    private FlowTodoRecord insertTodoRow(FlowRecord record) {
        FlowTodoRecord todo = new FlowTodoRecord(record);
        factory.flowTodoRecordRepository.save(todo);
        return todo;
    }

    /** 等价于 {@code UPDATE t_flow_record SET revoked = 1}。 */
    private void revokeRecordRow(FlowRecord record) {
        record.revoke();
        factory.flowRecordRepository.save(record);
    }

    /** 等价于 {@code DELETE FROM t_flow_todo_record WHERE recordId = ?}。 */
    private void deleteTodoRow(long recordId) {
        FlowTodoRecord todo = factory.flowTodoRecordRepository.getByTodoKey(String.valueOf(recordId));
        if (todo != null) {
            factory.flowTodoRecordRepository.delete(todo);
        }
    }

    // ==================== 用例 ====================

    /**
     * 场景 1：只插入待办行（{@code t_flow_todo_record}），不插入流程记录行。
     *
     * <p>期望验证：待办列表只按 {@code currentOperatorId} 过滤，因此列表能被"看到"，
     * 但审批时会因 {@code t_flow_record} 中不存在该 id 直接失败。</p>
     */
    @Test
    void insertTodoRowOnly_visibleInTodoListButCannotApprove() {
        Workflow workflow = saveWorkflow("[2]");
        startAndSubmit(workflow);

        FlowRecord b1Todo = onlyTodo(b1);
        // 伪造一条指向不存在记录（id=9999）的待办行
        FlowRecord ghost = newRecordRow(b1Todo, b3, b1Todo.getNodeOrder(), false);
        ghost.setId(9999L);
        insertTodoRow(ghost);

        // 待办列表可见（只查 currentOperatorId）
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(b3.getUserId()).size(),
                "只插待办行，待办列表即可见");
        // 流程记录不存在
        assertTrue(factory.flowRecordRepository.findTodoByOperator(b3.getUserId()).isEmpty(),
                "未插流程记录时，b3 没有真实的待办记录");
        // 点击审批直接失败（记录不存在）
        String bActionId = actionIdOf(b1Todo);
        assertThrows(FlowNotFoundException.class,
                () -> approve(b3, 9999L, bActionId),
                "只插待办行无法审批，必须同时存在 t_flow_record 行");
    }

    /**
     * 场景 2：只插入流程记录行，不插入待办行。
     *
     * <p>期望验证：待办列表中看不到（列表只读 {@code t_flow_todo_record}），
     * 但只要拿到 recordId，仍然可以正常审批并推进流程。</p>
     */
    @Test
    void insertRecordRowOnly_approvableWithoutTodoRow() {
        Workflow workflow = saveWorkflow("[2]");
        startAndSubmit(workflow);

        FlowRecord b1Todo = onlyTodo(b1);
        FlowRecord inserted = insertRecordRow(b1Todo, b3, b1Todo.getNodeOrder(), false);

        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(b3.getUserId()).isEmpty(),
                "未插待办行，b3 的待办列表为空");
        assertTrue(factory.flowRecordRepository.get(inserted.getId()).isTodo(),
                "流程记录本身是待办状态");

        // 拿到 recordId 即可审批成功，并推进到 C 节点
        approve(b3, inserted.getId());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).size(),
                "只插流程记录行也能完成审批并推进流程");
    }

    /**
     * 场景 3：SEQUENCE（循环审批）运行期加人。
     *
     * <p>步骤：b1 通过后，数据层为 b3 插入一条 {@code nodeOrder = maxNodeOrder + 1} 的隐藏记录 + 待办行。</p>
     *
     * <p>期望验证：</p>
     * <ul>
     *     <li>待办行不看 {@code hidden}，b3 会在待办列表看到但它无法审批（{@code isTodo() == false}）；</li>
     *     <li>b2 通过后节点<strong>不会</strong>完成（{@code maxNodeOrder} 已变为 2），且引擎自动激活 b3；</li>
     *     <li>b3 通过后流程才流转到 C —— 说明数据层加人确实影响完成判定，SEQUENCE 加人可闭环。</li>
     * </ul>
     */
    @Test
    void sequenceInsertApproverByDataLayer_blocksCompletionUntilNewApproverPasses() {
        Workflow workflow = saveWorkflow("[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE, 0));
        startAndSubmit(workflow);

        FlowRecord b1Todo = onlyTodo(b1);
        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(b2.getUserId()).isEmpty(),
                "SEQUENCE 下 b2 初始为隐藏，无待办行");

        // b1 通过 -> 激活 b2
        approve(b1, b1Todo.getId());
        FlowRecord b2Todo = onlyTodo(b2);

        // 数据层加人：b3 追加到末位（hidden = true，与引擎 SEQUENCE 生成规则一致）
        FlowRecord b3Row = insertRecordRow(b2Todo, b3, b2Todo.getNodeOrder() + 1, true);
        insertTodoRow(b3Row);

        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(b3.getUserId()).size(),
                "待办列表不看 hidden，b3 会看到待办");
        assertTrue(!factory.flowRecordRepository.get(b3Row.getId()).isTodo(),
                "但隐藏记录 isTodo() 为 false");
        assertThrows(FlowStateException.class, () -> approve(b3, b3Row.getId()),
                "隐藏记录不能直接审批");

        // b2 通过：maxNodeOrder 已变为 2，节点不应完成；同时引擎按 nodeOrder + 1 激活 b3
        approve(b2, b2Todo.getId());
        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).isEmpty(),
                "数据层加人后，b2 通过不能完成 B 节点");
        assertTrue(factory.flowRecordRepository.get(b3Row.getId()).isTodo(),
                "引擎的 SEQUENCE 激活逻辑会把 b3 置为可见待办");

        // b3 通过 -> 节点达到最大序号，流程流转到 C
        approve(b3, b3Row.getId());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).size(),
                "b3 通过后流程才流转到 C 节点");
    }

    /**
     * 场景 4：MERGE（并签，比例 1.0）运行期加人。
     *
     * <p>期望验证：加人后 {@code total}（不同 nodeOrder 数）变大，比例分母变化，
     * 原本 2/2 才完成的判定变成 2/3 不完成，需 b3 也通过才流转。</p>
     */
    @Test
    void mergeInsertApproverByDataLayer_changesPercentDenominator() {
        Workflow workflow = saveWorkflow("[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        startAndSubmit(workflow);

        FlowRecord b1Todo = onlyTodo(b1);
        FlowRecord b2Todo = onlyTodo(b2);

        approve(b1, b1Todo.getId());

        // 数据层加人：并签第 3 个席位（可见）
        FlowRecord b3Row = insertRecordRow(b2Todo, b3, 2, false);
        insertTodoRow(b3Row);

        // b2 通过：2/3 < 1.0，节点不完成
        approve(b2, b2Todo.getId());
        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).isEmpty(),
                "加人后分母变为 3，2 人通过不足以完成并签节点");

        // b3 通过：3/3 完成
        approve(b3, b3Row.getId());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).size(),
                "b3 通过后并签比例达成，流程流转到 C");
    }

    /**
     * 场景 5：SEQUENCE 运行期"减人"，删除中间审批人且<strong>不修复</strong>激活链。
     *
     * <p>期望验证：b1 通过后，引擎按"最近一条更大 {@code nodeOrder}"查找下一位待审批人
     * （issue #266 的激活链路修复），被撤销的 b2 已被 {@code revoked} 过滤，
     * b3 会被自动激活并产生待办 —— 数据层删除中间审批人不再需要手工维护 hidden 与待办行。</p>
     */
    @Test
    void sequenceRevokeMiddleApproverWithoutFix_activatesNextApprover() {
        Workflow workflow = saveWorkflow("[2,3,4]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE, 0));
        startAndSubmit(workflow);

        FlowRecord b1Todo = onlyTodo(b1);

        // 数据层减人：把 nodeOrder = 1 的 b2 记录撤销
        FlowRecord b2Record = bNodeRecords(b1Todo).stream()
                .filter(r -> r.getCurrentOperatorId() == b2.getUserId())
                .findFirst().orElseThrow();
        revokeRecordRow(b2Record);
        deleteTodoRow(b2Record.getId());

        // b1 通过 -> 跳过被撤销的 nodeOrder = 1，直接激活 nodeOrder = 2 的 b3
        approve(b1, b1Todo.getId());

        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(b3.getUserId()).size(),
                "按最近一条更大 nodeOrder 激活，b3 应获得待办");
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(b3.getUserId()).size(),
                "b3 的记录应被激活为有效待办");
        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).isEmpty(),
                "B 节点尚未完成，不应产生 C 节点待办");

        FlowRecord b3Record = bNodeRecords(b1Todo).stream()
                .filter(r -> r.getCurrentOperatorId() == b3.getUserId())
                .findFirst().orElseThrow();
        assertTrue(factory.flowRecordRepository.get(b3Record.getId()).isTodo(), "b3 已被激活");

        // b3 通过 -> 节点完成，流转到 C
        approve(b3, b3Record.getId());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).size(),
                "跳过被撤销的中间审批人后流程可以继续");
    }

    /**
     * 场景 6：SEQUENCE 运行期"减人" + <strong>手工修复</strong>激活链。
     *
     * <p>期望验证：撤销中间人后，只要把下一位 {@code hidden = false} 并补一条待办行，
     * 流程即可继续（b3 通过后 B 节点完成）。说明数据层减人可行，但必须人工维护 hidden 与待办。</p>
     *
     * <p>说明：issue #266 把激活条件改为"最近一条更大 {@code nodeOrder}"后，这一步手工修复
     * 已不再必需（见场景 5）；本用例保留为"运维坚持手工修链"路径的验证。</p>
     */
    @Test
    void sequenceRevokeMiddleApproverWithHiddenFix_proceeds() {
        Workflow workflow = saveWorkflow("[2,3,4]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE, 0));
        startAndSubmit(workflow);

        FlowRecord b1Todo = onlyTodo(b1);

        FlowRecord b2Record = bNodeRecords(b1Todo).stream()
                .filter(r -> r.getCurrentOperatorId() == b2.getUserId())
                .findFirst().orElseThrow();
        FlowRecord b3Record = bNodeRecords(b1Todo).stream()
                .filter(r -> r.getCurrentOperatorId() == b3.getUserId())
                .findFirst().orElseThrow();

        // 数据层减人 + 修复：撤销 b2，并手动激活 b3（UPDATE hidden = 0 + INSERT 待办行）
        revokeRecordRow(b2Record);
        deleteTodoRow(b2Record.getId());
        b3Record.show();
        factory.flowRecordRepository.save(b3Record);
        insertTodoRow(b3Record);

        // b1 通过 -> 节点未完成（maxNodeOrder = 2），b3 可直接审批
        approve(b1, b1Todo.getId());
        assertTrue(factory.flowRecordRepository.get(b3Record.getId()).isTodo(),
                "手工激活后 b3 成为有效待办");
        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).isEmpty(),
                "节点尚未完成，不应产生 C 节点待办");

        // b3 通过 -> maxNodeOrder 达到，节点完成
        approve(b3, b3Record.getId());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).size(),
                "减人 + 修复激活链后流程可以继续");
    }

    /**
     * 场景 7（风险）：MERGE 运行期"减人"，撤销最后一个未审批人。
     *
     * <p>期望验证：节点完成判定只在"有人提交审批动作"时触发，
     * 撤销最后一个待审批人后没有任何触发点，流程永久停滞在 B 节点。</p>
     */
    @Test
    void mergeRevokeLastApprover_stallsFlowForever() {
        Workflow workflow = saveWorkflow("[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        startAndSubmit(workflow);

        FlowRecord b1Todo = onlyTodo(b1);
        FlowRecord b2Todo = onlyTodo(b2);

        // b1 通过，1/2 未达比例
        approve(b1, b1Todo.getId());

        // 数据层减人：撤销 b2（最后一个待审批人）
        revokeRecordRow(b2Todo);
        deleteTodoRow(b2Todo.getId());

        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(b2.getUserId()).isEmpty(),
                "b2 的待办已消失");
        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).isEmpty(),
                "C 节点永远不会产生待办");
        // 流程记录全部停留在运行中，且 B 节点再无待办
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(b2Todo.getProcessId());
        assertTrue(records.stream().noneMatch(FlowRecord::isTodo),
                "整个流程已没有任何待办，但流程也没有结束");
        assertTrue(records.stream().noneMatch(FlowRecord::isFinish),
                "流程永久停留在 B 节点（无触发点重算完成判定）");
    }

    /**
     * 场景 8：只删待办行、不撤销流程记录（"假减人"）。
     *
     * <p>期望验证：待办列表里看不到了，但记录仍是待办，只要拿到 recordId 依旧能审批成功，
     * 甚至因此推进流程 —— 只删 {@code t_flow_todo_record} 无法真正减少审批人。</p>
     */
    @Test
    void deleteTodoRowOnly_cannotReallyRemoveApprover() {
        Workflow workflow = saveWorkflow("[2,3]",
                new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.MERGE, 1.0f));
        startAndSubmit(workflow);

        FlowRecord b1Todo = onlyTodo(b1);
        FlowRecord b2Todo = onlyTodo(b2);

        approve(b1, b1Todo.getId());

        // 数据层"假减人"：只删待办行
        deleteTodoRow(b2Todo.getId());
        assertTrue(factory.flowTodoRecordRepository.findByOperatorId(b2.getUserId()).isEmpty(),
                "b2 的待办列表已看不到");

        // 但记录仍可审批，并因此完成并签节点
        approve(b2, b2Todo.getId());
        assertEquals(1, factory.flowTodoRecordRepository.findByOperatorId(c1.getUserId()).size(),
                "只删待办行不能阻止该记录继续审批并推进流程");
    }

    // ==================== 辅助方法 ====================

    private Workflow saveWorkflow(String operatorIds) {
        return saveWorkflow(operatorIds, null);
    }

    private Workflow saveWorkflow(String operatorIds, MultiOperatorAuditStrategy auditStrategy) {
        for (User user : List.of(creator, b1, b2, b3, c1)) {
            factory.userGateway.save(user);
        }
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("issue264流程")
                .code("issue264")
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        NodeStrategyBuilder bStrategy = NodeStrategyBuilder.builder()
                .addStrategy(permission())
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return " + operatorIds + "}").getKey()));
        if (auditStrategy != null) {
            bStrategy.addStrategy(auditStrategy);
        }
        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(bStrategy.build())
                .build();

        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(permission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [5]}").getKey()))
                        .build())
                .build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("issue264流程")
                .code("issue264")
                .createdOperator(creator)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(EndNode.builder().build())
                .build();
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    private FormFieldPermissionStrategy permission() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission("issue264", "name", PermissionType.WRITE)
                .build());
    }

    private void startAndSubmit(Workflow workflow) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction startAction = startNode.actionManager().getActions().get(0);

        Map<String, Object> data = Map.of("name", "lorne");
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(creator.getUserId());
        long recordId = factory.flowService.create(createRequest);

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startAction.id(), "同意", creator.getUserId()));
        factory.flowService.action(actionRequest);
    }

    private void approve(User operator, long recordId) {
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        IFlowNode node = factory.workflowService
                .getWorkflowRuntime(record.getWorkRuntimeId())
                .toWorkflow()
                .getFlowNode(record.getNodeId());
        approve(operator, recordId, node.actionManager().getActions().get(0).id());
    }

    /** 记录可能不存在（待办行指向脏数据）时的审批调用：动作 id 由调用方提供。 */
    private void approve(User operator, long recordId, String actionId) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(Map.of("name", "lorne"));
        request.setRecordId(recordId);
        request.setAdvice(new FlowAdviceBody(actionId, "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowRecord onlyTodo(User user) {
        List<FlowRecord> todos = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, todos.size(), user.getName() + " 应仅有 1 条待办");
        return todos.get(0);
    }

    private String actionIdOf(FlowRecord record) {
        return factory.workflowService
                .getWorkflowRuntime(record.getWorkRuntimeId())
                .toWorkflow()
                .getFlowNode(record.getNodeId())
                .actionManager().getActions().get(0).id();
    }

    /** 按 (fromId, nodeId) 分组查询 B 节点记录：等价于 {@code select * from t_flow_record where fromId=? and nodeId=? and revoked=0}。 */
    private List<FlowRecord> bNodeRecords(FlowRecord anyBRecord) {
        return factory.flowRecordRepository.findCurrentNodeRecords(anyBRecord.getFromId(), anyBRecord.getNodeId());
    }
}
