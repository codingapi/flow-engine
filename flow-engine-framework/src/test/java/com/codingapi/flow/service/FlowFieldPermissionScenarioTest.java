package com.codingapi.flow.service;

import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.FormFieldPermission;
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
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 场景复现：A(开始)-B(审批)-C(结束)，字段 username/remark。
 * A 节点两字段均可编辑；B 节点配置 username 为只读。
 * 期望：流程到达 B 节点时，详情返回的字段权限 username 为 READ。
 */
class FlowFieldPermissionScenarioTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void should_apply_read_permission_at_approval_node() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("反馈流程")
                .code("feedback")
                .addField("用户", "username", DataType.STRING)
                .addField("备注", "remark", DataType.STRING)
                .build();

        // A 节点：两字段均可写
        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("feedback", "username", PermissionType.WRITE)
                                .addPermission("feedback", "remark", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        // B 节点：username 只读，remark 可写
        ApprovalNode bossNode = ApprovalNode.builder()
                .name("审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("feedback", "username", PermissionType.READ)
                                .addPermission("feedback", "remark", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript("def run(request){return [2]}").getKey()))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("反馈流程")
                .code("feedback")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("username", "lorne", "remark", "hello");

        // 发起流程
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startNode.actionManager().getActions().get(0).id());
        createRequest.setOperatorId(user.getUserId());
        factory.flowService.create(createRequest);

        // A 节点提交，推进到 B
        FlowRecord startRecord = factory.flowRecordRepository.findTodoByOperator(user.getUserId()).get(0);
        FlowActionRequest startRequest = new FlowActionRequest();
        startRequest.setFormData(data);
        startRequest.setRecordId(startRecord.getId());
        startRequest.setAdvice(new FlowAdviceBody(
                startNode.actionManager().getActions().get(0).id(), "提交", user.getUserId()));
        factory.flowService.action(startRequest);

        // B 节点待办
        List<FlowRecord> bossRecords = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecords.size());

        // 查看 B 节点详情
        FlowContent detail = factory.flowService.detail(
                new FlowDetailRequest(bossRecords.get(0).getId(), boss.getUserId()));

        FormFieldPermission usernamePermission = detail.getFieldPermissions().stream()
                .filter(item -> item.isField("feedback", "username"))
                .findFirst()
                .orElse(null);
        FormFieldPermission remarkPermission = detail.getFieldPermissions().stream()
                .filter(item -> item.isField("feedback", "remark"))
                .findFirst()
                .orElse(null);

        assertEquals(PermissionType.READ, usernamePermission.getType(),
                "B 节点 username 应为只读");
        assertEquals(PermissionType.WRITE, remarkPermission.getType(),
                "B 节点 remark 应可写");
    }
}