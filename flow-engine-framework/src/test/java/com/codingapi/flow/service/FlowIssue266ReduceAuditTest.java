package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.FlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #266：多人审批减签能力（合并 #264 / #265）。
 *
 * <p>流程 A-B-C，B 为多人审批节点。验证点：
 * <ul>
 *     <li>并签（MERGE）：减签后目标审批人的待办、合并待办组与合并关系一并清理，剩余审批人通过后节点完成；</li>
 *     <li>顺序审批（SEQUENCE）：减签尚未激活的审批人后，通过时仍能激活「下一位」审批人（序号空洞不应导致链断裂）；</li>
 *     <li>或签 / 随机一人：减签没有业务意义，直接拒绝；</li>
 *     <li>边界：不能减签自己；减签后节点仍必须保留至少一次待审批（并签比例已达成时拒绝）。</li>
 * </ul>
 */
class FlowIssue266ReduceAuditTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private final User user = new User(1, "user");
    private final User b1 = new User(2, "b1");
    private final User b2 = new User(3, "b2");
    private final User b3 = new User(4, "b3");
    private final User b4 = new User(5, "b4");
    private final User b5 = new User(6, "b5");

    @Test
    void mergeAuditReduce_shouldClearTargetTodoAndFinishAfterRemainingPassed() {
        // given A(开始) - B(并签 + 合并，b1/b2) - C(结束)
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.MERGE, 1f, List.of(2L, 3L), true);
        String bNodeId = nodeIdByName(workflow, "B审批");
        long startRecordId = createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);
        FlowRecord b2Record = pendingRecordOf(b2, b1Record);
        assertEquals(1, groupsOf(b2, bNodeId).size(), "减签前 b2 应有合并待办组");

        // when b1 减签 b2
        reduce(workflow, b1, b1Record, b2.getUserId());

        // then b2 的记录被撤销，待办、合并待办组与合并关系一并清理，b1 的合并组保留
        assertTrue(record(b2Record.getId()).isRevoked(), "b2 的记录应被撤销");
        assertNoTodo(b2, bNodeId);
        assertEquals(0, groupsOf(b2, bNodeId).size(), "b2 的合并待办组应被删除");
        assertEquals(1, factory.flowTodoMergeRepository.findAll().size(), "只应保留 b1 的合并关系");
        FlowTodoRecord b1Group = singleGroupOf(b1, bNodeId);
        assertEquals(1, b1Group.getMargeCount(), "b1 的合并组应保留 1 条记录");
        assertEquals(1, todoRecordsOf(b1, bNodeId).size(), "b1 的待办应保留");
        assertFalse(record(startRecordId).isFinish(), "减签不应推进流程");

        // when b1 通过
        pass(todoRecordOf(b1, bNodeId), workflow, b1);

        // then 流程结束，待办与合并关系清理干净
        assertNoTodo(b1, bNodeId);
        assertEquals(0, factory.flowTodoMergeRepository.findAll().size(), "不应残留合并关系");
        assertTrue(record(startRecordId).isFinish(), "剩余审批人通过后流程应结束");
    }

    @Test
    void sequenceAuditReduceHiddenApprover_shouldStillActivateNextApprover() {
        // given A(开始) - B(顺序审批，b1/b2/b3) - C(结束)
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.SEQUENCE, 0f, List.of(2L, 3L, 4L), false);
        String bNodeId = nodeIdByName(workflow, "B审批");
        long startRecordId = createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);
        FlowRecord b2Record = pendingRecordOf(b2, b1Record);
        assertEquals(0, todoRecordsOf(b2, bNodeId).size(), "b2 未被激活前不应有 todo");

        // when b1 减签尚未激活的 b2
        reduce(workflow, b1, b1Record, b2.getUserId());

        // then b2 记录撤销且不产生待办，b1 仍是唯一待办
        assertTrue(record(b2Record.getId()).isRevoked(), "b2 的记录应被撤销");
        assertEquals(0, todoRecordsOf(b2, bNodeId).size());
        assertEquals(1, todoRecordsOf(b1, bNodeId).size());

        // when b1 通过（序号出现空洞，仍应激活 b3）
        pass(todoRecordOf(b1, bNodeId), workflow, b1);

        // then b3 被激活，流程未结束
        assertEquals(0, todoRecordsOf(b2, bNodeId).size(), "被减签的 b2 不应被激活");
        assertEquals(1, todoRecordsOf(b3, bNodeId).size(), "序号空洞不应阻断后续审批人激活");
        assertFalse(record(startRecordId).isFinish(), "b3 未审批时流程不应结束");

        // when b3 通过
        pass(todoRecordOf(b3, bNodeId), workflow, b3);

        // then 流程结束
        assertTrue(record(startRecordId).isFinish(), "最后一位审批人通过后流程应结束");
    }

    @Test
    void sequenceAuditReduceAllFollowingApprovers_shouldFinishAfterCurrentPassed() {
        // given A(开始) - B(顺序审批，b1/b2/b3) - C(结束)
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.SEQUENCE, 0f, List.of(2L, 3L, 4L), false);
        String bNodeId = nodeIdByName(workflow, "B审批");
        long startRecordId = createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);

        // when b1 一次减签后续所有审批人
        reduce(workflow, b1, b1Record, b2.getUserId(), b3.getUserId());

        // then 仅剩 b1 待办，b2/b3 无待办
        assertEquals(1, todoRecordsOf(b1, bNodeId).size());
        assertEquals(0, todoRecordsOf(b2, bNodeId).size());
        assertEquals(0, todoRecordsOf(b3, bNodeId).size());

        // when b1 通过
        pass(todoRecordOf(b1, bNodeId), workflow, b1);

        // then 流程结束
        assertTrue(record(startRecordId).isFinish(), "仅剩的审批人通过后流程应结束");
    }

    @Test
    void addAuditThenReduce_shouldRemoveAddedApprover() {
        // given A(开始) - B(并签 + 合并，b1/b2) - C(结束)
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.MERGE, 1f, List.of(2L, 3L), true);
        String bNodeId = nodeIdByName(workflow, "B审批");
        long startRecordId = createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);

        // when b1 加签 b3，再减签 b3
        addAudit(workflow, b1, b1Record, b3.getUserId());
        assertFalse(todoRecordsOf(b3, bNodeId).isEmpty(), "加签后 b3 应有待办");

        reduce(workflow, b1, todoRecordOf(b1, bNodeId), b3.getUserId());

        // then 加签记录被撤销，待办清理，节点回到 b1/b2 并签
        assertNoTodo(b3, bNodeId);
        assertEquals(1, todoRecordsOf(b1, bNodeId).size());
        assertEquals(1, todoRecordsOf(b2, bNodeId).size());
        assertFalse(record(startRecordId).isFinish());
    }

    @Test
    void reduceInAnyAudit_shouldReject() {
        // given 或签节点
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.ANY, 0f, List.of(2L, 3L), false);
        String bNodeId = nodeIdByName(workflow, "B审批");
        createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);
        FlowRecord b2Record = pendingRecordOf(b2, b1Record);

        // when/then 或签减签没有业务意义，直接拒绝
        assertThrows(FlowExecutionException.class,
                () -> reduce(workflow, b1, b1Record, b2.getUserId()));
        assertFalse(record(b2Record.getId()).isRevoked(), "被拒绝的减签不应产生撤销");
    }

    @Test
    void reduceInRandomOneAudit_shouldReject() {
        // given 任意一人审批节点
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.RANDOM_ONE, 0f, List.of(2L, 3L), false);
        String bNodeId = nodeIdByName(workflow, "B审批");
        long startRecordId = createAndSubmit(workflow);

        // 随机一人只落库一条记录，执行人不一定是 b1
        List<FlowRecord> nodeRecords = nodeRecordsOf(startRecordId, bNodeId);
        assertEquals(1, nodeRecords.size(), "随机一人审批只应保留 1 条记录");
        FlowRecord currentRecord = nodeRecords.get(0);
        User executor = currentRecord.getCurrentOperatorId() == b1.getUserId() ? b1 : b2;
        long targetOperatorId = executor.getUserId() == b1.getUserId() ? b2.getUserId() : b1.getUserId();

        // when/then 随机一人减签没有业务意义，直接拒绝
        assertThrows(FlowExecutionException.class,
                () -> reduce(workflow, executor, currentRecord, targetOperatorId));
    }

    @Test
    void reduceOwnRecord_shouldReject() {
        // given
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.MERGE, 1f, List.of(2L, 3L), false);
        String bNodeId = nodeIdByName(workflow, "B审批");
        createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);

        // when/then 当前审批人不能减签自己
        assertThrows(FlowExecutionException.class,
                () -> reduce(workflow, b1, b1Record, b1.getUserId()));
        assertEquals(1, todoRecordsOf(b1, bNodeId).size(), "自己的待办应保留");
    }

    @Test
    void mergeAuditReduceWhenPercentAlreadySatisfied_shouldReject() {
        // given 并签比例 50%，5 位审批人：b1、b2 已通过，节点因 2 < 5 * 0.5 尚未完成
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.MERGE, 0.5f, List.of(2L, 3L, 4L, 5L, 6L), false);
        String bNodeId = nodeIdByName(workflow, "B审批");
        long startRecordId = createAndSubmit(workflow);

        pass(todoRecordOf(b1, bNodeId), workflow, b1);
        pass(todoRecordOf(b2, bNodeId), workflow, b2);
        assertFalse(record(startRecordId).isFinish(), "2/5 未达到 50%，节点不应完成");

        FlowRecord b3Record = todoRecordOf(b3, bNodeId);

        // when/then 再减掉 b4、b5 会让比例立刻达成（2/3），没有触发点，必须拒绝
        assertThrows(FlowExecutionException.class,
                () -> reduce(workflow, b3, b3Record, b4.getUserId(), b5.getUserId()));
        assertEquals(1, todoRecordsOf(b4, bNodeId).size(), "被拒绝的减签不应清理 b4 的待办");
        assertEquals(1, todoRecordsOf(b5, bNodeId).size(), "被拒绝的减签不应清理 b5 的待办");
    }

    @Test
    void reduceActionShouldBeVisibleOnlyWhenAnotherPendingApproverExists() {
        // given 单人审批节点 + 双人并签节点
        Workflow singleWorkflow = saveWorkflow("issue266-single", MultiOperatorAuditStrategy.Type.SEQUENCE,
                0f, List.of(2L), true);
        createAndSubmit(singleWorkflow);
        String singleNodeId = nodeIdByName(singleWorkflow, "B审批");

        Workflow mergeWorkflow = saveWorkflow("issue266-merge", MultiOperatorAuditStrategy.Type.MERGE,
                1f, List.of(2L, 3L), true);
        createAndSubmit(mergeWorkflow);
        String mergeNodeId = nodeIdByName(mergeWorkflow, "B审批");

        // when/then 单人节点不展示减签按钮
        FlowContent singleDetail = factory.flowService.detail(
                new FlowDetailRequest(todoRecordOf(b1, singleNodeId).getId(), b1.getUserId()));
        assertFalse(containsAction(singleDetail.getActions(), ActionType.REDUCE_AUDIT),
                "单人审批节点不应展示减签按钮");

        // 多人节点展示减签按钮
        FlowContent mergeDetail = factory.flowService.detail(
                new FlowDetailRequest(todoRecordOf(b1, mergeNodeId).getId(), b1.getUserId()));
        assertTrue(containsAction(mergeDetail.getActions(), ActionType.REDUCE_AUDIT),
                "多人并签节点应展示减签按钮");
    }

    /**
     * 测试目标：合并审批时，合并组内每条流程记录都应单独计算减签按钮与候选人。
     * 前置条件：B 为并签节点（b1/b2），开启记录合并，并发起两条流程实例。
     * 执行步骤：分别用 b1 在两条流程里的记录查询 detail。
     * 期望断言：两条记录都展示减签按钮，候选人均只有 b2；前端进入单条流程时即可据此切换按钮。
     */
    @Test
    void mergedDetailShouldExposeReduceActionForEachRecord() {
        // given
        Workflow workflow = saveWorkflow("issue-merge-multi", MultiOperatorAuditStrategy.Type.MERGE,
                1f, List.of(2L, 3L), true);
        String bNodeId = nodeIdByName(workflow, "B审批");
        createAndSubmit(workflow);
        createAndSubmit(workflow);

        List<FlowRecord> b1Todos = todoRecordsOf(b1, bNodeId);
        assertEquals(2, b1Todos.size(), "两条流程的记录应合并到同一个待办分组");

        // when/then
        for (FlowRecord record : b1Todos) {
            FlowContent detail = factory.flowService.detail(new FlowDetailRequest(record.getId(), b1.getUserId()));
            assertTrue(detail.isMergeable(), "合并组内的记录应标记为可合并审批");
            assertTrue(containsAction(detail.getActions(), ActionType.REDUCE_AUDIT),
                    "合并组内每条记录都应展示减签按钮");
            assertEquals(List.of(b2.getUserId()),
                    detail.getCurrentNodeOperators().stream().map(FlowOperator::getId).toList(),
                    "减签候选人应按记录所属流程实例计算");
        }
    }

    /**
     * 测试目标：加签不允许把已经在当前节点待办队列中的人员重复加入。
     * 前置条件：B 为并签节点，b1、b2 均为待审批人。
     * 执行步骤：b1 分别加签 b2（他人）与 b1（自己）。
     * 期望断言：两次都被拒绝（execution.addAudit.operatorAlreadyInTodo），节点记录数与待办均不变化。
     */
    @Test
    void addAuditToExistingPendingApprover_shouldReject() {
        // given
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.MERGE, 1f, List.of(2L, 3L), true);
        String bNodeId = nodeIdByName(workflow, "B审批");
        long startRecordId = createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);

        // when/then 加签已在待办中的他人被拒绝
        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> addAudit(workflow, b1, b1Record, b2.getUserId()));
        assertEquals("execution.addAudit.operatorAlreadyInTodo", exception.getErrCode());
        assertEquals(2, nodeRecordsOf(startRecordId, bNodeId).size(), "被拒绝的加签不应新增记录");
        assertEquals(1, todoRecordsOf(b2, bNodeId).size(), "b2 的待办应保持不变");

        // when/then 加签自己同样被拒绝
        FlowExecutionException selfException = assertThrows(FlowExecutionException.class,
                () -> addAudit(workflow, b1, b1Record, b1.getUserId()));
        assertEquals("execution.addAudit.operatorAlreadyInTodo", selfException.getErrCode());
        assertEquals(2, nodeRecordsOf(startRecordId, bNodeId).size(), "被拒绝的加签不应新增记录");
        assertEquals(1, todoRecordsOf(b1, bNodeId).size(), "b1 的待办应保持不变");
    }

    /**
     * 测试目标：当前节点只剩一名待审批人时不允许减签。
     * 前置条件：B 为单人审批节点，待审批人只有 b1。
     * 执行步骤：b1 尝试减签 b2。
     * 期望断言：被拒绝（execution.reduceAudit.singleApprover），b1 的待办保留。
     */
    @Test
    void reduceWhenOnlyOnePendingApprover_shouldReject() {
        // given
        Workflow workflow = saveWorkflow("issue266-single", MultiOperatorAuditStrategy.Type.SEQUENCE,
                0f, List.of(2L), true);
        String bNodeId = nodeIdByName(workflow, "B审批");
        createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);

        // when/then
        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> reduce(workflow, b1, b1Record, b2.getUserId()));
        assertEquals("execution.reduceAudit.singleApprover", exception.getErrCode());
        assertEquals(1, todoRecordsOf(b1, bNodeId).size(), "b1 的待办应保留");
    }

    /**
     * 测试目标：detail 返回当前节点待审批人员，作为减签选人的数据来源。
     * 前置条件：B 为顺序审批节点，b1/b2/b3。
     * 执行步骤：b1 查看 detail；b1 通过后 b2 查看 detail。
     * 期望断言：b1 看到 b2、b3（含尚未激活的隐藏记录，且不含自己）；b2 只看到 b3（已办的 b1 被排除）。
     */
    @Test
    void detailShouldExposeCurrentNodePendingOperators() {
        // given
        Workflow workflow = saveWorkflow(MultiOperatorAuditStrategy.Type.SEQUENCE, 0f, List.of(2L, 3L, 4L), false);
        String bNodeId = nodeIdByName(workflow, "B审批");
        createAndSubmit(workflow);

        FlowRecord b1Record = todoRecordOf(b1, bNodeId);

        // when/then b1 视角：b2、b3 均为当前节点待审批人
        FlowContent b1Detail = factory.flowService.detail(new FlowDetailRequest(b1Record.getId(), b1.getUserId()));
        assertEquals(List.of(b2.getUserId(), b3.getUserId()),
                b1Detail.getCurrentNodeOperators().stream().map(FlowOperator::getId).toList());

        // 动作能力标记：减签不支持合并批量，通过等其他动作支持
        assertEquals(false, actionMap(b1Detail, ActionType.REDUCE_AUDIT).get("batchable"));
        assertEquals(true, actionMap(b1Detail, ActionType.PASS).get("batchable"));

        // when b1 通过后，b2 成为当前审批人
        pass(b1Record, workflow, b1);

        // then b2 视角：只剩 b3，b1 已办不再作为候选人
        FlowContent b2Detail = factory.flowService.detail(
                new FlowDetailRequest(todoRecordOf(b2, bNodeId).getId(), b2.getUserId()));
        assertEquals(List.of(b3.getUserId()),
                b2Detail.getCurrentNodeOperators().stream().map(FlowOperator::getId).toList());
        assertEquals(b3.getName(), b2Detail.getCurrentNodeOperators().get(0).getName());
    }

    // ------------------------------------------------------------------
    // 辅助方法
    // ------------------------------------------------------------------

    private Workflow saveWorkflow(MultiOperatorAuditStrategy.Type type, float percent,
                                  List<Long> approverIds, boolean mergeable) {
        return saveWorkflow("issue266", type, percent, approverIds, mergeable);
    }

    private Workflow saveWorkflow(String code, MultiOperatorAuditStrategy.Type type, float percent,
                                  List<Long> approverIds, boolean mergeable) {
        registerUsers();
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code(code)
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(perm(code))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        String operatorScript = "def run(request){return [" + String.join(",", approverIds.stream().map(String::valueOf).toList()) + "]}";
        NodeStrategyBuilder strategyBuilder = NodeStrategyBuilder.builder()
                .addStrategy(perm(code))
                .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory.createOperatorLoadScript(operatorScript).getKey()))
                .addStrategy(new MultiOperatorAuditStrategy(type, percent));
        if (mergeable) {
            strategyBuilder.addStrategy(new RecordMergeStrategy(true));
        }

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(strategyBuilder.build())
                .build();

        EndNode endNode = EndNode.builder().build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("测试流程")
                .code(code)
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);
        return workflow;
    }

    private FormFieldPermissionStrategy perm(String code) {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(code, "name", PermissionType.WRITE)
                .build());
    }

    /**
     * 发起并提交到 B 节点，返回发起节点记录id
     */
    private long createAndSubmit(Workflow workflow) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction startAction = startNode.actionManager().getActions().get(0);
        Map<String, Object> data = Map.of("name", "lorne");

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(user.getUserId());
        long recordId = factory.flowService.create(createRequest);

        pass(startAction, user, data, recordId);
        return recordId;
    }

    private void registerUsers() {
        List.of(user, b1, b2, b3, b4, b5).forEach(factory.userGateway::save);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    private void pass(FlowRecord flowRecord, Workflow workflow, User operator) {
        pass(actionOf(workflow, ActionType.PASS), operator, Map.of("name", "lorne"), flowRecord.getId());
    }

    private void addAudit(Workflow workflow, User operator, FlowRecord flowRecord, long... targetOperatorIds) {
        execute(workflow, ActionType.ADD_AUDIT, operator, flowRecord, targetOperatorIds);
    }

    private void reduce(Workflow workflow, User operator, FlowRecord flowRecord, long... targetOperatorIds) {
        execute(workflow, ActionType.REDUCE_AUDIT, operator, flowRecord, targetOperatorIds);
    }

    private void execute(Workflow workflow, ActionType actionType, User operator,
                         FlowRecord flowRecord, long... targetOperatorIds) {
        FlowAdviceBody advice = new FlowAdviceBody(actionOf(workflow, actionType).id(), "同意", operator.getUserId());
        advice.setForwardOperatorIds(Arrays.stream(targetOperatorIds).boxed().toList());
        pass(actionOf(workflow, actionType), operator, advice, flowRecord.getId());
    }

    private void pass(IFlowAction action, User operator, Map<String, Object> data, long recordId) {
        pass(action, operator, new FlowAdviceBody(action.id(), "同意", operator.getUserId()), recordId);
    }

    private void pass(IFlowAction action, User operator, FlowAdviceBody advice, long recordId) {
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(Map.of("name", "lorne"));
        request.setRecordId(recordId);
        request.setAdvice(advice);
        factory.flowService.action(request);
    }

    private IFlowAction actionOf(Workflow workflow, ActionType actionType) {
        ApprovalNode approvalNode = (ApprovalNode) workflow.getNodes().stream()
                .filter(node -> node.getName().equals("B审批"))
                .findFirst().orElseThrow();
        IFlowAction action = approvalNode.actionManager().getActionByType(actionType.name());
        assertTrue(action != null, "节点应配置动作 " + actionType.name());
        return action;
    }

    private FlowRecord record(long recordId) {
        return factory.flowRecordRepository.get(recordId);
    }

    private List<FlowRecord> nodeRecordsOf(long startRecordId, String nodeId) {
        String processId = record(startRecordId).getProcessId();
        return factory.flowRecordRepository.findProcessRecords(processId).stream()
                .filter(record -> nodeId.equals(record.getNodeId()))
                .toList();
    }

    private FlowRecord todoRecordOf(User approver, String nodeId) {
        List<FlowRecord> records = todoRecordsOf(approver, nodeId);
        assertEquals(1, records.size(), approver.getName() + " 在 B 节点应有且仅有 1 条待办");
        return records.get(0);
    }

    /**
     * 取指定审批人的待审批记录（含顺序审批中尚未激活的隐藏记录）
     */
    private FlowRecord pendingRecordOf(User approver, FlowRecord anyNodeRecord) {
        return factory.flowRecordRepository
                .findCurrentNodeRecords(anyNodeRecord.getFromId(), anyNodeRecord.getNodeId()).stream()
                .filter(record -> record.getCurrentOperatorId() == approver.getUserId())
                .findFirst()
                .orElseThrow();
    }

    private List<FlowRecord> todoRecordsOf(User approver, String nodeId) {
        return factory.flowRecordRepository.findTodoByOperator(approver.getUserId()).stream()
                .filter(record -> nodeId.equals(record.getNodeId()))
                .toList();
    }

    private List<FlowTodoRecord> groupsOf(User approver, String nodeId) {
        return factory.flowTodoRecordRepository.findByOperatorId(approver.getUserId()).stream()
                .filter(group -> nodeId.equals(group.getNodeId()))
                .toList();
    }

    private FlowTodoRecord singleGroupOf(User approver, String nodeId) {
        List<FlowTodoRecord> groups = groupsOf(approver, nodeId);
        assertEquals(1, groups.size(), approver.getName() + " 在 B 节点应有且仅有 1 条合并待办组");
        return groups.get(0);
    }

    private void assertNoTodo(User approver, String nodeId) {
        assertEquals(0, todoRecordsOf(approver, nodeId).size(),
                approver.getName() + " 在 B 节点不应有 todo 记录");
    }

    private String nodeIdByName(Workflow workflow, String name) {
        return workflow.getNodes().stream()
                .filter(node -> node.getName().equals(name))
                .findFirst().orElseThrow().getId();
    }

    private boolean containsAction(List<Map<String, Object>> actions, ActionType actionType) {
        return actions.stream().anyMatch(action -> actionType.name().equals(action.get("type")));
    }

    private Map<String, Object> actionMap(FlowContent detail, ActionType actionType) {
        return detail.getActions().stream()
                .filter(action -> actionType.name().equals(action.get("type")))
                .findFirst()
                .orElseThrow();
    }
}
