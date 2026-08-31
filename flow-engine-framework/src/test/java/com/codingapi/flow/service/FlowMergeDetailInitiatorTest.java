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
import com.codingapi.flow.pojo.request.FlowDetailRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合并待办详情中，各合并记录的发起人（createdOperator）应保持各自流程的发起人，
 * 而不是全部等于当前打开记录（record）的发起人。
 */
public class FlowMergeDetailInitiatorTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void detail_should_preserve_each_merge_record_creator() {
        // given 两个不同发起人 + 一个审批人
        User initiator1 = new User(1, "initiator1");
        User initiator2 = new User(2, "initiator2");
        User boss = new User(3, "boss");
        factory.userGateway.save(initiator1);
        factory.userGateway.save(initiator2);
        factory.userGateway.save(boss);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [3]}").getKey()))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(initiator1)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        // when 两个不同发起人先后发起并提交，汇聚到审批人待办（合并为一条）
        for (User initiator : List.of(initiator1, initiator2)) {
            List<IFlowAction> startActions = startNode.actionManager().getActions();
            FlowCreateRequest createRequest = new FlowCreateRequest();
            createRequest.setWorkCode(workflow.getCode());
            createRequest.setFormData(data);
            createRequest.setActionId(startActions.get(0).id());
            createRequest.setOperatorId(initiator.getUserId());
            factory.flowService.create(createRequest);

            FlowRecord todoRecord = factory.flowRecordRepository.findTodoByOperator(initiator.getUserId()).get(0);
            FlowActionRequest actionRequest = new FlowActionRequest();
            actionRequest.setFormData(data);
            actionRequest.setRecordId(todoRecord.getId());
            actionRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", initiator.getUserId()));
            factory.flowService.action(actionRequest);
        }

        // 审批人待办汇聚为一条合并待办（cnt=2），但对应两条流程记录
        List<FlowRecord> bossTodoRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(2, bossTodoRecords.size());
        assertEquals(2, factory.flowTodoRecordRepository.findAll().get(0).getMargeCount());

        // when 打开合并待办详情（传入其中一条流程记录 id）
        FlowRecord opened = bossTodoRecords.get(0);
        assertEquals(2, factory.flowRecordService.getMergeRecord(opened.getTodoKey()).size(),
                "详情查询前合并关系应保持原始两条");
        FlowContent detail = factory.flowService.detail(new FlowDetailRequest(opened.getId(), boss.getUserId()));

        // then 详情应恰好包含两条合并记录，且无重复
        List<FlowContent.Body> todos = detail.getTodos();
        assertEquals(2, todos.size(), "详情应返回两条合并记录，且不得因查询详情而增多");
        assertEquals(2, todos.stream().map(FlowContent.Body::getRecordId).distinct().count(),
                "详情返回的合并记录不得重复");

        // then 每条合并记录的发起人应为各自流程自身的发起人（getMergeRecord 中的真实 creator）
        List<FlowRecord> mergeRecords = factory.flowRecordService.getMergeRecord(opened.getTodoKey());
        Map<Long, Long> recordToCreator = mergeRecords.stream()
                .collect(java.util.stream.Collectors.toMap(FlowRecord::getId, FlowRecord::getCreateOperatorId));
        assertTrue(recordToCreator.values().stream().distinct().count() == 2,
                "前置条件：两条合并记录应来自不同发起人");
        for (FlowContent.Body body : todos) {
            assertEquals(recordToCreator.get(body.getRecordId()), body.getCreatedOperator().getId(),
                    "合并记录 " + body.getRecordId() + " 的发起人应为其流程自身的发起人");
        }

        // then 查询详情不得改写入库合并状态（计数仍为 2）
        assertEquals(2, factory.flowTodoRecordRepository.findAll().get(0).getMargeCount(),
                "查询详情不得改变合并计数");
        assertEquals(2, factory.flowTodoMergeRepository.findAll().size(),
                "查询详情不得新增合并关系");
    }
}