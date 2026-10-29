package com.codingapi.example;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.action.actions.RejectAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
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
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #235 补充场景（生产路径）：C 节点任意审批，拒绝配置为终止流程。
 *
 * <p>验证 c1 拒绝终止后，c2 的同节点待办自动办结，流程终止，且流程视图/详情查询正常。
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:flow-issue235-terminate-test;DB_CLOSE_DELAY=-1")
class FlowIssue235AnyRejectTerminateTest {

    @Autowired
    private FlowService flowService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlowRecordRepository flowRecordRepository;

    private User createUser(String account) {
        User user = new User();
        user.setName(account);
        user.setAccount(account);
        user.setPassword("pwd-" + System.nanoTime());
        user.setFlowManager(false);
        return userRepository.save(user);
    }

    private FlowRecord todoOf(String processId, long operatorId) {
        List<FlowRecord> todos = flowRecordRepository.findTodoRecords(processId).stream()
                .filter(record -> record.getCurrentOperatorId() == operatorId)
                .toList();
        assertEquals(1, todos.size(), "审批人[" + operatorId + "]应有一条待办");
        return todos.get(0);
    }

    @Test
    void anyRejectTerminateShouldAutoDoneSiblingAndTerminateFlow() {
        String code = "issue235-terminate-" + System.nanoTime();

        User creator = createUser("issue235-creator-" + System.nanoTime());
        User b1 = createUser("issue235-b1-" + System.nanoTime());
        User c1 = createUser("issue235-c1-" + System.nanoTime());
        User c2 = createUser("issue235-c2-" + System.nanoTime());

        FlowForm form = FlowFormBuilder.builder()
                .name("issue235")
                .code(code)
                .addField("name", "name", DataType.STRING)
                .build();
        FormFieldPermissionsBuilder permissions = FormFieldPermissionsBuilder.builder()
                .addPermission(code, "name", PermissionType.WRITE);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .build())
                .actions(ActionBuilder.builder().addAction(CustomAction.defaultAction()).build())
                .build();

        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory
                                .createOperatorLoadScript("def run(request){return [" + b1.getId() + "]}")
                                .getKey()))
                        .build())
                .build();

        RejectAction rejectAction = RejectAction.defaultAction();
        rejectAction.setScript(FlowGroovyScriptFactory
                .createActionRejectScript("def run(request){return 'TERMINATE'}")
                .getKey());
        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory
                                .createOperatorLoadScript(
                                        "def run(request){return [" + c1.getId() + "," + c2.getId() + "]}")
                                .getKey()))
                        .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(PassAction.defaultAction())
                        .addAction(rejectAction)
                        .build())
                .build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("issue235")
                .code(code)
                .createdOperator(creator)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(EndNode.builder().build())
                .build();
        workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // 发起 -> B
        IFlowAction startAction = startNode.actionManager().getActions().get(0);
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(code);
        createRequest.setFormData(data);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(creator.getId());
        long startRecordId = flowService.create(createRequest);
        String processId = flowRecordRepository.get(startRecordId).getProcessId();

        FlowActionRequest startPass = new FlowActionRequest();
        startPass.setFormData(data);
        startPass.setRecordId(startRecordId);
        startPass.setAdvice(new FlowAdviceBody(startAction.id(), "同意", creator.getId()));
        flowService.action(startPass);

        // B 通过 -> C
        FlowRecord bTodo = todoOf(processId, b1.getId());
        pass(bTodo, workflow.getFlowNode(bTodo.getNodeId()), b1.getId(), data);

        // C 节点 c1、c2 同时收到待办
        FlowRecord c1Record = todoOf(processId, c1.getId());
        FlowRecord c2Record = todoOf(processId, c2.getId());

        // c1 拒绝并终止流程
        IFlowAction rejectActionOfNode = workflow.getFlowNode(c1Record.getNodeId())
                .actionManager().getActions().stream()
                .filter(action -> "REJECT".equals(action.type()))
                .findFirst().orElseThrow();
        FlowActionRequest rejectRequest = new FlowActionRequest();
        rejectRequest.setFormData(data);
        rejectRequest.setRecordId(c1Record.getId());
        rejectRequest.setAdvice(new FlowAdviceBody(rejectActionOfNode.id(), "拒绝", c1.getId()));
        flowService.action(rejectRequest);

        // 终止后：c2 无待办，c2 记录已办
        assertEquals(0, flowRecordRepository.findTodoRecords(processId).stream()
                        .filter(record -> record.getCurrentOperatorId() == c2.getId()).count(),
                "c2 不应遗留待办");

        // 以自动办结的 c2 记录为入口查询流程视图与详情，均不应报错
        assertNotNull(flowService.processNodes(
                new FlowProcessNodeRequest(c2Record.getId(), c2.getId(), data)));
        assertNotNull(flowService.detail(new FlowDetailRequest(c2Record.getId(), c2.getId())));

        List<FlowRecord> processRecords = flowRecordRepository.findProcessRecords(processId);
        List<FlowRecord> cRecords = processRecords.stream()
                .filter(record -> c1Record.getNodeId().equals(record.getNodeId()))
                .toList();
        assertEquals(2, cRecords.stream().filter(FlowRecord::isDone).count(),
                "C 节点两条记录都应为已办");
        assertTrue(processRecords.stream().allMatch(FlowRecord::isFinish),
                "流程终止后所有记录应进入终态");
        assertTrue(processRecords.stream()
                        .filter(record -> c2.getId() == record.getCurrentOperatorId())
                        .allMatch(FlowRecord::isFinish),
                "c2 自动办结的记录也应进入终态");
    }

    private void pass(FlowRecord record, com.codingapi.flow.node.IFlowNode node, long operatorId,
                      Map<String, Object> data) {
        IFlowAction action = node.actionManager().getActions().stream()
                .filter(item -> "PASS".equals(item.type()))
                .findFirst().orElseThrow();
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(data);
        request.setRecordId(record.getId());
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operatorId));
        flowService.action(request);
    }
}