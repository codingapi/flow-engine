package com.codingapi.flow.service;

import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 问题 210 复现测试：主流程 C（子流程节点）一次性创建 6 个子流程、每个子流程 B 节点 20 个审批人。
 * <p>
 * 用于量化网关延时（模拟真实 DB/远程查询 5ms）下，C 节点提交的整体耗时与网关查询次数，
 * 定位是否存在可优化的空间，例如操作人缓存是否因每次 create/action 清空而反复查询同一批人员。
 */
class FlowIssue210SubProcessPerformanceTest {

    private static final String FORM_CODE = "issue-210-form";
    private static final String PARENT_CODE = "issue-210-parent";
    private static final String CHILD_CODE = "issue-210-child";
    private static final int SUB_PROCESS_COUNT = 6;
    private static final int APPROVER_COUNT = 20;
    private static final long GATEWAY_DELAY_MS = 5;

    private MyFlowServiceFactory factory;
    private User initiator;
    private List<User> approvers;
    private StartNode childStart;
    private ApprovalNode childB;
    private DelayedGateway gateway;

    private StartNode parentStart;
    private ApprovalNode parentB;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        initiator = saveUser(1, "发起人");
        approvers = new ArrayList<>();
        for (int i = 0; i < APPROVER_COUNT; i++) {
            approvers.add(saveUser(100 + i, "审批人" + i));
        }
        FlowForm form = FlowFormBuilder.builder()
                .name("问题210测试表单")
                .code(FORM_CODE)
                .addField("业务内容", "content", DataType.STRING)
                .build();
        // 用带延时与计数的网关替换默认 UserGateway，模拟真实环境的人员查询代价
        gateway = new DelayedGateway(GatewayContext.getInstance().getFlowOperatorGateway(), GATEWAY_DELAY_MS);
        GatewayContext.getInstance().setFlowOperatorGateway(gateway);
        saveChildWorkflow(form);
        buildParentWorkflow(form);
    }

    @AfterEach
    void tearDown() {
        GatewayContext.getInstance().setFlowOperatorGateway(new com.codingapi.flow.gateway.impl.UserGateway());
    }

    /**
     * 测试目标：量化主流程 C（子流程）节点创建 6 个子流程、每个子流程 B 节点 20 个审批人的耗时与网关查询次数。
     * 前置条件：节点为 A-B-SubProcess-D-E，子流程 A-B-C-D 且 B 审批人取表单 approvers 字段；网关每次查询延时 5ms。
     * 执行步骤：提交主流程并审批 A、B，触发子流程批量创建。
     * 期望断言：共创建 6 个子流程、每个子流程 B 节点 20 个待办（总量 120 条）；记录耗时与网关批量查询次数。
     */
    @Test
    void shouldMeasureSubProcessBulkCreationTimeWithGatewayDelay() {
        // when：创建主流程并审批 A 节点，进入 B 节点
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", "parent");
        data.put("approvers", approvers.stream().map(User::getUserId).toList());
        long parentRecordId = createParent(data);
        approveMain(factory.flowRecordRepository.get(parentRecordId), parentStart, data);
        FlowRecord parentBRecord = findTodo(initiator, parentB.getId());
        assertNotNull(parentBRecord, "主流程应进入 B 审批节点");

        // when：审批 B 节点，触发 C 子流程节点批量创建 6 个子流程（B->C 流转）
        long startNanos = System.nanoTime();
        approveMain(parentBRecord, parentB, data);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        // then：6 个子流程实例，每个子流程 B 节点 20 个待办（共 120 条子流程记录）
        List<SubProcessRecord> subProcessRecords = factory.subProcessRepository
                .findByParentRecordId(parentBRecord.getId());
        assertEquals(1, subProcessRecords.size(), "主流程应产生一条子流程聚合记录");
        assertEquals(SUB_PROCESS_COUNT, subProcessRecords.get(0).getInstances().size(),
                "应创建订阅个数子流程");
        assertEquals(SUB_PROCESS_COUNT * APPROVER_COUNT, countChildBTodos(),
                "每个子流程 B 节点 20 审批人，共创建 120 条待办记录");

        // 汇报量化指标
        gateway.printReport();
        System.out.println("[issue-210] 主流程 B/C 节点提交总耗时: " + elapsedMs + " ms"
                + " (网关延时 " + GATEWAY_DELAY_MS + "ms/次)");
    }

    // ---------- 网关延时 + 计数 ----------

    private static class DelayedGateway implements FlowOperatorGateway {
        private final FlowOperatorGateway delegate;
        private final long delayMs;
        private long findCalls;
        private long getCalls;
        private long totalIds;

        DelayedGateway(FlowOperatorGateway delegate, long delayMs) {
            this.delegate = delegate;
            this.delayMs = delayMs;
        }

        @Override
        public IFlowOperator get(long id) {
            sleep();
            getCalls++;
            return delegate.get(id);
        }

        @Override
        public List<IFlowOperator> findByIds(List<Long> ids) {
            sleep();
            findCalls++;
            totalIds += ids.size();
            return delegate.findByIds(ids);
        }

        private void sleep() {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        void printReport() {
            System.out.println("[issue-210] 网关单点查询 get() 次数: " + getCalls);
            System.out.println("[issue-210] 网关批量查询 findByIds 次数: " + findCalls
                    + "，累计覆盖人员 " + totalIds + " 人次");
            System.out.println("[issue-210] 网关总查询次数: " + (getCalls + findCalls)
                    + "，纯网关延时约 " + ((getCalls + findCalls) * delayMs) + " ms");
        }
    }

    // ---------- 流程定义构建 ----------

    private void saveChildWorkflow(FlowForm form) {
        childStart = writableStart("子流程开始", form);
        childB = ApprovalNode.builder()
                .name("子流程B")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission(FORM_CODE, "content"))
                        .addStrategy(new MultiOperatorAuditStrategy(
                                MultiOperatorAuditStrategy.Type.MERGE, 1.0f))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return request.getFormData('approvers') }").getKey()))
                        .build())
                .build();
        ApprovalNode childC = ApprovalNode.builder()
                .name("子流程C")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission(FORM_CODE, "content"))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [" + approvers.get(0).getUserId() + "] }").getKey()))
                        .build())
                .build();
        Workflow childWorkflow = WorkflowBuilder.builder()
                .title("问题210-子流程")
                .code(CHILD_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(childStart)
                .addNode(childB)
                .addNode(childC)
                .addNode(EndNode.builder().name("子流程结束").build())
                .build();
        factory.workflowService.saveWorkflow(childWorkflow);
    }

    private void buildParentWorkflow(FlowForm form) {
        parentStart = writableStart("主流程开始", form);
        parentB = ApprovalNode.builder()
                .name("主流程B")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission(FORM_CODE, "content"))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [" + initiator.getUserId() + "] }").getKey()))
                        .build())
                .build();
        String createScript = """
                def run(request){
                    def approvers = request.getFormData('approvers')
                    def list = []
                    for (int i = 0; i < %d; i++) {
                        list.add(request.toCreateRequest('%s', %d, '%s', [approvers: approvers, content: 'child-' + i]))
                    }
                    return list
                }
                """.formatted(SUB_PROCESS_COUNT, CHILD_CODE, initiator.getUserId(),
                passAction(childStart).id());
        SubProcessNode subProcess = SubProcessNode.builder()
                .name("主流程C-子流程")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission(FORM_CODE, "content"))
                        .addStrategy(new SubProcessStrategy(
                                FlowGroovyScriptFactory.createSubProcessScript(createScript).getKey(), true))
                        .build())
                .build();
        ApprovalNode parentD = ApprovalNode.builder()
                .name("主流程D")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(readonlyPermission(FORM_CODE, "content"))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){ return [" + initiator.getUserId() + "] }").getKey()))
                        .build())
                .build();
        Workflow parentWorkflow = WorkflowBuilder.builder()
                .title("问题210-主流程")
                .code(PARENT_CODE)
                .createdOperator(initiator)
                .form(form)
                .addNode(parentStart)
                .addNode(parentB)
                .addNode(subProcess)
                .addNode(parentD)
                .addNode(EndNode.builder().name("主流程结束").build())
                .build();
        factory.workflowService.saveWorkflow(parentWorkflow);
    }

    // ---------- 辅助方法 ----------

    private long createParent(Map<String, Object> data) {
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(PARENT_CODE);
        request.setOperatorId(initiator.getUserId());
        request.setActionId(passAction(parentStart).id());
        request.setFormData(data);
        return factory.flowService.create(request);
    }

    private void approveMain(FlowRecord record, IFlowNode node, Map<String, Object> data) {
        FlowActionRequest request = new FlowActionRequest();
        request.setRecordId(record.getId());
        request.setFormData(data);
        request.setAdvice(new FlowAdviceBody(passAction(node).id(), "同意", initiator.getUserId()));
        factory.flowService.action(request);
    }

    private FlowRecord findTodo(User operator, String nodeId) {
        return factory.flowRecordRepository.findTodoByOperator(operator.getUserId()).stream()
                .filter(record -> record.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    private int countChildBTodos() {
        int total = 0;
        for (int i = 0; i < APPROVER_COUNT; i++) {
            total += factory.flowRecordRepository.findTodoByOperator(approvers.get(i).getUserId()).stream()
                    .filter(record -> record.getNodeId().equals(childB.getId()))
                    .count();
        }
        return total;
    }

    private StartNode writableStart(String name, FlowForm form) {
        return StartNode.builder()
                .name(name)
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(FORM_CODE, "content", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();
    }

    private FormFieldPermissionStrategy readonlyPermission(String formCode, String field) {
        return new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                .addPermission(formCode, field, PermissionType.READ)
                .build());
    }

    private com.codingapi.flow.action.IFlowAction passAction(IFlowNode node) {
        return node.actionManager().getActions().stream()
                .filter(action -> "PASS".equals(action.type()))
                .findFirst()
                .orElseThrow();
    }

    private User saveUser(long id, String name) {
        User user = new User(id, name);
        factory.userGateway.save(user);
        return user;
    }
}