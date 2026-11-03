package com.codingapi.example;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
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
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.infra.jpa.FlowTodoRecordEntityRepository;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
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
 * issue #240（生产持久化路径复现）：合并审批只处理部分记录时的待办代表记录异常。
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:flow-issue240-test;DB_CLOSE_DELAY=-1")
class FlowIssue240MergePartialApproveTest {

    @Autowired
    private FlowService flowService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlowRecordRepository flowRecordRepository;

    @Autowired
    private FlowTodoRecordEntityRepository flowTodoRecordEntityRepository;

    private User createUser(String account) {
        User user = new User();
        user.setName(account);
        user.setAccount(account);
        user.setPassword("pwd-" + System.nanoTime());
        user.setFlowManager(false);
        return userRepository.save(user);
    }

    @Test
    void partialApproveOfMergedTodo() {
        String code = "issue240-" + System.nanoTime();

        User a1 = createUser("issue240-a1-" + System.nanoTime());
        User a2 = createUser("issue240-a2-" + System.nanoTime());
        User a3 = createUser("issue240-a3-" + System.nanoTime());
        User a4 = createUser("issue240-a4-" + System.nanoTime());
        User b1 = createUser("issue240-b1-" + System.nanoTime());

        FlowForm form = FlowFormBuilder.builder()
                .name("issue240")
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

        String approverScript = "def run(request){return [" + b1.getId() + "]}";
        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(approverScript).getKey()))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build())
                .build();
        ApprovalNode cNode = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(approverScript).getKey()))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build())
                .build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("issue240")
                .code(code)
                .createdOperator(a1)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(cNode)
                .addNode(EndNode.builder().build())
                .build();
        workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // a1 最后提交，合并待办行的代表记录即为 a1 的记录（问题现场）
        createAndSubmit(startNode, code, a2, data);
        createAndSubmit(startNode, code, a3, data);
        createAndSubmit(startNode, code, a4, data);
        createAndSubmit(startNode, code, a1, data);

        List<FlowTodoRecordEntity> bTodos = todoGroupsOf(b1);
        System.out.println("B 阶段待办组：" + describe(bTodos));
        assertEquals(1, bTodos.size());
        assertEquals(4, bTodos.get(0).getMargeCount());

        // b1 打开合并待办详情，选择 a1、a2 进行审批
        FlowContent detail = flowService.detail(new FlowDetailRequest(bTodos.get(0).getRecordId(), b1.getId()));
        assertEquals(4, detail.getTodos().size());

        List<FlowContent.Body> selected = detail.getTodos().stream()
                .filter(body -> body.getCreatedOperator().getId() == a1.getId()
                        || body.getCreatedOperator().getId() == a2.getId())
                .toList();
        assertEquals(2, selected.size());

        for (FlowContent.Body body : selected) {
            approve(code, b1, body.getRecordId(), data);
        }

        List<FlowTodoRecordEntity> todos = todoGroupsOf(b1);
        System.out.println("审批后 b1 待办组：" + describe(todos));

        FlowTodoRecordEntity bGroup = findGroup(todos, bNode);
        FlowTodoRecordEntity cGroup = findGroup(todos, cNode);
        assertNotNull(bGroup, "B 节点合并待办不应消失");
        assertEquals(2, bGroup.getMargeCount(), "B 节点应剩余 a3、a4");
        assertNotNull(cGroup, "C 节点应产生合并待办");
        assertEquals(2, cGroup.getMargeCount(), "C 节点应合并 a1、a2");
        assertEquals(2, todos.size(), "b1 应只有 B、C 两个待办组");

        // B 行代表记录仍必须指向未办理的待办记录（a3/a4），否则待办行无法继续审批
        FlowRecord bRepresentative = flowRecordRepository.get(bGroup.getRecordId());
        assertNotNull(bRepresentative, "B 组代表记录应存在");
        assertTrue(bRepresentative.isTodo(),
                "B 组代表记录应为待办，实际指向 recordId=" + bRepresentative.getId()
                        + ",recordState=" + bRepresentative.getRecordState());

        // 以 B 行代表记录打开详情，仍应展示 a3、a4 两条合并记录且可合并审批
        FlowContent bDetail = flowService.detail(new FlowDetailRequest(bGroup.getRecordId(), b1.getId()));
        assertEquals(2, bDetail.getTodos().size(), "B 组详情应展示剩余 a3、a4 两条记录");
        assertTrue(bDetail.isMergeable(), "B 组详情应保持可合并审批状态");
    }

    private List<FlowTodoRecordEntity> todoGroupsOf(User operator) {
        return flowTodoRecordEntityRepository.findAll().stream()
                .filter(entity -> entity.getCurrentOperatorId() != null
                        && entity.getCurrentOperatorId().equals(operator.getId()))
                .toList();
    }

    private String describe(List<FlowTodoRecordEntity> todos) {
        return todos.stream()
                .map(todo -> todo.getNodeName() + "[key=" + todo.getTodoKey()
                        + ",count=" + todo.getMargeCount() + ",recordId=" + todo.getRecordId() + "]")
                .toList().toString();
    }

    private FlowTodoRecordEntity findGroup(List<FlowTodoRecordEntity> todos, IFlowNode node) {
        return todos.stream()
                .filter(todo -> node.getId().equals(todo.getNodeId()))
                .findFirst().orElse(null);
    }

    private void createAndSubmit(StartNode startNode, String code, User user, Map<String, Object> data) {
        IFlowAction startAction = startNode.actionManager().getActions().get(0);
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(code);
        createRequest.setFormData(data);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(user.getId());
        long recordId = flowService.create(createRequest);

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(startAction.id(), "同意", user.getId()));
        flowService.action(actionRequest);
    }

    private void approve(String code, User approver, long recordId, Map<String, Object> data) {
        FlowRecord record = flowRecordRepository.get(recordId);
        Workflow workflow = workflowService.getWorkflowByCode(code);
        IFlowAction passAction = workflow.getFlowNode(record.getNodeId()).actionManager().getActions().stream()
                .filter(action -> "PASS".equals(action.type()))
                .findFirst().orElseThrow();
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(passAction.id(), "同意", approver.getId()));
        flowService.action(actionRequest);
    }
}