package com.codingapi.flow.service;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.AdviceStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批意见校验能力测试。
 *
 * <p>验证点：
 * <ol>
 *     <li>审批意见长度上限（500 字符），超长在请求入口被拦截</li>
 *     <li>节点配置意见必填时，退回 / 转办 / 委派同样校验意见，不再仅限通过 / 拒绝</li>
 *     <li>节点开启隐藏审批意见时，必填校验放宽，携带退回节点等必要参数即可提交</li>
 *     <li>暂存（SAVE）不校验意见，保持原有行为</li>
 * </ol>
 *
 * <p>说明：以上均为校验语义调整，不涉及任何数据结构变更（无新增表、字段或流程定义 JSON 字段）。
 */
class FlowAdviceValidationServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private User a;
    private User b;
    private User c;
    private Workflow workflow;
    private StartNode startNode;
    private ApprovalNode approvalNode;

    /**
     * 构建 A -> B(审批意见策略可配) -> E 的流程
     */
    private void buildWorkflow(AdviceStrategy adviceStrategy, String workCode) {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code(workCode)
                .addField("标题", "title", DataType.STRING)
                .build();

        startNode = StartNode.builder().build();

        approvalNode = ApprovalNode.builder()
                .name("审批节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .addStrategy(adviceStrategy)
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        workflow = WorkflowBuilder.builder()
                .title("测试流程")
                .code(workCode)
                .createdOperator(a)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);
    }

    @BeforeEach
    void setUp() {
        a = new User(1, "a");
        b = new User(2, "b");
        c = new User(3, "c");
        factory.userGateway.save(a);
        factory.userGateway.save(b);
        factory.userGateway.save(c);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    /**
     * a 发起并提交到 B 节点，返回 b 的待办记录
     */
    private FlowRecord submitToApprovalNode() {
        Map<String, Object> data = Map.of("title", "test");
        IFlowAction startAction = startNode.actionManager().getActionByType(ActionType.PASS.name());

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(a.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> aTodos = factory.flowRecordRepository.findTodoByOperator(a.getUserId());
        assertEquals(1, aTodos.size());

        FlowActionRequest aAction = new FlowActionRequest();
        aAction.setFormData(data);
        aAction.setRecordId(aTodos.get(0).getId());
        aAction.setAdvice(new FlowAdviceBody(startAction.id(), "提交", a.getUserId()));
        factory.flowService.action(aAction);

        List<FlowRecord> bTodos = factory.flowRecordRepository.findTodoByOperator(b.getUserId());
        assertEquals(1, bTodos.size());
        return bTodos.get(0);
    }

    private FlowActionRequest buildRequest(FlowRecord record, String actionType, String advice, long operatorId) {
        IFlowAction action = approvalNode.actionManager().getActionByType(actionType);
        FlowActionRequest request = new FlowActionRequest();
        request.setFormData(Map.of("title", "test"));
        request.setRecordId(record.getId());
        request.setAdvice(new FlowAdviceBody(action.id(), advice, operatorId));
        return request;
    }

    // ------------------------------------------------------------------
    // 长度校验
    // ------------------------------------------------------------------

    @Test
    void adviceBody_shouldRejectTooLongAdvice() {
        // given - 超过上限 1 个字符
        FlowAdviceBody body = new FlowAdviceBody("action-1",
                "x".repeat(FlowAdviceBody.ADVICE_MAX_LENGTH + 1), 1);

        // when / then
        FlowValidationException exception = assertThrows(FlowValidationException.class, body::verify);
        assertTrue(exception.getMessage().contains("advice"));
    }

    @Test
    void adviceBody_shouldAcceptAdviceAtMaxLength() {
        // given - 恰好达到上限
        FlowAdviceBody body = new FlowAdviceBody("action-1",
                "x".repeat(FlowAdviceBody.ADVICE_MAX_LENGTH), 1);

        // when / then
        assertDoesNotThrow(body::verify);
    }

    @Test
    void adviceBody_shouldSkipLengthCheck_whenAdviceBlank() {
        // given - 全空白内容不做长度校验（视为未填写）
        FlowAdviceBody body = new FlowAdviceBody("action-1",
                " ".repeat(FlowAdviceBody.ADVICE_MAX_LENGTH + 10), 1);

        // when / then
        assertDoesNotThrow(body::verify);
    }

    @Test
    void action_shouldRejectTooLongAdvice() {
        // given
        buildWorkflow(AdviceStrategy.defaultStrategy(), "advice-too-long");
        FlowRecord bTodo = submitToApprovalNode();

        FlowActionRequest request = buildRequest(bTodo, ActionType.PASS.name(),
                "x".repeat(FlowAdviceBody.ADVICE_MAX_LENGTH + 1), b.getUserId());

        // when / then
        assertThrows(FlowValidationException.class, () -> factory.flowService.action(request));
    }

    // ------------------------------------------------------------------
    // 意见必填校验范围
    // ------------------------------------------------------------------

    @Test
    void reject_withoutAdvice_whenRequired_shouldThrow() {
        // given - B 节点：意见必填
        buildWorkflow(new AdviceStrategy(true, false, false), "advice-reject-required");
        FlowRecord bTodo = submitToApprovalNode();
        FlowActionRequest request = buildRequest(bTodo, ActionType.REJECT.name(), null, b.getUserId());

        // when / then
        FlowValidationException exception = assertThrows(FlowValidationException.class,
                () -> factory.flowService.action(request));
        assertTrue(exception.getMessage().contains("advice"));
    }

    @Test
    void return_withoutAdvice_whenRequired_shouldThrow() {
        // given - B 节点：意见必填
        buildWorkflow(new AdviceStrategy(true, false, false), "advice-return-required");
        FlowRecord bTodo = submitToApprovalNode();

        FlowActionRequest request = buildRequest(bTodo, ActionType.RETURN.name(), null, b.getUserId());
        request.getAdvice().setBackNodeId(startNode.getId());

        // when / then
        FlowValidationException exception = assertThrows(FlowValidationException.class,
                () -> factory.flowService.action(request));
        assertTrue(exception.getMessage().contains("advice"));
    }

    @Test
    void return_withAdvice_whenRequired_shouldPass() {
        // given
        buildWorkflow(new AdviceStrategy(true, false, false), "advice-return-filled");
        FlowRecord bTodo = submitToApprovalNode();

        FlowActionRequest request = buildRequest(bTodo, ActionType.RETURN.name(), "请补充材料", b.getUserId());
        request.getAdvice().setBackNodeId(startNode.getId());

        // when / then
        assertDoesNotThrow(() -> factory.flowService.action(request));

        // 退回发起节点后，发起人产生新的待办
        assertEquals(1, factory.flowRecordRepository.findTodoByOperator(a.getUserId()).size());
    }

    @Test
    void return_withoutAdvice_whenAdviceHidden_shouldNotThrow() {
        // given - B 节点：意见必填但隐藏（必填放宽）
        buildWorkflow(new AdviceStrategy(true, false, true), "advice-return-hidden");
        FlowRecord bTodo = submitToApprovalNode();

        FlowActionRequest request = buildRequest(bTodo, ActionType.RETURN.name(), null, b.getUserId());
        request.getAdvice().setBackNodeId(startNode.getId());

        // when / then
        assertDoesNotThrow(() -> factory.flowService.action(request));
    }

    @Test
    void transfer_withoutAdvice_whenRequired_shouldThrow() {
        // given
        buildWorkflow(new AdviceStrategy(true, false, false), "advice-transfer-required");
        FlowRecord bTodo = submitToApprovalNode();

        FlowActionRequest request = buildRequest(bTodo, ActionType.TRANSFER.name(), null, b.getUserId());
        request.getAdvice().setForwardOperatorIds(List.of(c.getUserId()));

        // when / then
        FlowValidationException exception = assertThrows(FlowValidationException.class,
                () -> factory.flowService.action(request));
        assertTrue(exception.getMessage().contains("advice"));
    }

    @Test
    void delegate_withoutAdvice_whenRequired_shouldThrow() {
        // given
        buildWorkflow(new AdviceStrategy(true, false, false), "advice-delegate-required");
        FlowRecord bTodo = submitToApprovalNode();

        FlowActionRequest request = buildRequest(bTodo, ActionType.DELEGATE.name(), null, b.getUserId());
        request.getAdvice().setForwardOperatorIds(List.of(c.getUserId()));

        // when / then
        FlowValidationException exception = assertThrows(FlowValidationException.class,
                () -> factory.flowService.action(request));
        assertTrue(exception.getMessage().contains("advice"));
    }

    @Test
    void save_withoutAdvice_whenRequired_shouldNotThrow() {
        // given - 暂存操作不校验意见，保持原有行为
        buildWorkflow(new AdviceStrategy(true, false, false), "advice-save-required");
        FlowRecord bTodo = submitToApprovalNode();

        FlowActionRequest request = buildRequest(bTodo, ActionType.SAVE.name(), null, b.getUserId());

        // when / then
        assertDoesNotThrow(() -> factory.flowService.action(request));
    }

    @Test
    void pass_withoutAdvice_whenRequired_shouldThrow() {
        // given - 通过动作的校验语义保持不变
        buildWorkflow(new AdviceStrategy(true, false, false), "advice-pass-required");
        FlowRecord bTodo = submitToApprovalNode();

        FlowActionRequest request = buildRequest(bTodo, ActionType.PASS.name(), null, b.getUserId());

        // when / then
        FlowValidationException exception = assertThrows(FlowValidationException.class,
                () -> factory.flowService.action(request));
        assertTrue(exception.getMessage().contains("advice"));
    }
}
