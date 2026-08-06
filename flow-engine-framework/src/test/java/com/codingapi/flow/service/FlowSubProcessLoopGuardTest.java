package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.NotifyNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 子流程循环配置与运行期嵌套深度防护场景测试。
 * <p>
 * 覆盖 issue #182 的两种循环：子流程创建自身（含跨流程复合循环）、普通节点异常跳转回自身；
 * 以及流程级最大嵌套深度参数（maxNestDepth）的运行期拦截与合法嵌套不误伤。
 */
class FlowSubProcessLoopGuardTest {

    private static final String FORM_CODE = "loop-guard-form";
    private static final String SELF_LOOP_CODE = "loop-guard-self";
    private static final String LOOP_A_CODE = "loop-guard-a";
    private static final String LOOP_B_CODE = "loop-guard-b";
    private static final String DEPTH_1_CODE = "loop-guard-depth-1";
    private static final String DEPTH_2_CODE = "loop-guard-depth-2";
    private static final String DEPTH_3_CODE = "loop-guard-depth-3";

    private MyFlowServiceFactory factory;
    private User initiator;
    private FlowForm form;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        initiator = saveUser(1, "发起人");
        form = FlowFormBuilder.builder()
                .name("循环防护测试表单")
                .code(FORM_CODE)
                .addField("业务内容", "content", DataType.STRING)
                .build();
    }

    /**
     * 测试目标：子流程默认脚本创建当前流程自身时，运行期沿 parentId 父链检测到同一子流程节点
     * 在实例链上重复触发，抛出子流程循环异常，而不是无限创建子流程记录。
     * 前置条件：主流程 A(开始) -> 子流程 -> B(结束)，子流程创建脚本为 toCreateRequest()（当前流程自身）。
     * 执行步骤：提交开始节点，子流程节点创建子流程并自动提交，子流程再次走到同一子流程节点。
     * 期望断言：抛出 FlowExecutionException，错误码为 execution.subProcess.loop。
     */
    @Test
    void shouldDetectSubProcessSelfLoop() {
        StartNode start = writableStart("自循环开始");
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("自循环子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(
                                        "def run(request){ return request.toCreateRequest() }").getKey(),
                                true))
                        .build())
                .build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("子流程自循环检测")
                .code(SELF_LOOP_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(subProcess)
                .addNode(EndNode.builder().name("自循环结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(SELF_LOOP_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        FlowRecord startRecord = factory.flowRecordRepository.get(recordId);
        approve(startRecord, passAction(start), initiator, data);

        FlowRecord childStartRecord = todos(initiator, start).stream()
                .filter(record -> record.getId() != startRecord.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError("子流程创建后应存在子流程开始待办"));

        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> approve(childStartRecord, passAction(start), initiator, data),
                "子流程创建自身必须在第二次触发时被拦截，而不是无限递归");
        assertEquals("execution.subProcess.loop", exception.getErrCode());
    }

    /**
     * 测试目标：审批节点在未匹配到操作人时，异常策略跳转回当前节点自身必须被拦截，
     * 而不是无限递归导致 StackOverflowError。
     * 前置条件：审批节点操作人脚本返回空列表，异常触发脚本返回当前节点自身id。
     * 执行步骤：提交开始节点进入该审批节点。
     * 期望断言：抛出 FlowExecutionException，错误码为 execution.node.errorTriggerLoop。
     */
    @Test
    void shouldRejectErrorTriggerLoopBackToSelfNode() {
        StartNode start = writableStart("异常自循环开始");
        ApprovalNode approval = ApprovalNode.builder()
                .name("异常自循环审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [] }").getKey()))
                        .addStrategy(new ErrorTriggerStrategy(
                                FlowGroovyScriptFactory.createErrorTriggerScript(
                                        "def run(request){ return request.getCurrentNode().getId() }").getKey()))
                        .build())
                .build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("异常跳转自循环检测")
                .code("loop-guard-error-self")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(approval)
                .addNode(EndNode.builder().name("结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode("loop-guard-error-self");
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        FlowRecord startRecord = factory.flowRecordRepository.get(recordId);

        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> approve(startRecord, passAction(start), initiator, data),
                "异常跳转到当前节点自身必须被拦截，而不是 StackOverflowError");
        assertEquals("execution.node.errorTriggerLoop", exception.getErrCode());
    }

    /**
     * 测试目标：跨流程复合循环 A -> 子流程(B) -> 子流程(A) 在第二次进入 A 的子流程节点时被拦截。
     * 前置条件：A 的子流程节点创建 B，B 的子流程节点创建 A，均自动提交。
     * 执行步骤：提交 A 开始节点，依次自动创建 B、A 的子流程实例。
     * 期望断言：抛出 FlowExecutionException，错误码为 execution.subProcess.loop。
     */
    @Test
    void shouldDetectCrossWorkflowLoop() {
        StartNode startA = writableStart("A开始");
        StartNode startB = writableStart("B开始");
        String saveActionIdB = saveAction(startB).id();
        SubProcessNode subProcessA = SubProcessNode.builder()
                .name("A的子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(
                                        "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                                                .formatted(LOOP_B_CODE, initiator.getUserId(), saveActionIdB)).getKey(),
                                true))
                        .build())
                .build();
        String saveActionIdA = saveAction(startA).id();
        SubProcessNode subProcessB = SubProcessNode.builder()
                .name("B的子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(
                                        "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                                                .formatted(LOOP_A_CODE, initiator.getUserId(), saveActionIdA)).getKey(),
                                true))
                        .build())
                .build();
        Workflow workflowA = WorkflowBuilder.builder()
                .title("复合循环A")
                .code(LOOP_A_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(startA)
                .addNode(subProcessA)
                .addNode(EndNode.builder().name("A结束").build())
                .build();
        Workflow workflowB = WorkflowBuilder.builder()
                .title("复合循环B")
                .code(LOOP_B_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(startB)
                .addNode(subProcessB)
                .addNode(EndNode.builder().name("B结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflowA);
        factory.workflowService.saveWorkflow(workflowB);

        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(LOOP_A_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(startA).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        FlowRecord startRecord = factory.flowRecordRepository.get(recordId);
        approve(startRecord, passAction(startA), initiator, data);

        FlowRecord childBStart = todos(initiator, startB).get(0);
        approve(childBStart, passAction(startB), initiator, data);

        FlowRecord childAStart = todos(initiator, startA).stream()
                .filter(record -> record.getId() != startRecord.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError("进入 A 的子流程后应存在 A 子流程开始待办"));

        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> approve(childAStart, passAction(startA), initiator, data),
                "A -> B -> A 复合循环必须在第二次进入 A 的子流程节点时被拦截");
        assertEquals("execution.subProcess.loop", exception.getErrCode());
    }

    /**
     * 测试目标：子流程嵌套层数超过流程级 maxNestDepth 时被运行期拦截。
     * 前置条件：三个流程 W1/W2/W3 链式嵌套，W1 的子流程创建 W2，W2 的子流程创建 W3，均设置 maxNestDepth=2。
     * 执行步骤：提交 W1 自动进入 W2，再提交 W2 触发 W3 的创建。
     * 期望断言：W2 的子流程节点创建 W3 时抛出 FlowExecutionException，错误码为 execution.subProcess.maxDepth。
     */
    @Test
    void shouldRejectExceedingMaxNestDepth() {
        int maxDepth = 2;
        StartNode start1 = writableStart("深度1开始");
        StartNode start2 = writableStart("深度2开始");
        StartNode start3 = writableStart("深度3开始");
        String saveActionId2 = saveAction(start2).id();
        String saveActionId3 = saveAction(start3).id();
        SubProcessNode sp1 = SubProcessNode.builder()
                .name("深度1子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(
                                        "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                                                .formatted(DEPTH_2_CODE, initiator.getUserId(), saveActionId2)).getKey(),
                                true))
                        .build())
                .build();
        SubProcessNode sp2 = SubProcessNode.builder()
                .name("深度2子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(
                                        "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                                                .formatted(DEPTH_3_CODE, initiator.getUserId(), saveActionId3)).getKey(),
                                true))
                        .build())
                .build();
        saveWorkflowWithDepth(DEPTH_1_CODE, "深度1", start1, sp1, maxDepth);
        saveWorkflowWithDepth(DEPTH_2_CODE, "深度2", start2, sp2, maxDepth);
        saveWorkflowWithDepth(DEPTH_3_CODE, "深度3", start3, null, maxDepth);

        Map<String, Object> data = Map.of("content", "depth");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(DEPTH_1_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start1).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        approve(factory.flowRecordRepository.get(recordId), passAction(start1), initiator, data);

        FlowRecord w2Start = todos(initiator, start2).get(0);
        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> approve(w2Start, passAction(start2), initiator, data),
                "嵌套层数超过 maxNestDepth 必须被拒绝创建");
        assertEquals("execution.subProcess.maxDepth", exception.getErrCode());
    }

    /**
     * 测试目标：合法的多层子流程嵌套（未超过 maxNestDepth）不被误伤。
     * 前置条件：三个流程 W1/W2/W3 链式嵌套，使用默认 maxNestDepth。
     * 执行步骤：依次提交 W1、W2、W3 的开始节点。
     * 期望断言：W3 的子流程正常创建并停留开始待办，全程不抛异常。
     */
    @Test
    void shouldAllowNestedSubProcessWithinDepthLimit() {
        StartNode start1 = writableStart("合法深度1开始");
        StartNode start2 = writableStart("合法深度2开始");
        StartNode start3 = writableStart("合法深度3开始");
        String saveActionId2 = saveAction(start2).id();
        String saveActionId3 = saveAction(start3).id();
        SubProcessNode sp1 = SubProcessNode.builder()
                .name("合法深度1子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(
                                        "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                                                .formatted(DEPTH_2_CODE, initiator.getUserId(), saveActionId2)).getKey(),
                                true))
                        .build())
                .build();
        SubProcessNode sp2 = SubProcessNode.builder()
                .name("合法深度2子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(
                                        "def run(request){ return request.toCreateRequest('%s', %d, '%s', request.getFormData()) }"
                                                .formatted(DEPTH_3_CODE, initiator.getUserId(), saveActionId3)).getKey(),
                                true))
                        .build())
                .build();
        saveWorkflowWithDepth(DEPTH_1_CODE, "合法深度1", start1, sp1, 10);
        saveWorkflowWithDepth(DEPTH_2_CODE, "合法深度2", start2, sp2, 10);
        saveWorkflowWithDepth(DEPTH_3_CODE, "合法深度3", start3, null, 10);

        Map<String, Object> data = Map.of("content", "valid");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(DEPTH_1_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start1).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        approve(factory.flowRecordRepository.get(recordId), passAction(start1), initiator, data);

        FlowRecord w2Start = todos(initiator, start2).get(0);
        approve(w2Start, passAction(start2), initiator, data);

        assertEquals(1, todos(initiator, start3).size(), "第三层子流程应正常创建并停留开始待办");
    }

    /**
     * 测试目标：审批节点异常触发跳转到抄送节点时必须明确报错，而不是静默停滞。
     * 前置条件：C 审批节点操作人为空，异常触发脚本返回 B 抄送节点 id。
     * 执行步骤：提交 A 开始节点，B 抄送自动完成，C 生成时触发异常跳转。
     * 期望断言：抛出 FlowExecutionException，错误码为 execution.node.invalidJumpTarget。
     */
    @Test
    void shouldRejectErrorTriggerJumpToNotifyNode() {
        StartNode start = writableStart("跳转抄送开始");
        NotifyNode notifyB = NotifyNode.builder()
                .name("B抄送")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [" + initiator.getUserId() + "] }").getKey()))
                        .build())
                .build();
        ApprovalNode approvalC = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [] }").getKey()))
                        .addStrategy(new ErrorTriggerStrategy(
                                FlowGroovyScriptFactory.createErrorTriggerScript(
                                        "def run(request){ return '" + notifyB.getId() + "' }").getKey()))
                        .build())
                .build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("异常跳转抄送拦截")
                .code("loop-guard-jump-notify")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(notifyB)
                .addNode(approvalC)
                .addNode(EndNode.builder().name("结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode("loop-guard-jump-notify");
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        FlowRecord startRecord = factory.flowRecordRepository.get(recordId);

        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> approve(startRecord, passAction(start), initiator, data),
                "异常跳转到抄送节点必须被拒绝，而不是静默停滞");
        assertEquals("execution.node.invalidJumpTarget", exception.getErrCode());
    }

    /**
     * 测试目标：拒绝动作跳转到抄送节点时必须明确报错，而不是静默停滞。
     * 前置条件：C 审批节点拒绝脚本返回 B 抄送节点 id。
     * 执行步骤：提交 A 开始节点，B 抄送自动完成，拒绝 C。
     * 期望断言：抛出 FlowExecutionException，错误码为 execution.node.invalidJumpTarget。
     */
    @Test
    void shouldRejectJumpBackToNotifyNode() {
        StartNode start = writableStart("退回抄送开始");
        NotifyNode notifyB = NotifyNode.builder()
                .name("B抄送")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [" + initiator.getUserId() + "] }").getKey()))
                        .build())
                .build();
        ApprovalNode approvalC = ApprovalNode.builder()
                .name("C审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [" + initiator.getUserId() + "] }").getKey()))
                        .build())
                .build();
        approvalC.actionManager().getActions().stream()
                .filter(action -> "REJECT".equals(action.type()))
                .findFirst()
                .ifPresent(action -> {
                    com.codingapi.flow.action.actions.RejectAction reject =
                            (com.codingapi.flow.action.actions.RejectAction) action;
                    reject.setScript(FlowGroovyScriptFactory.createActionRejectScript(
                            "def run(request){ return '" + notifyB.getId() + "' }").getKey());
                });
        Workflow workflow = WorkflowBuilder.builder()
                .title("退回抄送拦截")
                .code("loop-guard-reject-notify")
                .createdOperator(initiator)
                .form(form)
                .addNode(start)
                .addNode(notifyB)
                .addNode(approvalC)
                .addNode(EndNode.builder().name("结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode("loop-guard-reject-notify");
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        FlowRecord startRecord = factory.flowRecordRepository.get(recordId);
        approve(startRecord, passAction(start), initiator, data);

        FlowRecord cTodo = todos(initiator, approvalC).get(0);
        IFlowAction reject = approvalC.actionManager().getActions().stream()
                .filter(action -> "REJECT".equals(action.type()))
                .findFirst()
                .orElseThrow();
        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> approve(cTodo, reject, initiator, data),
                "拒绝跳转到抄送节点必须被拒绝，而不是静默停滞");
        assertEquals("execution.node.invalidJumpTarget", exception.getErrCode());
    }

    /**
     * 测试目标：errorTrigger 递归深度超过流程级 maxNestDepth 时被拦截。
     * 前置条件：A->B->C->D 四个审批节点操作人皆为空，异常脚本依次跳转下一个节点，maxNestDepth=2。
     * 执行步骤：提交开始节点触发 A 的异常跳转链。
     * 期望断言：抛出 FlowExecutionException，错误码为 execution.node.errorTriggerDepth。
     */
    @Test
    void shouldRejectErrorTriggerDepthExceeded() {
        StartNode start = writableStart("深度链开始");
        int maxNestDepth = 2;
        ApprovalNode nodeA = emptyApproval("A");
        ApprovalNode nodeB = emptyApproval("B");
        ApprovalNode nodeC = emptyApproval("C");
        ApprovalNode nodeD = emptyApproval("D");
        String scriptA = errorTriggerTo(nodeB.getId());
        String scriptB = errorTriggerTo(nodeC.getId());
        String scriptC = errorTriggerTo(nodeD.getId());
        addErrorTrigger(nodeA, scriptA);
        addErrorTrigger(nodeB, scriptB);
        addErrorTrigger(nodeC, scriptC);
        Workflow workflow = WorkflowBuilder.builder()
                .title("异常深度链检测")
                .code("loop-guard-error-depth")
                .createdOperator(initiator)
                .form(form)
                .maxNestDepth(maxNestDepth)
                .addNode(start)
                .addNode(nodeA)
                .addNode(nodeB)
                .addNode(nodeC)
                .addNode(nodeD)
                .addNode(EndNode.builder().name("结束").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode("loop-guard-error-depth");
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        FlowRecord startRecord = factory.flowRecordRepository.get(recordId);

        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> approve(startRecord, passAction(start), initiator, data),
                "errorTrigger 递归深度超过 maxNestDepth 必须被拦截");
        assertEquals("execution.node.errorTriggerDepth", exception.getErrCode());
    }

    /**
     * 测试目标：同一节点在实例链上的执行次数超过 maxNestDepth（环形状）时被拦截。
     * 前置条件：A->B->C->D，C 拒绝退回 B，maxNestDepth=1（B 仅允许执行一次）。
     * 执行步骤：提交 A 进入 B，通过 B 进入 C，拒绝 C 退回 B。
     * 期望断言：B 第二次执行时抛出 FlowExecutionException，错误码为 execution.node.loopDepth。
     */
    @Test
    void shouldRejectNodeExecutionCountExceeded() {
        StartNode start = writableStart("环检测开始");
        ApprovalNode nodeB = approvalNode("B", initiator);
        ApprovalNode nodeC = approvalNode("C", initiator);
        nodeC.actionManager().getActions().stream()
                .filter(action -> "REJECT".equals(action.type()))
                .findFirst()
                .ifPresent(action -> {
                    com.codingapi.flow.action.actions.RejectAction reject =
                            (com.codingapi.flow.action.actions.RejectAction) action;
                    reject.setScript(FlowGroovyScriptFactory.createActionRejectScript(
                            "def run(request){ return '" + nodeB.getId() + "' }").getKey());
                });
        Workflow workflow = WorkflowBuilder.builder()
                .title("节点执行次数检测")
                .code("loop-guard-node-count")
                .createdOperator(initiator)
                .form(form)
                .maxNestDepth(1)
                .addNode(start)
                .addNode(nodeB)
                .addNode(nodeC)
                .addNode(EndNode.builder().name("D").build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("content", "parent");
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode("loop-guard-node-count");
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(start).id());
        request.setFormData(data);
        long recordId = factory.flowService.create(request);
        FlowRecord startRecord = factory.flowRecordRepository.get(recordId);
        approve(startRecord, passAction(start), initiator, data);

        FlowRecord bTodo = todos(initiator, nodeB).get(0);
        approve(bTodo, passAction(nodeB), initiator, data);

        FlowRecord cTodo = todos(initiator, nodeC).get(0);
        IFlowAction reject = nodeC.actionManager().getActions().stream()
                .filter(action -> "REJECT".equals(action.type()))
                .findFirst()
                .orElseThrow();
        FlowExecutionException exception = assertThrows(FlowExecutionException.class,
                () -> approve(cTodo, reject, initiator, data),
                "节点执行次数超过 maxNestDepth 必须被拦截（环形状）");
        assertEquals("execution.node.loopDepth", exception.getErrCode());
    }

    private void saveWorkflowWithDepth(String code, String title, StartNode start,
                                       SubProcessNode subProcess, int maxDepth) {
        WorkflowBuilder builder = WorkflowBuilder.builder()
                .title(title)
                .code(code)
                .createdOperator(initiator)
                .form(form)
                .maxNestDepth(maxDepth)
                .addNode(start);
        if (subProcess != null) {
            builder.addNode(subProcess);
        }
        builder.addNode(EndNode.builder().name(title + "结束").build());
        factory.workflowService.saveWorkflow(builder.build());
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

    private ApprovalNode emptyApproval(String name) {
        return ApprovalNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission())
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [] }").getKey()))
                        .build())
                .build();
    }

    private String errorTriggerTo(String nodeId) {
        return FlowGroovyScriptFactory.createErrorTriggerScript(
                "def run(request){ return '" + nodeId + "' }").getKey();
    }

    private void addErrorTrigger(ApprovalNode node, String errorTriggerKey) {
        // 移除默认的"回退开始节点"异常策略，替换为自定义跳转目标
        node.getStrategies().removeIf(strategy -> strategy instanceof ErrorTriggerStrategy);
        node.getStrategies().add(new ErrorTriggerStrategy(errorTriggerKey));
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

    private IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActions().stream()
                .filter(action -> "PASS".equals(action.type()))
                .findFirst()
                .orElseThrow();
    }

    private IFlowAction saveAction(IFlowNode node) {
        return node.actionManager().getActions().stream()
                .filter(action -> "SAVE".equals(action.type()))
                .findFirst()
                .orElseThrow();
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