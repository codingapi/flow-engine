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
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.pojo.request.FlowUrgeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.script.runtime.IBeanFactory;
import com.codingapi.flow.strategy.node.ErrorTriggerStrategy;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RouterStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowSampleServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void create() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
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

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());
    }


    @Test
    void importWorkflow() {
        String data = "data:application/json;base64,eyJ1cGRhdGVkVGltZSI6IjE3NzMzNjE4MjI1NjAiLCJjb2RlIjoieVN2Vmw3NTV6NSIsIm5vZGVzIjpbeyJ2aWV3IjoiZGVmYXVsdCIsInN0cmF0ZWdpZXMiOlt7InNjcmlwdCI6Ii8vIEBTQ1JJUFRfVElUTEUg5L2g5pyJ5LiA5p2h5b6F5YqeXG5kZWYgcnVuKHJlcXVlc3Qpe1xuICAgIHJldHVybiAn5L2g5pyJ5LiA5p2h5b6F5YqeJ1xufVxuIiwic3RyYXRlZ3lUeXBlIjoiTm9kZVRpdGxlU3RyYXRlZ3kifSx7InN0cmF0ZWd5VHlwZSI6IkZvcm1GaWVsZFBlcm1pc3Npb25TdHJhdGVneSIsImZpZWxkUGVybWlzc2lvbnMiOltdfSx7ImVuYWJsZSI6dHJ1ZSwidHlwZSI6IlJFVk9LRV9DVVJSRU5UIiwic3RyYXRlZ3lUeXBlIjoiUmV2b2tlU3RyYXRlZ3kifV0sImRpc3BsYXkiOnRydWUsIm5hbWUiOiLlvIDlp4voioLngrkiLCJpZCI6Ik1Qa2pIMUZRWThPVm9ha2pNNSIsInR5cGUiOiJTVEFSVCIsImFjdGlvbnMiOlt7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6IumAmui/hyJ9LCJpZCI6IjdmcFdzdmdSQnpuTFRHN05ocCIsInR5cGUiOiJQQVNTIiwidGl0bGUiOiLpgJrov4cifSx7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6IuS/neWtmCJ9LCJpZCI6InlkZWNoNXQ0M1AxcWdmclBnNSIsInR5cGUiOiJTQVZFIiwidGl0bGUiOiLkv53lrZgifV0sIm9yZGVyIjoiMCJ9LHsidmlldyI6ImRlZmF1bHQiLCJzdHJhdGVnaWVzIjpbeyJ0aW1lb3V0VGltZSI6Ijg2NDAwMDAwIiwidHlwZSI6IlJFTUlORCIsInN0cmF0ZWd5VHlwZSI6IlRpbWVvdXRTdHJhdGVneSJ9LHsidHlwZSI6IlNFUVVFTkNFIiwicGVyY2VudCI6IjAuMCIsInN0cmF0ZWd5VHlwZSI6Ik11bHRpT3BlcmF0b3JBdWRpdFN0cmF0ZWd5In0seyJ0eXBlIjoiQVVUT19QQVNTIiwic3RyYXRlZ3lUeXBlIjoiU2FtZU9wZXJhdG9yQXVkaXRTdHJhdGVneSJ9LHsiZW5hYmxlIjpmYWxzZSwic3RyYXRlZ3lUeXBlIjoiUmVjb3JkTWVyZ2VTdHJhdGVneSJ9LHsidHlwZSI6IlJFU1VNRSIsInN0cmF0ZWd5VHlwZSI6IlJlc3VibWl0U3RyYXRlZ3kifSx7InNpZ25SZXF1aXJlZCI6ZmFsc2UsImFkdmljZVJlcXVpcmVkIjpmYWxzZSwic3RyYXRlZ3lUeXBlIjoiQWR2aWNlU3RyYXRlZ3kifSx7InNjcmlwdCI6Ii8vIEBTQ1JJUFRfVElUTEUg5Zue6YCA6Iez5byA5aeL6IqC54K5XG4vLyBAU0NSSVBUX01FVEEge1widHlwZVwiOlwibm9kZVwiLFwibm9kZVwiOlwiU1RBUlRcIn1cbmRlZiBydW4ocmVxdWVzdCl7XG4gICAgcmV0dXJuICRiaW5kLmNyZWF0ZUVycm9yVGhyb3cocmVxdWVzdC5nZXRTdGFydE5vZGUoKSk7XG59XG4iLCJzdHJhdGVneVR5cGUiOiJFcnJvclRyaWdnZXJTdHJhdGVneSJ9LHsic2NyaXB0IjoiLy8gQFNDUklQVF9USVRMRSDkvaDmnInkuIDmnaHlvoXlip5cbmRlZiBydW4ocmVxdWVzdCl7XG4gICAgcmV0dXJuICfkvaDmnInkuIDmnaHlvoXlip4nXG59XG4iLCJzdHJhdGVneVR5cGUiOiJOb2RlVGl0bGVTdHJhdGVneSJ9LHsic3RyYXRlZ3lUeXBlIjoiRm9ybUZpZWxkUGVybWlzc2lvblN0cmF0ZWd5IiwiZmllbGRQZXJtaXNzaW9ucyI6eyIkcmVmIjoiJC5ub2Rlc1swXS5zdHJhdGVnaWVzWzFdLmZpZWxkUGVybWlzc2lvbnMifX0seyJzY3JpcHQiOiIvLyBAU0NSSVBUX1RJVExFIOa1geeoi+WIm+W7uuiAhVxuLy8gQFNDUklQVF9NRVRBIHtcInR5cGVcIjpcImNyZWF0b3JcIn1cbmRlZiBydW4ocmVxdWVzdCl7XG4gICAgcmV0dXJuIFtyZXF1ZXN0LmdldENyZWF0ZWRPcGVyYXRvcigpXVxufVxuIiwic3RyYXRlZ3lUeXBlIjoiT3BlcmF0b3JMb2FkU3RyYXRlZ3kifSx7ImVuYWJsZSI6dHJ1ZSwidHlwZSI6IlJFVk9LRV9DVVJSRU5UIiwic3RyYXRlZ3lUeXBlIjoiUmV2b2tlU3RyYXRlZ3kifV0sImRpc3BsYXkiOnRydWUsIm5hbWUiOiLlrqHmibnoioLngrkiLCJpZCI6InpYT05yamxrT3Y5dlNoN0V1WSIsInR5cGUiOiJBUFBST1ZBTCIsImFjdGlvbnMiOlt7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6IumAmui/hyJ9LCJpZCI6IkdjTmQzUHFhTzFKUllXV3pLbyIsInR5cGUiOiJQQVNTIiwidGl0bGUiOiLpgJrov4cifSx7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6IuaLkue7nSJ9LCJpZCI6IjRWSDFvS3g3NW41UnlIa2ZaNyIsInR5cGUiOiJSRUpFQ1QiLCJ0aXRsZSI6IuaLkue7nSIsInNjcmlwdCI6Ii8vIEBTQ1JJUFRfVElUTEUg6L+U5Zue5byA5aeL6IqC54K5XG4vLyBAU0NSSVBUX01FVEEge1widHlwZVwiOlwiU1RBUlRcIn1cbmRlZiBydW4ocmVxdWVzdCl7XG4gICAgcmV0dXJuIHJlcXVlc3QuZ2V0U3RhcnROb2RlKCkuZ2V0SWQoKTtcbn1cbiJ9LHsiZW5hYmxlIjp0cnVlLCJkaXNwbGF5Ijp7InRpdGxlIjoi5L+d5a2YIn0sImlkIjoiZVhUSXZvbHgyQWtrZndLVzAyIiwidHlwZSI6IlNBVkUiLCJ0aXRsZSI6IuS/neWtmCJ9LHsiZW5hYmxlIjp0cnVlLCJkaXNwbGF5Ijp7InRpdGxlIjoi5Yqg562+In0sImlkIjoiRm9zZVUzNVpZT3c3U0pRZk5WIiwidHlwZSI6IkFERF9BVURJVCIsInRpdGxlIjoi5Yqg562+In0seyJlbmFibGUiOnRydWUsImRpc3BsYXkiOnsidGl0bGUiOiLovazlip4ifSwiaWQiOiJ3VkRRRnhFUzhtMGdDV1lnajMiLCJ0eXBlIjoiVFJBTlNGRVIiLCJ0aXRsZSI6Iui9rOWKniJ9LHsiZW5hYmxlIjp0cnVlLCJkaXNwbGF5Ijp7InRpdGxlIjoi6YCA5ZueIn0sImlkIjoiWjk1WEk0QUFnejVlYXdhSGw0IiwidHlwZSI6IlJFVFVSTiIsInRpdGxlIjoi6YCA5ZueIn0seyJlbmFibGUiOnRydWUsImRpc3BsYXkiOnsidGl0bGUiOiLlp5TmtL4ifSwiaWQiOiJXSGoyU3pnRXJLNDdFVHFZcUciLCJ0eXBlIjoiREVMRUdBVEUiLCJ0aXRsZSI6IuWnlOa0viJ9XSwib3JkZXIiOiIwIn0seyJzdHJhdGVnaWVzIjp7IiRyZWYiOiIkLm5vZGVzWzBdLnN0cmF0ZWdpZXNbMV0uZmllbGRQZXJtaXNzaW9ucyJ9LCJkaXNwbGF5Ijp0cnVlLCJuYW1lIjoi57uT5p2f6IqC54K5IiwiaWQiOiJBRDBGRVE0VzRlVGtST0hDVzEiLCJ0eXBlIjoiRU5EIiwiYWN0aW9ucyI6eyIkcmVmIjoiJC5ub2Rlc1swXS5zdHJhdGVnaWVzWzFdLmZpZWxkUGVybWlzc2lvbnMifSwib3JkZXIiOiIwIn1dLCJmb3JtIjp7ImNvZGUiOiJsZWF2ZSIsIm5hbWUiOiLor7flgYfljZUiLCJmaWVsZHMiOlt7ImNvZGUiOiJkYXlzIiwiaGlkZGVuIjpmYWxzZSwiZGF0YVR5cGUiOiJOVU1CRVIiLCJuYW1lIjoi5aSp5pWwIiwiaWQiOiI0ZmM4YzVlMi0zOGE4LTRhNzItODJhMy1kOWM2OWNjMmIxYmUiLCJwbGFjZWhvbGRlciI6Iuivt+i+k+WFpeWkqeaVsCIsInR5cGUiOiJudW1iZXIiLCJyZXF1aXJlZCI6dHJ1ZX0seyJjb2RlIjoiZGVzYyIsImhpZGRlbiI6ZmFsc2UsImRhdGFUeXBlIjoiU1RSSU5HIiwibmFtZSI6IueQhueUsSIsImlkIjoiMWJlNjAzMzgtY2QzYy00NDFiLWIwYjctMzNjYmIzMjBmOTgyIiwicGxhY2Vob2xkZXIiOiLor7fovpPlhaXnkIbnlLEiLCJ0eXBlIjoic3RyaW5nIiwicmVxdWlyZWQiOnRydWV9XX0sImNyZWF0ZWRPcGVyYXRvciI6IjEiLCJzdHJhdGVnaWVzIjpbeyJlbmFibGUiOnRydWUsInN0cmF0ZWd5VHlwZSI6IkludGVyZmVyZVN0cmF0ZWd5In0seyJlbmFibGUiOnRydWUsImludGVydmFsIjoiNjAiLCJzdHJhdGVneVR5cGUiOiJVcmdlU3RyYXRlZ3kifV0sImNyZWF0ZWRUaW1lIjoiMTc3MzM2MTI2OTg0OSIsImlkIjoiem9jd0ptWW1lOURFRlBvQTA4IiwidGl0bGUiOiLor7flgYfmtYHnqIsiLCJvcGVyYXRvckNyZWF0ZVNjcmlwdCI6Ii8vIEBTQ1JJUFRfVElUTEUg5Lu75oSP55So5oi3XG4vLyBAU0NSSVBUX01FVEEge1widHlwZVwiOlwiYW55XCJ9XG5kZWYgcnVuKHJlcXVlc3Qpe1xuICAgIHJldHVybiB0cnVlXG59XG4ifQ==";
        String workId = factory.workflowService.importWorkflow(data);
        Workflow workflow = factory.workflowService.getWorkflow(workId);
        assertEquals("leave", workflow.getForm().getCode());
        assertEquals("请假流程", workflow.getTitle());
    }


    /**
     * 全部通过测试
     */
    @Test
    void pass() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        assertEquals(user.getUserId(), bossRecordList.get(0).getSubmitOperatorId());
        assertEquals(user.getName(), bossRecordList.get(0).getSubmitOperatorName());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 办理节点测试
     */
    @Test
    void handle() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        HandleNode bossNode = HandleNode.builder()
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
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), null, user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 抄送节点测试
     */
    @Test
    void notifyNode() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        NotifyNode bossNode = NotifyNode.builder()
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
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), null, user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(userRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());
    }


    /**
     * 条件分支测试
     */
    @Test
    void condition() {

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
                .addField("请假天数", "days", DataType.NUMBER)
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

        ApprovalNode departApprovalNode = ApprovalNode.builder()
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

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("经理审批")
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


        ConditionBranchNode departConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .blocks(departApprovalNode)
                .build();

        ConditionBranchNode bossConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') > 3}")
                .order(2)
                .blocks(bossApprovalNode)
                .build();

        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制")
                .blocks(departConditionNode, bossConditionNode)
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(conditionNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

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


        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 拒绝测试
     */
    @Test
    void reject() {

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
        FlowScriptContext.getInstance().setBeanFactory(new IBeanFactory() {
            @Override
            public FlowRecord getRecordById(long id) {
                return factory.flowRecordRepository.get(id);
            }
        });

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(1).id(), "不同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> userToDoList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userToDoList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userToDoList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());
    }


    /**
     * 并行分支测试
     */
    @Test
    void parallel() {

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
                .addField("请假天数", "days", DataType.NUMBER)
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


        ApprovalNode departApprovalNode = ApprovalNode.builder()
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

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
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

        ApprovalNode bigBossApprovalNode = ApprovalNode.builder()
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


        ParallelBranchNode parallelBranchNode1 = ParallelBranchNode.builder()
                .name("并行分支1")
                .blocks(departApprovalNode)
                .order(1)
                .build();

        ParallelBranchNode parallelBranchNode2 = ParallelBranchNode.builder()
                .name("并行分支2")
                .blocks(bossApprovalNode, bigBossApprovalNode)
                .order(2)
                .build();

        ParallelNode parallelNode = ParallelNode.builder()
                .name("并行控制节点")
                .blocks(parallelBranchNode1, parallelBranchNode2)
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(parallelNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

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

        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bossActions = bossApprovalNode.actionManager().getActions();

        FlowActionRequest dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(dossRequest);


        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bigBossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 包容分支测试
     */
    @Test
    void inclusive() {

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
                .addField("请假天数", "days", DataType.NUMBER)
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


        ApprovalNode departApprovalNode = ApprovalNode.builder()
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

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
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

        ApprovalNode bigBossApprovalNode = ApprovalNode.builder()
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


        InclusiveBranchNode parallelBranchNode1 = InclusiveBranchNode.builder()
                .name("包容分支1")
                .conditionScript("def run(request){return true}")
                .blocks(departApprovalNode)
                .order(1)
                .build();

        InclusiveBranchNode parallelBranchNode2 = InclusiveBranchNode.builder()
                .name("包容分支2")
                .conditionScript("def run(request){return request.getFormData('days') >= 3}")
                .blocks(bossApprovalNode, bigBossApprovalNode)
                .order(2)
                .build();

        InclusiveNode inclusiveNode = InclusiveNode.builder()
                .name("包容控制")
                .blocks(parallelBranchNode1, parallelBranchNode2)
                .build();


        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(inclusiveNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

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


        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bossActions = bossApprovalNode.actionManager().getActions();

        FlowActionRequest dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(dossRequest);


        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bigBossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 路由节点测试
     */
    @Test
    void router() {

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
                .addField("请假天数", "days", DataType.NUMBER)
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

        RouterNode routerNode = RouterNode.builder()
                .name("路由节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new RouterStrategy(String.format("def run(request){return '%s'}", bossNode.getId())))
                        .build())
                .build();

        ApprovalNode departNode = ApprovalNode.builder()
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


        ConditionBranchNode departConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .blocks(departNode, routerNode)
                .build();

        ConditionBranchNode bossConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') > 3}")
                .order(2)
                .blocks(bossNode)
                .build();

        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制节点")
                .blocks(departConditionNode, bossConditionNode)
                .build();


        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(conditionNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 2, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);

        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest submitRequest = new FlowActionRequest();
        submitRequest.setFormData(data);
        submitRequest.setRecordId(userRecordList.get(0).getId());
        submitRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(submitRequest);

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());


        List<IFlowAction> departActions = departNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);


        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(userRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 全部通过测试
     */
    @Test
    void delay() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        DelayNode delayNode = DelayNode.builder()
                .name("延迟节点")
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
                .addNode(delayNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());
        try {
            // 默认等待时间为5秒
            Thread.sleep(8000);
        } catch (Exception ignore) {
        }

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 触发节点测试
     */
    @Test
    void trigger() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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
                        .build()
                )
                .build();

        TriggerNode triggerNode = TriggerNode.builder()
                .name("触发流程")
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(triggerNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 子流程节点测试
     */
    @Test
    void subProcess() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        SubProcessNode subProcessNode = SubProcessNode.builder()
                .name("子流程")
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
                .addNode(subProcessNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = userCreateRequest.toActionRequest(userRecordList.get(0).getId());
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

        // 为老板再次创建一个待办流程
        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

    }


    /**
     * 保存测试
     */
    @Test
    void save() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        // 保存数据
        data.put("reason", "test");
        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(1).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        Map<String, Object> currentFormData = userRecordList.get(0).getFormData();
        assertEquals("test", currentFormData.get("reason"));

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 加签测试
     */
    @Test
    void addAudit() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest addAuditRequest = new FlowActionRequest();
        addAuditRequest.setFormData(data);
        addAuditRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody addAuditAdviceBody = new FlowAdviceBody(bossActions.get(3).id(), boss.getUserId());
        addAuditAdviceBody.setForwardOperatorIds(List.of(3L));
        addAuditRequest.setAdvice(addAuditAdviceBody);
        factory.flowService.action(addAuditRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(0, lorneRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(lorneRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 转办测试
     */
    @Test
    void transfer() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest transferRequest = new FlowActionRequest();
        transferRequest.setFormData(data);
        transferRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody transferAdviceBody = new FlowAdviceBody(bossActions.get(4).id(), boss.getUserId());
        transferAdviceBody.setForwardOperatorIds(List.of(3L));
        transferRequest.setAdvice(transferAdviceBody);
        factory.flowService.action(transferRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(lorneRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(lorneRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 退回节点测试
     */
    @Test
    void returnNode() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest returnRequest = new FlowActionRequest();
        returnRequest.setFormData(data);
        returnRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody backAdviceBody = new FlowAdviceBody(bossActions.get(5).id(), boss.getUserId());
        backAdviceBody.setBackNodeId(startNode.getId());
        returnRequest.setAdvice(backAdviceBody);
        factory.flowService.action(returnRequest);

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 委托测试
     */
    @Test
    void delegate() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest delegateRequest = new FlowActionRequest();
        delegateRequest.setFormData(data);
        delegateRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody delegateAdviceBody = new FlowAdviceBody(bossActions.get(6).id(), boss.getUserId());
        delegateAdviceBody.setForwardOperatorIds(List.of(lorne.getUserId()));
        delegateRequest.setAdvice(delegateAdviceBody);
        factory.flowService.action(delegateRequest);

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(lorneRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 自定义事件测试
     */
    @Test
    void custom() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        ApprovalNode bossNode = ApprovalNode.builder()
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
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
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

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(7).id(), boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 撤回测试
     */
    @Test
    void revoke() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> userDoneList = factory.flowRecordRepository.findDoneByOperator(user.getUserId());
        assertEquals(1, userDoneList.size());

        factory.flowService.revoke(new FlowRevokeRequest(userDoneList.get(0).getId(), user.getUserId()));

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 催办测试
     */
    @Test
    void urge() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> userDoneList = factory.flowRecordRepository.findDoneByOperator(user.getUserId());
        assertEquals(1, userDoneList.size());

        factory.flowService.urge(new FlowUrgeRequest(userDoneList.get(0).getId(), user.getUserId()));

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 流程干预测试
     */
    @Test
    void interfere() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne", true);

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 用户转交审批测试
     */
    @Test
    void forwardOperator() {

        User lorne = new User(3, "lorne");
        User user = new User(1, "user");
        // 老板将审批权转给了lorne账户
        User boss = new User(2, "boss", lorne);

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(lorneRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(lorneRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 节点异常测试
     */
    @Test
    void errorOperatorTest() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [-1]}"))
                        .addStrategy(new ErrorTriggerStrategy("def run(request){ return $bind.createErrorThrow(3); }"))
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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(lorneRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(lorneRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 节点异常测试
     */
    @Test
    void errorNodeTest() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.NUMBER)
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
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [-1]}"))
                        .addStrategy(new ErrorTriggerStrategy("def run(request){ return $bind.createErrorThrow(request.getNode('" + startNode.getId() + "')); }"))
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

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(userRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isFinish).toList().size());

    }
}