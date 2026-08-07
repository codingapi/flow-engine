package com.codingapi.example;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.mock.MockInstance;
import com.codingapi.flow.mock.MockInstanceFactory;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.repository.WorkflowRepository;
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
 * 回归测试（mock 模式）：A(开始)-B(审批)-C(结束)，在 A 提交时
 * 不应出现 "Query requires transaction be in progress" 异常。
 * <p>
 * mock 模式下 FlowService/WorkflowService 为 new 出的裸对象（无事务代理），
 * lockById 悲观锁查询依赖仓储自身开启事务。
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:flow-mock-test;DB_CLOSE_DELAY=-1")
class MockFlowCreateReproduceTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private FlowOperatorGateway flowOperatorGateway;

    @Autowired
    private UserRepository userRepository;

    @Test
    void should_create_flow_in_mock_mode_without_transaction_required_error() {
        User admin = userRepository.getUserByAccount(User.ADMIN_ACCOUNT);
        assertTrue(admin != null, "admin 用户应从 AdminInitializer 初始化");

        // 模拟真实用户操作：通过 /api/cmd/workflow/mock 创建 mock 实例
        MockInstance mockInstance = MockInstanceFactory.getInstance()
                .create(flowOperatorGateway, workflowRepository);
        try {
            String code = "reimburse-mock-" + System.nanoTime();

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

            long recordId = mockInstance.getFlowService().create(createRequest);
            assertTrue(recordId > 0, "mock 模式下流程创建应成功并返回记录id");
        } finally {
            MockInstanceFactory.getInstance().clear(mockInstance.getMockKey());
        }
    }
}