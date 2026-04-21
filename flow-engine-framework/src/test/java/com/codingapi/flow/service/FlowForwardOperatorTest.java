package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 转接人功能测试
 */
class FlowForwardOperatorTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private User creator;
    private User operator;
    private User forwardUser;
    private Workflow workflow;
    private StartNode startNode;
    private ApprovalNode approvalNode;

    @BeforeEach
    void setUp() {
        creator = new User(1L, "creator");
        operator = new User(2L, "operator");
        forwardUser = new User(3L, "forward");

        factory.userGateway.save(creator);
        factory.userGateway.save(operator);
        factory.userGateway.save(forwardUser);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code("test")
                .addField("金额", "amount", DataType.INTEGER)
                .build();

        startNode = StartNode.builder().build();

        approvalNode = ApprovalNode.builder()
                .name("审批节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        workflow = WorkflowBuilder.builder()
                .title("测试流程")
                .code("test")
                .createdOperator(creator)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);
    }

    @Test
    void shouldNotUseForwardOperator_whenFirstCreateFlowRecord() {
        // given - creator 创建流程
        Map<String, Object> data = Map.of("amount", 100);

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkId(workflow.getId());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(creator.getUserId());

        factory.flowService.create(createRequest);

        // then - 首次创建不使用转接人（creator 没有设置 forwardOperator）
        List<FlowRecord> records = factory.flowRecordRepository.findTodoByOperator(creator.getUserId());
        assertEquals(1, records.size());
        FlowRecord record = records.get(0);
        assertEquals(creator.getUserId(), record.getCurrentOperatorId());
        assertEquals(0, record.getForwardOperatorId());
    }

    @Test
    void shouldUseForwardOperator_whenApprovalFlow() {
        // given - 创建一个有 forwardOperator 的 user 作为审批人
        User boss = new User(4L, "boss", false, forwardUser);
        factory.userGateway.save(boss);

        // 修改 approvalNode 的操作人加载脚本
        approvalNode = ApprovalNode.builder()
                .name("审批节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [4]}"))  // 返回 boss
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        workflow = WorkflowBuilder.builder()
                .title("测试流程")
                .code("test2")
                .createdOperator(creator)
                .form(workflow.getForm())
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        // 创建流程
        Map<String, Object> data = Map.of("amount", 100);
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkId(workflow.getId());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(creator.getUserId());
        factory.flowService.create(createRequest);

        // creator 通过
        List<FlowRecord> creatorRecords = factory.flowRecordRepository.findTodoByOperator(creator.getUserId());
        assertEquals(1, creatorRecords.size());

        FlowActionRequest passRequest = new FlowActionRequest();
        passRequest.setFormData(data);
        passRequest.setRecordId(creatorRecords.get(0).getId());
        passRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", creator.getUserId()));
        factory.flowService.action(passRequest);

        // then - boss 执行后，forwardUser 收到待办（因为 boss.forwardOperator() 返回 forwardUser）
        List<FlowRecord> forwardRecords = factory.flowRecordRepository.findTodoByOperator(forwardUser.getUserId());
        assertEquals(1, forwardRecords.size());
        assertEquals(forwardUser.getUserId(), forwardRecords.get(0).getCurrentOperatorId());
        assertEquals(boss.getUserId(), forwardRecords.get(0).getForwardOperatorId());
    }

    @Test
    void shouldNotForward_whenForwardOperatorReturnsNull() {
        // given - 创建一个 forwardOperator 返回 null 的 boss
        User bossWithNullForward = new User(5L, "boss", false) {
            @Override
            public IFlowOperator forwardOperator(FlowSession flowSession) {
                return null;  // 不转接
            }
        };
        factory.userGateway.save(bossWithNullForward);

        // 修改 approvalNode 的操作人加载脚本
        approvalNode = ApprovalNode.builder()
                .name("审批节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [5]}"))  // 返回 bossWithNullForward
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        workflow = WorkflowBuilder.builder()
                .title("测试流程")
                .code("test3")
                .createdOperator(creator)
                .form(workflow.getForm())
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        // 创建流程
        Map<String, Object> data = Map.of("amount", 100);
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkId(workflow.getId());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(creator.getUserId());
        factory.flowService.create(createRequest);

        // creator 通过
        List<FlowRecord> creatorRecords = factory.flowRecordRepository.findTodoByOperator(creator.getUserId());
        FlowActionRequest passRequest = new FlowActionRequest();
        passRequest.setFormData(data);
        passRequest.setRecordId(creatorRecords.get(0).getId());
        passRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", creator.getUserId()));
        factory.flowService.action(passRequest);

        // then - bossWithNullForward 收到待办（因为 forwardOperator 返回 null）
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(bossWithNullForward.getUserId());
        assertEquals(1, bossRecords.size());
        assertEquals(bossWithNullForward.getUserId(), bossRecords.get(0).getCurrentOperatorId());
        assertEquals(0, bossRecords.get(0).getForwardOperatorId());
    }

    @Test
    void shouldUseForwardOperator_withFormDataCondition() {
        // given - 创建一个根据表单数据决定是否转接的 boss
        User conditionalBoss = new User(6L, "boss", false, forwardUser) {
            @Override
            public IFlowOperator forwardOperator(FlowSession flowSession) {
                Object amount = flowSession.getFormData("amount");
                if (amount != null && ((Number) amount).doubleValue() > 10000) {
                    return forwardUser;  // 金额大于 10000 时转接
                }
                return null;  // 否则不转接
            }
        };
        factory.userGateway.save(conditionalBoss);

        // 修改 approvalNode 的操作人加载脚本
        approvalNode = ApprovalNode.builder()
                .name("审批节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [6]}"))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        workflow = WorkflowBuilder.builder()
                .title("测试流程")
                .code("test4")
                .createdOperator(creator)
                .form(workflow.getForm())
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        // 创建流程 - 金额大于 10000
        Map<String, Object> data = Map.of("amount", 15000);
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkId(workflow.getId());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(creator.getUserId());
        factory.flowService.create(createRequest);

        // creator 通过
        List<FlowRecord> creatorRecords = factory.flowRecordRepository.findTodoByOperator(creator.getUserId());
        FlowActionRequest passRequest = new FlowActionRequest();
        passRequest.setFormData(data);
        passRequest.setRecordId(creatorRecords.get(0).getId());
        passRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", creator.getUserId()));
        factory.flowService.action(passRequest);

        // then - 金额大于 10000，转接给 forwardUser
        List<FlowRecord> forwardRecords = factory.flowRecordRepository.findTodoByOperator(forwardUser.getUserId());
        assertEquals(1, forwardRecords.size());
        assertEquals(forwardUser.getUserId(), forwardRecords.get(0).getCurrentOperatorId());
        assertEquals(conditionalBoss.getUserId(), forwardRecords.get(0).getForwardOperatorId());
    }

    @Test
    void shouldNotForward_whenFormDataConditionNotMet() {
        // given - 创建一个根据表单数据决定是否转接的 boss
        User conditionalBoss = new User(7L, "boss", false, forwardUser) {
            @Override
            public IFlowOperator forwardOperator(FlowSession flowSession) {
                Object amount = flowSession.getFormData("amount");
                if (amount != null && ((Number) amount).doubleValue() > 10000) {
                    return forwardUser;
                }
                return null;
            }
        };
        factory.userGateway.save(conditionalBoss);

        // 修改 approvalNode 的操作人加载脚本
        approvalNode = ApprovalNode.builder()
                .name("审批节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [7]}"))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        workflow = WorkflowBuilder.builder()
                .title("测试流程")
                .code("test5")
                .createdOperator(creator)
                .form(workflow.getForm())
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        // 创建流程 - 金额小于 10000
        Map<String, Object> data = Map.of("amount", 5000);
        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkId(workflow.getId());
        createRequest.setFormData(data);
        createRequest.setActionId(startActions.get(0).id());
        createRequest.setOperatorId(creator.getUserId());
        factory.flowService.create(createRequest);

        // creator 通过
        List<FlowRecord> creatorRecords = factory.flowRecordRepository.findTodoByOperator(creator.getUserId());
        FlowActionRequest passRequest = new FlowActionRequest();
        passRequest.setFormData(data);
        passRequest.setRecordId(creatorRecords.get(0).getId());
        passRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", creator.getUserId()));
        factory.flowService.action(passRequest);

        // then - 金额小于 10000，不转接，conditionalBoss 收到待办
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(conditionalBoss.getUserId());
        assertEquals(1, bossRecords.size());
        assertEquals(conditionalBoss.getUserId(), bossRecords.get(0).getCurrentOperatorId());
        assertEquals(0, bossRecords.get(0).getForwardOperatorId());
    }
}
