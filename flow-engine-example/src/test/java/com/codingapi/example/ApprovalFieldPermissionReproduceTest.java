package com.codingapi.example;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
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
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 复现 issue #191：A(开始)-B(审批)-C(结束)，username/remark 两字段。
 * B 节点 username 配置为只读，流程到达 B 后详情应返回 READ 权限。
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:permission-test;DB_CLOSE_DELAY=-1")
class ApprovalFieldPermissionReproduceTest {

    @Autowired
    private FlowService flowService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private FlowRecordService flowRecordService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void should_return_read_permission_at_approval_node() {
        User admin = userRepository.getUserByAccount(User.ADMIN_ACCOUNT);
        String code = "permission-" + System.nanoTime();

        FlowForm form = FlowFormBuilder.builder()
                .name("反馈流程")
                .code(code)
                .addField("用户", "username", DataType.STRING)
                .addField("备注", "remark", DataType.STRING)
                .build();

        FormFieldPermissionsBuilder permissions = FormFieldPermissionsBuilder.builder()
                .addPermission(code, "username", PermissionType.WRITE)
                .addPermission(code, "remark", PermissionType.WRITE);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "username", PermissionType.READ)
                                .addPermission(code, "remark", PermissionType.WRITE)
                                .build()))
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy())
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("反馈流程")
                .code(code)
                .createdOperator(admin)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();

        workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("username", "lorne", "remark", "hello");

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startNode.actionManager().getActions().get(0).id());
        createRequest.setOperatorId(admin.getUserId());
        long startRecordId = flowService.create(createRequest);

        // 发起人提交到审批节点（指定审批人为 admin）
        FlowRecord startRecord = flowRecordService.getFlowRecord(startRecordId);
        FlowAdviceBody advice = new FlowAdviceBody(
                startNode.actionManager().getActions().get(0).id(), "提交", admin.getUserId());
        advice.setOperatorSelectMap(Map.of(approvalNode.getId(), List.of(admin.getUserId())));

        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(startRecord.getId());
        actionRequest.setAdvice(advice);
        flowService.action(actionRequest);

        // 审批节点待办
        FlowRecord approvalRecord = flowRecordService.findFlowRecordByProcessId(startRecord.getProcessId())
                .stream()
                .filter(r -> approvalNode.getId().equals(r.getNodeId()))
                .findFirst()
                .orElse(null);
        assertNotNull(approvalRecord, "应存在审批节点待办");

        FlowContent detail = flowService.detail(
                new FlowDetailRequest(approvalRecord.getId(), admin.getUserId()));

        FormFieldPermission usernamePermission = detail.getFieldPermissions().stream()
                .filter(item -> item.isField(code, "username"))
                .findFirst()
                .orElse(null);
        FormFieldPermission remarkPermission = detail.getFieldPermissions().stream()
                .filter(item -> item.isField(code, "remark"))
                .findFirst()
                .orElse(null);

        assertEquals(PermissionType.READ, usernamePermission.getType(),
                "审批节点 username 应为只读");
        assertEquals(PermissionType.WRITE, remarkPermission.getType(),
                "审批节点 remark 应可写");
    }
}