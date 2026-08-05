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
import com.codingapi.flow.pojo.request.FlowDeleteRequest;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.ErrorTriggerStrategy;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowSubProcessEnhancementServiceTest {

    private static final String FORM_CODE = "sub-process-enhancement-form";
    private static final String PARENT_CODE = "sub-process-enhancement-parent";
    private static final String CHILD_CODE = "sub-process-enhancement-child";

    private MyFlowServiceFactory factory;
    private User initiator;
    private User childOperator;
    private User finalOperator;
    private FlowForm form;
    private StartNode childStart;
    private ApprovalNode childApproval;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        initiator = saveUser(1, "发起人");
        childOperator = saveUser(2, "子流程审批人");
        finalOperator = saveUser(3, "主流程最终审批人");
        form = FlowFormBuilder.builder()
                .name("子流程增强测试表单")
                .code(FORM_CODE)
                .addField("业务内容", "content", DataType.STRING)
                .build();
        saveChildWorkflow();
    }

    /**
     * 测试目标：验证一个子流程节点能创建两个实例，并在全部实例满足结果脚本前阻塞主流程。
     * 前置条件：创建脚本返回两个 FlowCreateRequest，结果脚本要求已完成数量等于总数。
     * 执行步骤：提交主流程，依次完成两个子流程，再进入主流程最终审批节点。
     * 期望断言：第一次完成不恢复主流程；第二次完成只恢复一次；后续节点脚本能读取两条最终记录。
     */
    @Test
    void shouldCreateMultipleChildrenWaitForResultAndExposeFinalRecordsToLaterScripts() {
        StartNode parentStart = writableStart("主流程开始");
        String subProcessNodeId = "sub-process-node";
        String createScript = """
                def run(request){
                    return [
                        request.toCreateRequest('%s', %d, '%s', [content:'child-1']),
                        request.toCreateRequest('%s', %d, '%s', [content:'child-2'])
                    ]
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id(),
                CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        String resultScript = """
                def run(request){
                    def records = request.findSubProcessRecords(request.getCurrentNode().getId())
                    return request.getCurrentSubProcessRecord() != null
                        && records.size() == request.getSubProcessTotal()
                }
                """;
        SubProcessNode subProcess = SubProcessNode.builder()
                .id(subProcessNodeId)
                .name("批量子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(),
                                true,
                                FlowGroovyScriptFactory.createSubProcessResultScript(resultScript).getKey()))
                        .build())
                .build();
        String operatorScript = """
                def run(request){
                    def records = request.findSubProcessRecords('%s')
                    return records.size() == 2 && records.every { it.getFormData().get('content').startsWith('child-') }
                        ? [%d] : []
                }
                """.formatted(subProcessNodeId, finalOperator.getUserId());
        ApprovalNode finalApproval = ApprovalNode.builder()
                .name("最终审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(operatorScript).getKey()))
                        .build())
                .build();
        saveParentWorkflow(parentStart, subProcess, finalApproval);

        long parentRecordId = createAndSubmitParent(parentStart);

        List<FlowRecord> childTodos = todos(childOperator, childApproval);
        assertEquals(2, childTodos.size(), "子流程节点应同时创建两个子流程待办");
        assertTrue(todos(finalOperator, finalApproval).isEmpty(), "子流程未完成时主流程必须保持阻塞");
        assertThrows(FlowStateException.class,
                () -> factory.flowService.revoke(new FlowRevokeRequest(parentRecordId, initiator.getUserId())),
                "主流程等待子流程时不允许撤销触发记录");

        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        assertTrue(todos(finalOperator, finalApproval).isEmpty(), "第一个子流程完成后结果条件尚未满足");
        ProcessNode waitingSubProcess = findProcessNode(
                loadProcessNodes(parentRecordId, initiator), subProcessNodeId);
        assertAll("部分子流程完成时的节点信息",
                () -> assertEquals(ProcessNode.ApproveState.PROCESSING,
                        waitingSubProcess.getApproveState()),
                () -> assertEquals(2, waitingSubProcess.getSubProcess().getTotalCount()),
                () -> assertEquals(1, waitingSubProcess.getSubProcess().getFinishedCount()),
                () -> assertEquals("WAITING", waitingSubProcess.getSubProcess().getState().name()));

        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);
        List<ProcessNode> resumedProcessNodes = loadProcessNodes(finalTodo.getId(), finalOperator);
        ProcessNode passedSubProcess = findProcessNode(resumedProcessNodes, subProcessNodeId);
        assertAll("全部子流程完成后的节点信息",
                () -> assertEquals(1, todos(finalOperator, finalApproval).size(),
                        "全部子流程完成后主流程只能恢复一次"),
                () -> assertEquals(List.of("主流程开始", "批量子流程", "最终审批", "主流程结束"),
                        resumedProcessNodes.stream().map(ProcessNode::getNodeName).toList()),
                () -> assertEquals(ProcessNode.ApproveState.PASS, passedSubProcess.getApproveState()),
                () -> assertEquals(2, passedSubProcess.getSubProcess().getFinishedCount()),
                () -> assertEquals("PASSED", passedSubProcess.getSubProcess().getState().name()),
                () -> assertTrue(passedSubProcess.getSubProcess().getInstances().stream()
                        .allMatch(instance -> "FINISHED".equals(instance.getState().name()))));

        approve(finalTodo, passAction(finalApproval), finalOperator, finalTodo.getFormData());
        List<ProcessNode> finishedProcessNodes = loadProcessNodes(finalTodo.getId(), finalOperator);
        assertAll("主流程完成后的完整节点信息",
                () -> assertEquals(List.of("主流程开始", "批量子流程", "最终审批", "主流程结束"),
                        finishedProcessNodes.stream().map(ProcessNode::getNodeName).toList()),
                () -> assertTrue(finishedProcessNodes.stream()
                        .allMatch(node -> node.getApproveState() == ProcessNode.ApproveState.PASS)));
    }

    /**
     * 测试目标：验证主流程等待子流程时，“全部流程节点”仍展示子流程节点和后续节点。
     * 前置条件：流程定义为 A -> B -> SubProcess -> C -> D，子流程已经启动但尚未结束。
     * 执行步骤：完成 A、B 节点，使主流程停留在 SubProcess 节点，再查询主流程节点列表。
     * 期望断言：展示 A、B、SubProcess、C、D；子流程处理中，C、D 尚未执行。
     */
    @Test
    void shouldShowWaitingSubProcessAndRemainingNodesInParentProcessView() {
        StartNode nodeA = writableStart("A");
        ApprovalNode nodeB = approvalNode("B", finalOperator);
        String createScript = """
                def run(request){
                    return request.toCreateRequest('%s', %d, '%s', [content:'waiting-child'])
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("SubProcess")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(), true))
                        .build())
                .build();
        ApprovalNode nodeC = approvalNode("C", initiator);
        Workflow parentWorkflow = WorkflowBuilder.builder()
                .title("等待子流程时的主流程节点展示")
                .code(PARENT_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(nodeA)
                .addNode(nodeB)
                .addNode(subProcess)
                .addNode(nodeC)
                .addNode(EndNode.builder().name("D").build())
                .build();
        factory.workflowService.saveWorkflow(parentWorkflow);

        createAndSubmitParent(nodeA);
        FlowRecord nodeBRecord = todos(finalOperator, nodeB).get(0);
        approve(nodeBRecord, passAction(nodeB), finalOperator, nodeBRecord.getFormData());

        assertEquals(1, todos(childOperator, childApproval).size(), "子流程应处于审批中");
        assertTrue(todos(initiator, nodeC).isEmpty(), "主流程 C 节点尚未执行");

        List<ProcessNode> processNodes = factory.flowService.processNodes(
                new FlowProcessNodeRequest(nodeBRecord.getId(), finalOperator.getUserId(),
                        Map.of("content", "parent")));
        Map<String, ProcessNode.ApproveState> stateByNodeName = processNodes.stream()
                .collect(Collectors.toMap(ProcessNode::getNodeName, ProcessNode::getApproveState));
        ProcessNode subProcessView = processNodes.stream()
                .filter(node -> "SubProcess".equals(node.getNodeName()))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals(List.of("A", "B", "SubProcess", "C", "D"),
                        processNodes.stream().map(ProcessNode::getNodeName).toList()),
                () -> assertEquals(ProcessNode.ApproveState.PASS, stateByNodeName.get("A")),
                () -> assertEquals(ProcessNode.ApproveState.PASS, stateByNodeName.get("B")),
                () -> assertEquals(ProcessNode.ApproveState.PROCESSING, stateByNodeName.get("SubProcess")),
                () -> assertEquals(ProcessNode.ApproveState.PENDING, stateByNodeName.get("C")),
                () -> assertEquals(ProcessNode.ApproveState.PENDING, stateByNodeName.get("D")),
                () -> assertNotNull(subProcessView.getSubProcess()),
                () -> assertEquals(1, subProcessView.getSubProcess().getTotalCount()),
                () -> assertEquals(0, subProcessView.getSubProcess().getFinishedCount()),
                () -> assertEquals("WAITING", subProcessView.getSubProcess().getState().name()),
                () -> assertEquals("RUNNING",
                        subProcessView.getSubProcess().getInstances().get(0).getState().name())
        );
    }

    /**
     * 测试目标：验证全部子流程结束但结果脚本仍返回 false 时执行节点异常跳转。
     * 前置条件：结果脚本固定返回 false，异常策略跳转到主流程开始节点。
     * 执行步骤：提交主流程并完成子流程审批。
     * 期望断言：主流程不会进入正常后续节点，而是在开始节点生成新的待办。
     */
    @Test
    void shouldUseErrorTriggerNodeWhenAllChildrenFinishWithoutPassingResultScript() {
        StartNode parentStart = writableStart("异常回退开始");
        String createScript = """
                def run(request){
                    return request.toCreateRequest('%s', %d, '%s', [content:'failed-child'])
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("结果异常子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(),
                                true,
                                FlowGroovyScriptFactory.createSubProcessResultScript(
                                        "def run(request){ return false }").getKey()))
                        .addStrategy(new ErrorTriggerStrategy(
                                FlowGroovyScriptFactory.createErrorTriggerScript(
                                        "def run(request){ return '" + parentStart.getId() + "' }").getKey()))
                        .build())
                .build();
        ApprovalNode finalApproval = approvalNode("不应进入的正常节点", finalOperator);
        saveParentWorkflow(parentStart, subProcess, finalApproval);

        createAndSubmitParent(parentStart);
        FlowRecord childTodo = todos(childOperator, childApproval).get(0);
        approve(childTodo, passAction(childApproval), childOperator, childTodo.getFormData());

        assertTrue(todos(finalOperator, finalApproval).isEmpty(), "结果失败时不能进入正常后续节点");
        FlowRecord returnedStart = todos(initiator, parentStart).get(0);
        List<ProcessNode> processNodes = loadProcessNodes(returnedStart.getId(), initiator);
        ProcessNode errorSubProcess = processNodes.stream()
                .filter(node -> subProcess.getId().equals(node.getNodeId()) && node.getSubProcess() != null)
                .findFirst()
                .orElseThrow();
        assertAll("子流程结果异常后的节点信息",
                () -> assertEquals(1, todos(initiator, parentStart).size(),
                        "结果失败时应按异常策略回退开始节点"),
                () -> assertEquals(ProcessNode.ApproveState.ERROR, errorSubProcess.getApproveState()),
                () -> assertEquals("ERROR", errorSubProcess.getSubProcess().getState().name()),
                () -> assertEquals(1, errorSubProcess.getSubProcess().getFinishedCount()));
    }

    /**
     * 测试目标：验证结果脚本可以按完成占比提前放行主流程。
     * 前置条件：子流程节点创建两个实例，脚本在完成数达到 50% 时返回 true。
     * 执行步骤：依次完成两个子流程。
     * 期望断言：第一个结束时主流程恢复，剩余子流程结束时不重复恢复。
     */
    @Test
    void shouldResumeOnceWhenCustomCompletionPercentageIsReached() {
        StartNode parentStart = writableStart("占比判定开始");
        String createScript = """
                def run(request){
                    return [
                        request.toCreateRequest('%s', %d, '%s', [content:'ratio-1']),
                        request.toCreateRequest('%s', %d, '%s', [content:'ratio-2'])
                    ]
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id(),
                CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        String resultScript = """
                def run(request){
                    def completed = request.findSubProcessRecords(request.getCurrentNode().getId()).size()
                    return completed * 2 >= request.getSubProcessTotal()
                }
                """;
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("占比判定子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(),
                                true,
                                FlowGroovyScriptFactory.createSubProcessResultScript(resultScript).getKey()))
                        .build())
                .build();
        ApprovalNode finalApproval = approvalNode("占比通过后节点", finalOperator);
        saveParentWorkflow(parentStart, subProcess, finalApproval);

        createAndSubmitParent(parentStart);
        List<FlowRecord> childTodos = todos(childOperator, childApproval);

        approve(childTodos.get(0), passAction(childApproval), childOperator, childTodos.get(0).getFormData());
        FlowRecord finalTodo = todos(finalOperator, finalApproval).get(0);
        ProcessNode passedSubProcess = findProcessNode(
                loadProcessNodes(finalTodo.getId(), finalOperator), subProcess.getId());
        assertAll("完成占比达标后的子流程节点信息",
                () -> assertEquals(1, todos(finalOperator, finalApproval).size(),
                        "完成占比达标后应恢复主流程"),
                () -> assertEquals(ProcessNode.ApproveState.PASS, passedSubProcess.getApproveState()),
                () -> assertEquals(1, passedSubProcess.getSubProcess().getFinishedCount()),
                () -> assertEquals(2, passedSubProcess.getSubProcess().getTotalCount()),
                () -> assertEquals("PASSED", passedSubProcess.getSubProcess().getState().name()));

        approve(childTodos.get(1), passAction(childApproval), childOperator, childTodos.get(1).getFormData());
        ProcessNode updatedSubProcess = findProcessNode(
                loadProcessNodes(finalTodo.getId(), finalOperator), subProcess.getId());
        assertAll("提前放行后的剩余子流程完成信息",
                () -> assertEquals(1, todos(finalOperator, finalApproval).size(),
                        "后续子流程结束不能重复恢复主流程"),
                () -> assertEquals(ProcessNode.ApproveState.PASS, updatedSubProcess.getApproveState()),
                () -> assertEquals(2, updatedSubProcess.getSubProcess().getFinishedCount()),
                () -> assertTrue(updatedSubProcess.getSubProcess().getInstances().stream()
                        .allMatch(instance -> "FINISHED".equals(instance.getState().name()))));
    }

    /**
     * 测试目标：验证未自动提交的子流程不能在主流程等待期间被删除。
     * 前置条件：子流程策略关闭创建后提交。
     * 执行步骤：创建主流程后尝试删除子流程开始待办。
     * 期望断言：删除被拒绝，避免主流程永久等待。
     */
    @Test
    void shouldRejectDeletingChildDraftWhileParentIsWaiting() {
        StartNode parentStart = writableStart("不自动提交开始");
        String createScript = """
                def run(request){
                    return request.toCreateRequest('%s', %d, '%s', [content:'draft-child'])
                }
                """.formatted(CHILD_CODE, initiator.getUserId(), passAction(childStart).id());
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("不自动提交子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(), false))
                        .build())
                .build();
        ApprovalNode finalApproval = approvalNode("子流程后节点", finalOperator);
        saveParentWorkflow(parentStart, subProcess, finalApproval);

        createAndSubmitParent(parentStart);
        FlowRecord childDraft = todos(initiator, childStart).get(0);

        assertThrows(FlowStateException.class,
                () -> factory.flowService.delete(
                        new FlowDeleteRequest(childDraft.getId(), initiator.getUserId())));
        assertTrue(childDraft.isTodo());
    }

    private void saveChildWorkflow() {
        childStart = writableStart("子流程开始");
        childApproval = approvalNode("子流程审批", childOperator);
        Workflow childWorkflow = WorkflowBuilder.builder()
                .title("子流程增强测试-子流程")
                .code(CHILD_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(childStart)
                .addNode(childApproval)
                .addNode(EndNode.builder().name("子流程结束").build())
                .build();
        factory.workflowService.saveWorkflow(childWorkflow);
    }

    private void saveParentWorkflow(StartNode start, SubProcessNode subProcess, ApprovalNode finalApproval) {
        Workflow parentWorkflow = WorkflowBuilder.builder()
                .title("子流程增强测试-主流程")
                .code(PARENT_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(finalApproval)
                .addNode(EndNode.builder().name("主流程结束").build())
                .build();
        factory.workflowService.saveWorkflow(parentWorkflow);
    }

    private long createAndSubmitParent(StartNode start) {
        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(PARENT_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        approve(factory.flowRecordRepository.get(recordId), passAction(start), initiator, data);
        return recordId;
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
                        .addStrategy(readonlyPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(script).getKey()))
                        .build())
                .build();
    }

    private FormFieldPermissionStrategy readonlyPermission() {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(FORM_CODE, "content", PermissionType.READ)
                .build());
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

    private List<ProcessNode> loadProcessNodes(long recordId, User viewer) {
        return factory.flowService.processNodes(new FlowProcessNodeRequest(
                recordId, viewer.getUserId(), Map.of("content", "parent")));
    }

    private ProcessNode findProcessNode(List<ProcessNode> nodes, String nodeId) {
        return nodes.stream()
                .filter(node -> nodeId.equals(node.getNodeId()))
                .findFirst()
                .orElseThrow();
    }

    private IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActions().stream()
                .filter(action -> "PASS".equals(action.type()))
                .findFirst()
                .orElseThrow();
    }

    private User saveUser(long id, String name) {
        User user = new User(id, name);
        factory.userGateway.save(user);
        return user;
    }
}
