package com.codingapi.example;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.infra.jpa.FlowTodoRecordEntityRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 待办查询的身份隔离测试（issue #288 P2）。
 *
 * <p>校验两件事：
 * <ol>
 *     <li>身份条件真正落到 SQL 上（不是内存过滤）；</li>
 *     <li>历史数据身份列为 {@code NULL} 时，按默认身份查询仍然能查到——否则升级瞬间
 *     所有存量待办都会从用户列表里消失。</li>
 * </ol>
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:flow-todo-identity-test;DB_CLOSE_DELAY=-1")
class FlowTodoIdentityQueryTest {

    @Autowired
    private FlowService flowService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlowTodoRecordEntityRepository flowTodoRecordEntityRepository;

    /**
     * 历史数据（身份为 NULL）必须能被默认身份查到，且不会串到非默认身份的查询结果里。
     */
    @Test
    void should_match_null_identity_as_default_for_legacy_rows() {
        long operatorId = 880001L;

        FlowTodoRecordEntity legacy = new FlowTodoRecordEntity();
        legacy.setProcessId("legacy-process");
        legacy.setWorkflowRuntimeId(1L);
        legacy.setWorkCode("legacy");
        legacy.setNodeId("start");
        legacy.setNodeType("START");
        legacy.setNodeName("开始");
        legacy.setTitle("历史待办");
        legacy.setCurrentOperatorId(operatorId);
        legacy.setCurrentOperatorName("user1");
        legacy.setCurrentOperatorIdentity(null);
        legacy.setTodoKey(operatorId + "-1-start");
        legacy.setMargeCount(0);
        legacy.setMergeable(false);
        legacy.setRecordId(1L);
        legacy.setCreateTime(System.currentTimeMillis());
        flowTodoRecordEntityRepository.save(legacy);

        FlowTodoRecordEntity department1 = new FlowTodoRecordEntity();
        department1.setProcessId("department1-process");
        department1.setWorkflowRuntimeId(1L);
        department1.setWorkCode("leave");
        department1.setNodeId("start");
        department1.setNodeType("START");
        department1.setNodeName("开始");
        department1.setTitle("部门1待办");
        department1.setCurrentOperatorId(operatorId);
        department1.setCurrentOperatorName("user1");
        department1.setCurrentOperatorIdentity("department1");
        department1.setTodoKey(operatorId + "-1-start#department1");
        department1.setMargeCount(0);
        department1.setMergeable(false);
        department1.setRecordId(2L);
        department1.setCreateTime(System.currentTimeMillis());
        flowTodoRecordEntityRepository.save(department1);

        PageRequest page = PageRequest.of(0, 10);

        // 默认身份：只命中身份为 NULL 的历史行
        assertEquals(1, flowTodoRecordEntityRepository
                .findTodoRecordPageByIdentity(operatorId, "", page).getTotalElements());
        // 空串与 NULL 等价，不能把非默认身份的行也带出来
        assertEquals(0, flowTodoRecordEntityRepository
                .findTodoRecordPageByIdentity(operatorId, "department2", page).getTotalElements());
        // 非默认身份：只命中自己的行
        assertEquals(1, flowTodoRecordEntityRepository
                .findTodoRecordPageByIdentity(operatorId, "department1", page).getTotalElements());
    }

    /**
     * 端到端：以指定身份发起流程 → 待办按身份落库 → 按身份可查、按默认身份查不到。
     */
    @Test
    void should_persist_identity_and_query_todo_by_identity() {
        User admin = userRepository.getUserByAccount(User.ADMIN_ACCOUNT);
        String code = "identity-" + System.nanoTime();

        FlowForm form = FlowFormBuilder.builder()
                .name("身份测试流程")
                .code(code)
                .addField("事由", "reason", DataType.STRING)
                .build();

        FormFieldPermissionsBuilder permissions = FormFieldPermissionsBuilder.builder()
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
                .title("身份测试流程")
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
        createRequest.setFormData(Map.of("reason", "测试身份"));
        createRequest.setActionId(startNode.actionManager().getActions().get(0).id());
        createRequest.setOperatorId(admin.getUserId());
        createRequest.setOperatorIdentity("department1");

        PageRequest page = PageRequest.of(0, 50);
        // 用增量断言，避免同一上下文中其他用例的待办影响计数
        long identityBefore = flowTodoRecordEntityRepository
                .findTodoRecordPageByIdentity(admin.getUserId(), "department1", page).getTotalElements();
        long defaultBefore = flowTodoRecordEntityRepository
                .findTodoRecordPageByIdentity(admin.getUserId(), "", page).getTotalElements();

        long recordId = flowService.create(createRequest);
        assertTrue(recordId > 0, "流程创建应成功并返回记录id");

        long identityAfter = flowTodoRecordEntityRepository
                .findTodoRecordPageByIdentity(admin.getUserId(), "department1", page).getTotalElements();
        long defaultAfter = flowTodoRecordEntityRepository
                .findTodoRecordPageByIdentity(admin.getUserId(), "", page).getTotalElements();

        assertEquals(identityBefore + 1, identityAfter, "带身份的待办应能按身份查到");
        assertEquals(defaultBefore, defaultAfter, "带身份的待办不应出现在默认身份列表里");
    }
}
