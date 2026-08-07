package com.codingapi.example;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试（生产路径）：A(开始)-B(审批)-C(结束)，在 A 提交时正常创建流程。
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:flow-create-test;DB_CLOSE_DELAY=-1")
class FlowCreateReproduceTest {

    @Autowired
    private FlowService flowService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void should_create_flow_without_transaction_required_error() {
        User admin = userRepository.getUserByAccount(User.ADMIN_ACCOUNT);
        assertTrue(admin != null, "admin 用户应从 AdminInitializer 初始化");

        String code = "reimburse-" + System.nanoTime();

        FlowForm form = FlowFormBuilder.builder()
                .name("报销流程")
                .code(code)
                .addField("报销金额", "amount", DataType.DOUBLE)
                .addField("报销事由", "reason", DataType.STRING)
                .build();

        FormFieldPermissionsBuilder permissions = FormFieldPermissionsBuilder.builder()
                .addPermission(code, "amount", PermissionType.WRITE)
                .addPermission(code, "reason", PermissionType.WRITE);

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .build())
                .build();

        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(permissions.build()))
                        .addStrategy(OperatorLoadStrategy.initiatorSelectStrategy())
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        Workflow workflow = WorkflowBuilder.builder()
                .title("报销流程")
                .code(code)
                .createdOperator(admin)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();

        workflowService.saveWorkflow(workflow);

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(Map.of("amount", 100.0, "reason", "出差"));
        createRequest.setActionId(startNode.actionManager().getActions().get(0).id());
        createRequest.setOperatorId(admin.getUserId());

        long recordId = flowService.create(createRequest);
        assertTrue(recordId > 0, "流程创建应成功并返回记录id");
    }
}