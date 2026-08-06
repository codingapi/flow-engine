package com.codingapi.flow.service;

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
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.response.FlowContent;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 节点级隐藏审批意见能力测试（issue #180）。
 *
 * <p>场景：一些流程中审批意见非必填，甚至不需要审批意见。
 * 节点支持配置隐藏审批意见，隐藏后审批时不再展示审批意见输入框。
 *
 * <p>验证点：
 * <ol>
 *     <li>AdviceStrategy 支持 adviceHidden 配置，可序列化/反序列化，兼容无该字段的存量数据</li>
 *     <li>详情接口按节点下发 adviceHidden</li>
 *     <li>隐藏审批意见时必填校验放宽：即使配置了意见必填，未填写意见也可正常审批，避免流程卡死</li>
 * </ol>
 */
class FlowAdviceHiddenServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private User a;
    private User b;
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
        factory.userGateway.save(a);
        factory.userGateway.save(b);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
    }

    /**
     * a 发起并提交到 B 节点，返回 b 的待办记录
     */
    private FlowRecord submitToApprovalNode() {
        Map<String, Object> data = Map.of("title", "test");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(a.getUserId());
        factory.flowService.create(createRequest);

        List<FlowRecord> aTodos = factory.flowRecordRepository.findTodoByOperator(a.getUserId());
        assertEquals(1, aTodos.size());

        FlowActionRequest aAction = new FlowActionRequest();
        aAction.setFormData(data);
        aAction.setRecordId(aTodos.get(0).getId());
        aAction.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "提交", a.getUserId()));
        factory.flowService.action(aAction);

        List<FlowRecord> bTodos = factory.flowRecordRepository.findTodoByOperator(b.getUserId());
        assertEquals(1, bTodos.size());
        return bTodos.get(0);
    }

    /**
     * AdviceStrategy 序列化应包含 adviceHidden；
     * 反序列化兼容无 adviceHidden 字段的存量数据（默认 false）。
     */
    @Test
    void adviceStrategy_shouldSerializeAdviceHidden_andBeBackwardCompatible() {
        // given - 开启隐藏意见的策略
        AdviceStrategy strategy = new AdviceStrategy();
        strategy.setAdviceRequired(true);
        strategy.setSignRequired(false);
        strategy.setAdviceHidden(true);

        // when - 序列化
        Map<String, Object> map = strategy.toMap();

        // then - 包含 adviceHidden
        assertEquals(true, map.get("adviceHidden"));

        // when - 反序列化
        AdviceStrategy restored = AdviceStrategy.fromMap(map);

        // then
        assertTrue(restored.isAdviceHidden());
        assertTrue(restored.isAdviceRequired());

        // given - 存量数据无 adviceHidden 字段
        map.remove("adviceHidden");

        // when - 反序列化
        AdviceStrategy backwardCompatible = AdviceStrategy.fromMap(map);

        // then - 默认不隐藏
        assertFalse(backwardCompatible.isAdviceHidden());
        assertTrue(backwardCompatible.isAdviceRequired());
    }

    /**
     * 节点配置隐藏审批意见后，详情接口应下发 adviceHidden=true；
     * 且意见必填被放宽，adviceRequired 下发为 false。
     */
    @Test
    void detail_shouldReturnAdviceHidden_whenNodeConfiguredHidden() {
        // given - B 节点配置：意见必填 + 隐藏意见
        buildWorkflow(new AdviceStrategy(true, false, true), "advice-hidden-detail");
        FlowRecord bTodo = submitToApprovalNode();

        // when - b 查看流程详情
        FlowContent detail = factory.flowService.detail(new FlowDetailRequest(bTodo.getId(), b.getUserId()));

        // then - 下发隐藏意见配置
        assertTrue(detail.isAdviceHidden(), "节点配置隐藏审批意见后，详情接口应下发 adviceHidden=true");
        // 隐藏意见时必填不生效，避免用户无法填写意见导致流程卡死
        assertFalse(detail.isAdviceRequired(), "隐藏审批意见时，意见必填应被放宽为 false");
    }

    /**
     * 未配置隐藏意见时，详情接口下发 adviceHidden=false。
     */
    @Test
    void detail_shouldReturnAdviceNotHidden_byDefault() {
        // given - B 节点默认策略
        buildWorkflow(AdviceStrategy.defaultStrategy(), "advice-visible-detail");
        FlowRecord bTodo = submitToApprovalNode();

        // when - b 查看流程详情
        FlowContent detail = factory.flowService.detail(new FlowDetailRequest(bTodo.getId(), b.getUserId()));

        // then
        assertFalse(detail.isAdviceHidden());
    }

    /**
     * 隐藏审批意见时，即使节点配置了意见必填，未填写意见也可正常审批，流程正常结束。
     */
    @Test
    void action_shouldPassWithoutAdvice_whenAdviceHidden() {
        // given - B 节点配置：意见必填 + 隐藏意见
        buildWorkflow(new AdviceStrategy(true, false, true), "advice-hidden-action");
        FlowRecord bTodo = submitToApprovalNode();
        Map<String, Object> data = Map.of("title", "test");

        // when - b 不填写意见直接审批通过
        List<IFlowAction> actions = approvalNode.actionManager().getActions();
        FlowActionRequest bAction = new FlowActionRequest();
        bAction.setFormData(data);
        bAction.setRecordId(bTodo.getId());
        bAction.setAdvice(new FlowAdviceBody(actions.get(0).id(), null, b.getUserId()));
        factory.flowService.action(bAction);

        // then - 流程正常结束
        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bTodo.getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).count());
    }

    /**
     * 对照：未隐藏意见且意见必填时，未填写意见审批应报错。
     */
    @Test
    void action_shouldFailWithoutAdvice_whenAdviceRequiredAndNotHidden() {
        // given - B 节点配置：意见必填、不隐藏
        buildWorkflow(new AdviceStrategy(true, false, false), "advice-required-action");
        FlowRecord bTodo = submitToApprovalNode();
        Map<String, Object> data = Map.of("title", "test");

        // when - b 不填写意见直接审批通过
        List<IFlowAction> actions = approvalNode.actionManager().getActions();
        FlowActionRequest bAction = new FlowActionRequest();
        bAction.setFormData(data);
        bAction.setRecordId(bTodo.getId());
        bAction.setAdvice(new FlowAdviceBody(actions.get(0).id(), null, b.getUserId()));

        // then - 应抛出校验异常
        assertThrows(FlowValidationException.class, () -> factory.flowService.action(bAction));
    }
}
