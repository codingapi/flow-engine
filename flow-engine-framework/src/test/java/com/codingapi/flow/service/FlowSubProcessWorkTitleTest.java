package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 子流程实例支持手动传入 workTitle（流程标题）的测试（issue #197）。
 */
class FlowSubProcessWorkTitleTest {

    private static final String FORM_CODE = "sub-process-work-title-form";
    private static final String PARENT_CODE = "sub-process-work-title-parent";
    private static final String CHILD_CODE = "sub-process-work-title-child";

    private MyFlowServiceFactory factory;
    private User initiator;
    private User childOperator;
    private FlowForm form;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        initiator = saveUser(1, "发起人");
        childOperator = saveUser(2, "子流程审批人");
        form = FlowFormBuilder.builder()
                .name("子流程标题测试表单")
                .code(FORM_CODE)
                .addField("业务内容", "content", DataType.STRING)
                .build();
    }

    /**
     * 测试目标：子流程脚本 5 参 toCreateRequest 传入 workTitle 时，子流程开始记录标题等于自定义标题。
     * 前置条件：主流程子流程节点脚本返回带 workTitle 的 FlowCreateRequest。
     * 执行步骤：提交主流程，触发子流程创建。
     * 期望断言：子流程待办记录的 getWorkTitle() 等于传入的自定义标题。
     */
    @Test
    void shouldUseCustomWorkTitleWhenSubProcessScriptPassesIt() {
        StartNode childStart = writableStart("子流程开始");
        ApprovalNode childApproval = approvalNode("子流程审批", childOperator);
        saveChildWorkflow("子流程标题测试-子流程", childStart, childApproval);
        String createScript = """
                def run(request){
                    return request.toCreateRequest('%s', %d, '%s', [content:'child'], '批次一号')
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        StartNode parentStart = writableStart("主流程开始");
        saveParentWorkflow("子流程标题测试-主流程", parentStart, subProcessNode(createScript));

        createAndSubmitParent(parentStart);

        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        assertEquals(1, childTodos.size(), "应创建一个子流程待办");
        assertEquals("批次一号", childTodos.get(0).getWorkTitle(),
                "5 参脚本传入自定义标题后，子流程记录标题应被覆盖");
    }

    /**
     * 测试目标：子流程脚本 4 参 toCreateRequest 不传 workTitle 时，回落子流程流程定义标题。
     * 前置条件：主流程子流程节点脚本返回不带 workTitle 的 FlowCreateRequest。
     * 执行步骤：提交主流程，触发子流程创建。
     * 期望断言：子流程待办记录的 getWorkTitle() 等于子流程流程定义的标题。
     */
    @Test
    void shouldFallbackToWorkflowTitleWhenSubProcessScriptOmitsWorkTitle() {
        StartNode childStart = writableStart("子流程开始");
        ApprovalNode childApproval = approvalNode("子流程审批", childOperator);
        saveChildWorkflow("子流程标题测试-子流程", childStart, childApproval);
        String createScript = """
                def run(request){
                    return request.toCreateRequest('%s', %d, '%s', [content:'child'])
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        StartNode parentStart = writableStart("主流程开始");
        saveParentWorkflow("子流程标题测试-主流程", parentStart, subProcessNode(createScript));

        createAndSubmitParent(parentStart);

        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        assertEquals(1, childTodos.size(), "应创建一个子流程待办");
        assertEquals("子流程标题测试-子流程", childTodos.get(0).getWorkTitle(),
                "4 参脚本不传标题时应回落子流程流程定义标题");
    }

    /**
     * 测试目标：创建请求直接携带 workTitle 时，首条记录标题被覆盖（REST 语义）。
     * 前置条件：直接构造 FlowCreateRequest 并设置 workTitle。
     * 执行步骤：调用 flowService.create() 创建流程。
     * 期望断言：生成的开始记录 getWorkTitle() 等于设置的标题。
     */
    @Test
    void shouldOverrideWorkTitleWhenCreateRequestCarriesIt() {
        StartNode start = writableStart("顶层流程开始");
        factory.workflowService.saveWorkflow(WorkflowBuilder.builder()
                .title("顶层流程默认标题")
                .code("sub-process-work-title-top")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(EndNode.builder().name("顶层流程结束").build())
                .build());

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode("sub-process-work-title-top");
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(Map.of("content", "top"));
        request.setWorkTitle("手动传入的顶层标题");

        long recordId = factory.flowService.create(request);
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        assertEquals("手动传入的顶层标题", record.getWorkTitle(),
                "创建请求携带 workTitle 时记录标题应被覆盖");
    }

    /**
     * 测试目标：创建请求不携带 workTitle 时回落流程定义标题。
     * 前置条件：直接构造 FlowCreateRequest 且不设置 workTitle。
     * 执行步骤：调用 flowService.create() 创建流程。
     * 期望断言：生成的开始记录 getWorkTitle() 等于流程定义标题。
     */
    @Test
    void shouldKeepWorkflowTitleWhenCreateRequestHasNoWorkTitle() {
        StartNode start = writableStart("回落流程开始");
        factory.workflowService.saveWorkflow(WorkflowBuilder.builder()
                .title("回落流程定义标题")
                .code("sub-process-work-title-fallback")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(EndNode.builder().name("回落流程结束").build())
                .build());

        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode("sub-process-work-title-fallback");
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(Map.of("content", "fallback"));

        long recordId = factory.flowService.create(request);
        FlowRecord record = factory.flowRecordRepository.get(recordId);
        assertEquals("回落流程定义标题", record.getWorkTitle());
    }

    private void saveChildWorkflow(String title, StartNode childStart, ApprovalNode childApproval) {
        factory.workflowService.saveWorkflow(WorkflowBuilder.builder()
                .title(title)
                .code(CHILD_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(childStart)
                .addNode(childApproval)
                .addNode(EndNode.builder().name("子流程结束").build())
                .build());
    }

    private void saveParentWorkflow(String title, StartNode start, SubProcessNode subProcess) {
        factory.workflowService.saveWorkflow(WorkflowBuilder.builder()
                .title(title)
                .code(PARENT_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(EndNode.builder().name("主流程结束").build())
                .build());
    }

    private SubProcessNode subProcessNode(String createScript) {
        return SubProcessNode.builder()
                .name("子流程节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(), true))
                        .build())
                .build();
    }

    private void createAndSubmitParent(StartNode start) {
        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(PARENT_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        approve(factory.flowRecordRepository.get(recordId), passAction(start), initiator, data);
    }

    private StartNode writableStart(String name) {
        return StartNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(FORM_CODE, "content", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();
    }

    private ApprovalNode approvalNode(String name, User operator) {
        String script = "def run(request){return [" + operator.getUserId() + "]}";
        return ApprovalNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(FORM_CODE, "content", PermissionType.READ)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(script).getKey()))
                        .build())
                .build();
    }

    private IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActions().stream()
                .filter(action -> "PASS".equals(action.type()))
                .findFirst()
                .orElseThrow();
    }

    private void approve(FlowRecord record, IFlowAction action, User operator, Map<String, Object> data) {
        FlowActionRequest request = new FlowActionRequest();
        request.setRecordId(record.getId());
        request.setFormData(data);
        request.setAdvice(new FlowAdviceBody(action.id(), "同意", operator.getUserId()));
        factory.flowService.action(request);
    }

    private List<FlowRecord> todos(User operator, IFlowNode node) {
        return factory.flowRecordRepository.findTodoByOperator(operator.getUserId()).stream()
                .filter(record -> record.getNodeId().equals(node.getId()))
                .toList();
    }

    private User saveUser(long id, String name) {
        User user = new User(id, name);
        factory.userGateway.save(user);
        return user;
    }
}