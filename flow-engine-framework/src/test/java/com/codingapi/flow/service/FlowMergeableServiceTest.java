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
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowMergeableServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();


    /**
     * 合并记录测试
     */
    @Test
    void mergeableRecords() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
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
                        .addAction(new CustomAction())
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");


        int count = 5;
        for (int i = 0; i < count; i++) {
            List<IFlowAction> startActions = startNode.actionManager().getActions();
            FlowCreateRequest userCreateRequest = new FlowCreateRequest();
            userCreateRequest.setWorkId(workflow.getId());
            userCreateRequest.setFormData(data);
            userCreateRequest.setActionId(startActions.get(0).id());
            userCreateRequest.setOperatorId(user.getUserId());
            factory.flowService.create(userCreateRequest);

            List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
            assertEquals(1, userRecordList.size());

            FlowActionRequest userRequest = new FlowActionRequest();
            userRequest.setFormData(data);
            userRequest.setRecordId(userRecordList.get(0).getId());
            userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
            factory.flowService.action(userRequest);
        }


        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(count, bossRecordList.size());
        assertEquals(count, bossRecordList.stream().filter(FlowRecord::isMergeable).toList().size());

        List<String> mergeIdList = bossRecordList.stream().map(FlowRecord::getTodoKey).toList();
        Set<String> set = new HashSet<>(mergeIdList);
        assertEquals(1,set.size());

        List<FlowTodoRecord> todoRecords = factory.flowTodoRecordRepository.findByOperatorId(boss.getUserId());
        assertEquals(1, todoRecords.size());

        FlowTodoRecord todoMargeRecord = todoRecords.get(0);
        List<FlowTodoMerge> mergeList = factory.flowTodoMergeRepository.findByTodoId(todoMargeRecord.getId());
        assertEquals(count, mergeList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        for(int i=0;i<count;i++){
            FlowActionRequest bossRequest = new FlowActionRequest();
            bossRequest.setFormData(data);
            bossRequest.setRecordId(bossRecordList.get(i).getId());
            bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
            factory.flowService.action(bossRequest);
        }

        for(int i=0;i<count;i++) {
            List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(i).getProcessId());
            assertEquals(2, records.size());
            assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());
        }

        List<FlowTodoRecord> todoRecordList = factory.flowTodoRecordRepository.findAll();
        assertEquals(0, todoRecordList.size());

        List<FlowTodoMerge> todoMargeList = factory.flowTodoMergeRepository.findAll();
        assertEquals(0, todoMargeList.size());
     }

}
