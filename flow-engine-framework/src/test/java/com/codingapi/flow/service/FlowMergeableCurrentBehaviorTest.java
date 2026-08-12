package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 当前合并审批能力（审批人合并）行为基线测试。
 *
 * <p>当前实现中，合并依据 {@link FlowRecord#getTodoKey()} 固定为
 * {@code currentOperatorId-workRuntimeId-nodeId}，即同一审批人在同一节点下的多条
 * 流程记录会合并为一条待办（issue #202 中的"审批人合并"）。</p>
 *
 * <p>这些测试固化当前能力行为，作为后续新增"发起人合并 / 提交人合并"类型支持的基线。</p>
 */
public class FlowMergeableCurrentBehaviorTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    /**
     * 场景：A-B-C 流程，B 节点开启合并，多个发起人提交后按审批人合并为一条待办。
     */
    @Test
    void shouldMergeByApprover() {
        // given
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(user, "mt-merge", "def run(request){return [2]}",
                true, null);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");
        int count = 3;

        // when
        for (int i = 0; i < count; i++) {
            submitAndApprove(user, workflow, data);
        }

        // then
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(count, bossRecords.size());
        assertEquals(count, bossRecords.stream().filter(FlowRecord::isMergeable).toList().size());

        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(boss.getUserId());
        assertEquals(1, todos.size());
        assertEquals(count, todos.get(0).getMargeCount());
    }

    /**
     * 场景：B 节点多人审批（或签），不同审批人的记录各自合并，互不干扰。
     */
    @Test
    void shouldNotMergeDifferentApprovers() {
        // given
        User user = new User(1, "user");
        User boss1 = new User(2, "boss1");
        User boss2 = new User(3, "boss2");
        factory.userGateway.save(user);
        factory.userGateway.save(boss1);
        factory.userGateway.save(boss2);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(user, "mt-multi", "def run(request){return [2,3]}",
                true, MultiOperatorAuditStrategy.Type.ANY);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");
        int count = 2;

        // when
        for (int i = 0; i < count; i++) {
            submitAndApprove(user, workflow, data);
        }

        // then 每个审批人各自合并成一条待办，合并依据不同
        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findAll();
        assertEquals(2, todos.size());
        Set<String> keys = todos.stream().map(FlowTodoRecord::getTodoKey).collect(Collectors.toSet());
        assertEquals(2, keys.size());
    }

    /**
     * 场景：B 节点未开启合并时，每条流程记录独立生成待办。
     */
    @Test
    void shouldNotMergeWhenDisabled() {
        // given
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(user, "mt-nomerge", "def run(request){return [2]}",
                false, null);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");
        int count = 2;

        // when
        for (int i = 0; i < count; i++) {
            submitAndApprove(user, workflow, data);
        }

        // then
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(count, bossRecords.size());
        List<FlowTodoRecord> todos = factory.flowTodoRecordRepository.findByOperatorId(boss.getUserId());
        assertEquals(count, todos.size());
    }

    /**
     * 场景：A-B-C 流程中，A 节点既是发起人也是提交人，B 节点记录的
     * createOperatorId / submitOperatorId 均等于提交人，currentOperatorId 为审批人。
     */
    @Test
    void shouldKeepCreatorAndSubmitterFields() {
        // given
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(user, "mt-fields", "def run(request){return [2]}",
                true, null);
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // when
        submitAndApprove(user, workflow, data);

        // then
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());
        FlowRecord bossRecord = bossRecords.get(0);
        assertEquals(user.getUserId(), bossRecord.getCreateOperatorId());
        assertEquals(user.getUserId(), bossRecord.getSubmitOperatorId());
        assertEquals(boss.getUserId(), bossRecord.getCurrentOperatorId());
        // 合并依据包含审批人
        assertTrue(bossRecord.getTodoKey().contains(String.valueOf(boss.getUserId())));
    }

    // ─── 辅助方法 ───

    private Workflow buildWorkflow(User createdBy, String code, String operatorScript,
                                   boolean mergeable, MultiOperatorAuditStrategy.Type multiType) {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code(code)
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        NodeStrategyBuilder approvalStrategies = NodeStrategyBuilder.builder()
                .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                        .addPermission(code, "name", PermissionType.WRITE)
                        .build()))
                .addStrategy(new OperatorLoadStrategy(
                        FlowGroovyScriptFactory.createOperatorLoadScript(operatorScript).getKey()));
        if (mergeable) {
            approvalStrategies.addStrategy(new RecordMergeStrategy(true));
        }
        if (multiType != null) {
            approvalStrategies.addStrategy(new MultiOperatorAuditStrategy(multiType, 0f));
        }

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("审批")
                .strategies(approvalStrategies.build())
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("测试流程")
                .code(code)
                .createdOperator(createdBy)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
    }

    private void submitAndApprove(User user, Workflow workflow, Map<String, Object> data) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> userRecords = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        FlowRecord userRecord = userRecords.get(userRecords.size() - 1);

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(userRecord.getId());
        actionRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(actionRequest);
    }
}