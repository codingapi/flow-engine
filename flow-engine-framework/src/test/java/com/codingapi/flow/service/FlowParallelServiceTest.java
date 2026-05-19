package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowParallelServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();


    /**
     * 并行分支测试
     */
    @Test
    void parallelAndParallel() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(depart);
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
                .build();


        ApprovalNode departApprovalNode1 = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode1 = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();

        ApprovalNode bigBossApprovalNode1 = ApprovalNode.builder()
                .name("大老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.READ)
                                .addPermission("leave", "days", PermissionType.READ)
                                .addPermission("leave", "reason", PermissionType.READ)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();


        ParallelBranchNode parallelBranchNode11 = ParallelBranchNode.builder()
                .name("并行分支1")
                .blocks(departApprovalNode1)
                .order(1)
                .build();

        ParallelBranchNode parallelBranchNode12 = ParallelBranchNode.builder()
                .name("并行分支2")
                .blocks(bossApprovalNode1, bigBossApprovalNode1)
                .order(2)
                .build();

        ParallelNode parallelNode1 = ParallelNode.builder()
                .name("并行控制节点")
                .blocks(parallelBranchNode11, parallelBranchNode12)
                .build();


        ApprovalNode departApprovalNode2 = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode2 = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();

        ApprovalNode bigBossApprovalNode2 = ApprovalNode.builder()
                .name("大老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();


        ParallelBranchNode parallelBranchNode21 = ParallelBranchNode.builder()
                .name("并行分支1")
                .blocks(departApprovalNode2)
                .order(1)
                .build();

        ParallelBranchNode parallelBranchNode22 = ParallelBranchNode.builder()
                .name("并行分支2")
                .blocks(bossApprovalNode2, bigBossApprovalNode2)
                .order(2)
                .build();

        ParallelNode parallelNode2 = ParallelNode.builder()
                .name("并行控制节点")
                .blocks(parallelBranchNode21, parallelBranchNode22)
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(parallelNode1)
                .addNode(parallelNode2)
                .addNode(endNode)
                .build();

         factory.workflowService.saveWorkflow(workflow);


        List<IFlowNode> nextNodes = workflow.nextNodes(bossApprovalNode1);
        assertEquals(1, nextNodes.size());
        assertEquals(bigBossApprovalNode1, nextNodes.get(0));


        Map<String, Object> data = Map.of("name", "lorne", "days", 3, "reason", "leave");

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

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        List<FlowRecord> boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> departActions = departApprovalNode1.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bossActions = bossApprovalNode1.actionManager().getActions();

        FlowActionRequest dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(dossRequest);


        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode1.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bigBossRequest);

        departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());


        departActions = departApprovalNode2.actionManager().getActions();

        departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());


        bossActions = bossApprovalNode2.actionManager().getActions();

        dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(dossRequest);


        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        bigBossActions = bigBossApprovalNode2.actionManager().getActions();

        bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bigBossRequest);


        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(7, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(7, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    @Test
    void parallelTest(){
        User user = new User(1, "user");
        factory.userGateway.save(user);

        String json = """
                {"updatedTime":"1779115566400","code":"3MgPSwshbC","nodes":[{"view":"default","strategies":[{"script":"// @SCRIPT_TITLE 你有一条待办\\ndef run(request){\\n    return '你有一条待办'\\n}\\n","strategyType":"NodeTitleStrategy"},{"strategyType":"FormFieldPermissionStrategy","fieldPermissions":[{"formCode":"leave","fieldCode":"desc","type":"READ"}]},{"enable":true,"type":"REVOKE_CURRENT","strategyType":"RevokeStrategy"}],"display":true,"name":"开始节点","id":"U7TBIwMsBqtPMlLM2X","type":"START","actions":[{"enable":true,"display":{"title":"通过"},"id":"A48hmH57atyK2inSb6","type":"PASS","title":"通过"},{"enable":true,"display":{"title":"保存"},"id":"WnkyxEDBEMnLtphCQw","type":"SAVE","title":"保存"}],"order":"0"},{"strategies":[],"blocks":[{"strategies":{"$ref":"$.nodes[1].strategies"},"blocks":[{"view":"default","strategies":[{"timeoutTime":"86400000","type":"REMIND","strategyType":"TimeoutStrategy"},{"type":"SEQUENCE","percent":"0.0","strategyType":"MultiOperatorAuditStrategy"},{"type":"MANUAL_PASS","strategyType":"SameOperatorAuditStrategy"},{"enable":false,"strategyType":"RecordMergeStrategy"},{"type":"RESUME","strategyType":"ResubmitStrategy"},{"signRequired":false,"adviceRequired":false,"strategyType":"AdviceStrategy"},{"script":"// @SCRIPT_TITLE 回退至开始节点\\n// @SCRIPT_META {\\"type\\":\\"node\\",\\"node\\":\\"START\\"}\\ndef run(request){\\n    return request.getStartNode().getId();\\n}\\n","strategyType":"ErrorTriggerStrategy"},{"script":"// @SCRIPT_TITLE 你有一条待办\\ndef run(request){\\n    return '你有一条待办'\\n}\\n","strategyType":"NodeTitleStrategy"},{"strategyType":"FormFieldPermissionStrategy","fieldPermissions":{"$ref":"$.nodes[1].strategies"}},{"selectType":"SCRIPT","script":"// @SCRIPT_TITLE 流程创建者\\n// @SCRIPT_META {\\"type\\":\\"creator\\"}\\ndef run(request){\\n    return [request.getCreatedOperatorId()]\\n}\\n","strategyType":"OperatorLoadStrategy"},{"enable":true,"type":"REVOKE_CURRENT","strategyType":"RevokeStrategy"}],"display":true,"name":"审批节点","id":"rNtslevbNRB2P3qPrO","type":"APPROVAL","actions":[{"enable":true,"display":{"title":"通过"},"id":"4vCA3MdH1W8fLoDufv","type":"PASS","title":"通过"},{"enable":true,"display":{"title":"拒绝"},"id":"R4Oz7Qii91QFyfJHOj","type":"REJECT","title":"拒绝","script":"// @SCRIPT_TITLE 返回开始节点\\n// @SCRIPT_META {\\"type\\":\\"START\\"}\\ndef run(request){\\n    return request.getStartNode().getId();\\n}\\n"},{"enable":true,"display":{"title":"保存"},"id":"W65JLyz9w67chUuWqK","type":"SAVE","title":"保存"},{"enable":true,"display":{"title":"加签"},"id":"5pT9hbBQiJmJRWMcKv","type":"ADD_AUDIT","title":"加签"},{"enable":true,"display":{"title":"转办"},"id":"cRNl4ls1AVm4J2UiCZ","type":"TRANSFER","title":"转办"},{"enable":true,"display":{"title":"退回"},"id":"Vk7NI8uGA7WZBqeMhp","type":"RETURN","title":"退回"},{"enable":true,"display":{"title":"委派"},"id":"0z1BK3A8OHVtOooajh","type":"DELEGATE","title":"委派"}],"order":"0"}],"display":false,"name":"并行分支节点","id":"QRYBK1RdEGNjqHCWo7","type":"PARALLEL_BRANCH","actions":{"$ref":"$.nodes[1].strategies"},"order":"1"},{"strategies":{"$ref":"$.nodes[1].strategies"},"blocks":[{"view":"default","strategies":[{"timeoutTime":"86400000","type":"REMIND","strategyType":"TimeoutStrategy"},{"type":"SEQUENCE","percent":"0.0","strategyType":"MultiOperatorAuditStrategy"},{"type":"MANUAL_PASS","strategyType":"SameOperatorAuditStrategy"},{"enable":false,"strategyType":"RecordMergeStrategy"},{"type":"RESUME","strategyType":"ResubmitStrategy"},{"signRequired":false,"adviceRequired":false,"strategyType":"AdviceStrategy"},{"script":"// @SCRIPT_TITLE 回退至开始节点\\n// @SCRIPT_META {\\"type\\":\\"node\\",\\"node\\":\\"START\\"}\\ndef run(request){\\n    return request.getStartNode().getId();\\n}\\n","strategyType":"ErrorTriggerStrategy"},{"script":"// @SCRIPT_TITLE 你有一条待办\\ndef run(request){\\n    return '你有一条待办'\\n}\\n","strategyType":"NodeTitleStrategy"},{"strategyType":"FormFieldPermissionStrategy","fieldPermissions":{"$ref":"$.nodes[1].strategies"}},{"selectType":"SCRIPT","script":"// @SCRIPT_TITLE 流程创建者\\n// @SCRIPT_META {\\"type\\":\\"creator\\"}\\ndef run(request){\\n    return [request.getCreatedOperatorId()]\\n}\\n","strategyType":"OperatorLoadStrategy"},{"enable":true,"type":"REVOKE_CURRENT","strategyType":"RevokeStrategy"}],"display":true,"name":"审批节点","id":"q6NO5sCyEmGDw0uNpp","type":"APPROVAL","actions":[{"enable":true,"display":{"title":"通过"},"id":"3MCWiENt6glv9xzea5","type":"PASS","title":"通过"},{"enable":true,"display":{"title":"拒绝"},"id":"dJnTZ9LcBum9gXc1Ba","type":"REJECT","title":"拒绝","script":"// @SCRIPT_TITLE 返回开始节点\\n// @SCRIPT_META {\\"type\\":\\"START\\"}\\ndef run(request){\\n    return request.getStartNode().getId();\\n}\\n"},{"enable":true,"display":{"title":"保存"},"id":"UvA6Wl9epOLCpcRERi","type":"SAVE","title":"保存"},{"enable":true,"display":{"title":"加签"},"id":"P8rvYisfKJsQN6xdFt","type":"ADD_AUDIT","title":"加签"},{"enable":true,"display":{"title":"转办"},"id":"MWO88KiwmDGOOd6FJ6","type":"TRANSFER","title":"转办"},{"enable":true,"display":{"title":"退回"},"id":"cLdgknFlunfTV2fiwL","type":"RETURN","title":"退回"},{"enable":true,"display":{"title":"委派"},"id":"kaGmQkZm1lDm1xpLSc","type":"DELEGATE","title":"委派"}],"order":"0"}],"display":false,"name":"并行分支节点","id":"aiDuVhF32mKgmfG17t","type":"PARALLEL_BRANCH","actions":{"$ref":"$.nodes[1].strategies"},"order":"2"}],"display":false,"name":"并行控制节点","id":"xvXK0CmCs8G7vfSNWR","type":"PARALLEL","actions":{"$ref":"$.nodes[1].strategies"},"order":"0"},{"view":"default","strategies":[{"script":"// @SCRIPT_TITLE 回退至开始节点\\n// @SCRIPT_META {\\"type\\":\\"node\\",\\"node\\":\\"START\\"}\\ndef run(request){\\n    return request.getStartNode().getId();\\n}\\n","strategyType":"ErrorTriggerStrategy"},{"script":"// @SCRIPT_TITLE 你有一条待办\\ndef run(request){\\n    return '你有一条待办'\\n}\\n","strategyType":"NodeTitleStrategy"},{"strategyType":"FormFieldPermissionStrategy","fieldPermissions":[{"formCode":"leave","fieldCode":"desc","type":"WRITE"}]},{"selectType":"SCRIPT","script":"// @SCRIPT_TITLE 流程创建者\\n// @SCRIPT_META {\\"type\\":\\"creator\\"}\\ndef run(request){\\n    return [request.getCreatedOperatorId()]\\n}\\n","strategyType":"OperatorLoadStrategy"}],"display":true,"name":"抄送节点","id":"IhWkd6SKzARfMCN2bW","type":"NOTIFY","actions":{"$ref":"$.nodes[1].strategies"},"order":"2"},{"strategies":{"$ref":"$.nodes[1].strategies"},"display":true,"name":"结束节点","id":"IH0QcEwZlODLlq9uq2","type":"END","actions":{"$ref":"$.nodes[1].strategies"},"order":"0"}],"form":{"code":"leave","name":"请假单","fields":[{"code":"desc","hidden":false,"dataType":"INTEGER","name":"天数","attributes":[],"id":"53759b8e-e622-424e-a10b-a92b4fb90ad6","placeholder":"请输入理由","type":"integer","required":true}]},"createdOperator":"1","strategies":[{"enable":true,"strategyType":"InterfereStrategy"},{"enable":true,"interval":"60","strategyType":"UrgeStrategy"}],"description":"这是一个流程的备注信息","createdTime":"1774603227796","id":"JTWgurhWfc998EMqbw","title":"请假","operatorCreateScript":"// @SCRIPT_TITLE 任意用户\\n// @SCRIPT_META {\\"type\\":\\"any\\"}\\ndef run(request){\\n    return true\\n}\\n"}
                """;

        Workflow workflow = Workflow.formJson(json);
        workflow.enable();
        factory.workflowService.saveWorkflow(workflow);

        String processId = null;

        IFlowNode startNode =  workflow.getStartNode();
        Map<String, Object> data = Map.of( "desc", 3);

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        processId = userRecordList.get(0).getProcessId();

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(2, userRecordList.size());


        for (int i=0;i<2;i++){
            FlowRecord currentRecord = userRecordList.get(i);
            IFlowNode flowNode = workflow.getFlowNode(currentRecord.getNodeId());
            userRequest = new FlowActionRequest();
            userRequest.setFormData(data);
            userRequest.setRecordId(currentRecord.getId());
            userRequest.setAdvice(new FlowAdviceBody(flowNode.actionManager().getAction(PassAction.class).id(), "同意", user.getUserId()));

            if(i==1){
                System.out.println(currentRecord);
            }
            factory.flowService.action(userRequest);
        }

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(0, userRecordList.size());

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(processId);
        assertEquals(4, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }
}