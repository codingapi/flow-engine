package com.codingapi.flow.pojo.response;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.operator.OperatorKey;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 当前节点待审批人的身份维度测试（issue #288 衍生 / #290、#292）。
 *
 * <p>同一个人员以两个身份被指定到同一节点时：
 * <ul>
 *     <li>分配关系必须保存两项 {@code (人员id, 身份)}；</li>
 *     <li>节点必须生成两条待办记录，身份各自独立；</li>
 *     <li>详情页的当前节点待审批人列表必须同时出现两个身份（只按人员 id 去重会吞掉一个）。</li>
 * </ul>
 */
class FlowCurrentNodeOperatorsIdentityTest {

    private static final long CREATOR_ID = 1;
    private static final long APPROVER_ID = 2;
    private static final String IDENTITY_ONE = "department1";
    private static final String IDENTITY_TWO = "department2";

    private MyFlowServiceFactory factory;
    private Workflow workflow;
    private ApprovalNode approvalNode;

    @BeforeEach
    void setUp() {
        User creator = new User(CREATOR_ID, "creator");
        User approver = new User(APPROVER_ID, "user1");
        factory = new MyFlowServiceFactory();
        factory.userGateway.save(creator);
        factory.userGateway.save(approver);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        String workCode = "identity-current-operators";
        FlowForm form = FlowFormBuilder.builder()
                .name("多身份流程")
                .code(workCode)
                .addField("标题", "title", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy())
                        // 并签：两个身份同时在岗，避免默认顺序审批把第二条记录隐藏
                        .addStrategy(new MultiOperatorAuditStrategy(
                                MultiOperatorAuditStrategy.Type.MERGE, 1.0f))
                        .build())
                .build();

        workflow = WorkflowBuilder.builder()
                .title("多身份流程")
                .code(workCode)
                .createdOperator(creator)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(EndNode.builder().build())
                .build();
        factory.workflowService.saveWorkflow(workflow);
    }

    private Map<String, Object> formData() {
        return Map.of("title", "同一人两个身份");
    }

    /**
     * 发起流程，并在开始节点把审批节点的操作人指定为「同一人的两个身份」。
     */
    private String createWithTwoIdentities() {
        List<IFlowAction> startActions = workflow.getStartNode().actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(formData());
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(CREATOR_ID);
        createRequest.setOperatorSelectOperators(Map.of(approvalNode.getId(), List.of(
                OperatorKey.of(APPROVER_ID, IDENTITY_ONE),
                OperatorKey.of(APPROVER_ID, IDENTITY_TWO))));
        factory.flowService.create(createRequest);

        List<FlowRecord> creatorRecords = factory.flowRecordRepository.findTodoByOperator(CREATOR_ID);
        assertEquals(1, creatorRecords.size(), "发起后应有一条发起人待办");

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(formData());
        actionRequest.setRecordId(creatorRecords.get(0).getId());
        actionRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", CREATOR_ID));
        factory.flowService.action(actionRequest);

        return creatorRecords.get(0).getProcessId();
    }

    @Test
    void should_persist_two_identities_of_same_operator() {
        String processId = createWithTwoIdentities();

        List<OperatorKey> assigned = factory.flowOperatorAssignmentRepository
                .findOperators(processId, approvalNode.getId());

        assertEquals(2, assigned.size(), "同一人的两个身份应各自保存一项");
        assertTrue(assigned.contains(OperatorKey.of(APPROVER_ID, IDENTITY_ONE)));
        assertTrue(assigned.contains(OperatorKey.of(APPROVER_ID, IDENTITY_TWO)));
    }

    @Test
    void should_keep_legacy_id_view_for_assignment() {
        String processId = createWithTwoIdentities();

        List<Long> legacyIds = factory.flowOperatorAssignmentRepository
                .findOperatorIds(processId, approvalNode.getId());

        assertEquals(List.of(APPROVER_ID, APPROVER_ID), legacyIds, "存量按 id 读取的口径不变");
    }

    @Test
    void should_create_one_record_per_identity() {
        String processId = createWithTwoIdentities();

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(processId).stream()
                .filter(record -> approvalNode.getId().equals(record.getNodeId()))
                .toList();

        assertEquals(2, records.size(), "每个身份都应生成一条审批记录");
        assertEquals(Set.of(IDENTITY_ONE, IDENTITY_TWO),
                records.stream().map(FlowRecord::getCurrentOperatorIdentity).collect(java.util.stream.Collectors.toSet()));
    }

    /**
     * 详情页当前节点待审批人：同一人的两个身份必须同时出现，且当前操作人只排除「同人同身份」。
     */
    @Test
    void should_list_both_identities_in_current_node_operators() {
        String processId = createWithTwoIdentities();

        List<FlowRecord> nodeRecords = factory.flowRecordRepository.findProcessRecords(processId).stream()
                .filter(record -> approvalNode.getId().equals(record.getNodeId()))
                .toList();
        assertEquals(2, nodeRecords.size());

        FlowRecord currentRecord = nodeRecords.stream()
                .filter(record -> IDENTITY_ONE.equals(record.getCurrentOperatorIdentity()))
                .findFirst()
                .orElseThrow();

        List<FlowOperator> operators = FlowContent.resolveCurrentNodeOperators(currentRecord, nodeRecords);

        assertEquals(1, operators.size(), "同一人的另一个身份仍应出现在待审批人列表中");
        assertEquals(IDENTITY_TWO, operators.get(0).getIdentity());
        assertEquals(APPROVER_ID, operators.get(0).getId());
    }
}
