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
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RecordMergeStrategy;
import com.codingapi.flow.strategy.node.RevokeStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #260：合并审批流程由开始节点人员撤回流程时，同组其他流程的合并待办被一并删除。
 *
 * <p>流程 A-B-D，B 为多人审批（或签，审批人 b1、b2）且开启记录合并。
 * a1、a2 两条流程在 B 节点合并为 b1、b2 各自的合并待办（组内各 2 条记录）。</p>
 *
 * <p>期望：a1 撤回后，b1、b2 的合并待办仅移除 a1 的记录、保留 a2；
 * a1 重新提交后，合并待办恢复为 2 条。</p>
 */
public class FlowIssue260MergeRevokeTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void revokeOneProcessOfMergedTodo_shouldKeepRemainingProcessAndRestoreAfterResubmit() {
        // given
        User a1 = new User(11, "a1");
        User a2 = new User(12, "a2");
        User b1 = new User(2, "b1");
        User b2 = new User(3, "b2");
        List.of(a1, a2, b1, b2).forEach(factory.userGateway::save);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        Workflow workflow = buildWorkflow(a1, "issue260");
        factory.workflowService.saveWorkflow(workflow);
        String bNodeId = bNodeId(workflow);

        Map<String, Object> data = Map.of("name", "lorne");

        // a2 先提交、a1 后提交：合并待办的代表记录为 a1 的 B 记录，与问题现场一致
        createAndSubmit(a2, workflow, data);
        long a1StartRecordId = createAndSubmit(a1, workflow, data);

        for (User approver : List.of(b1, b2)) {
            FlowTodoRecord group = singleGroupOf(approver, bNodeId);
            assertEquals(2, group.getMargeCount(), approver.getName() + " 的合并组应有 a1、a2 两条记录");
            assertEquals(a1.getUserId(), factory.flowRecordRepository.get(group.getRecordId()).getCreateOperatorId(),
                    "合并待办代表记录应为最后提交的 a1");
        }

        // when a1 撤回自己的流程
        factory.flowService.revoke(new FlowRevokeRequest(a1StartRecordId, a1.getUserId()));

        // then b1、b2 的合并待办应保留 a2，且代表记录仍指向未办理的待办
        for (User approver : List.of(b1, b2)) {
            FlowTodoRecord group = singleGroupOf(approver, bNodeId);
            assertEquals(1, group.getMargeCount(), approver.getName() + " 的合并组应仅剩 a2 一条记录");
            FlowRecord representative = factory.flowRecordRepository.get(group.getRecordId());
            assertNotNull(representative, approver.getName() + " 的合并代表记录应存在");
            assertEquals(a2.getUserId(), representative.getCreateOperatorId(),
                    approver.getName() + " 的合并代表记录应指向仍待办的 a2");
            assertTrue(representative.isTodo(),
                    approver.getName() + " 的合并代表记录应仍为待办，实际：" + representative.getId());
            assertEquals(1, countTodoRecords(approver, bNodeId),
                    approver.getName() + " 在 B 节点应仅剩 1 条待办记录");
            assertEquals(1, factory.flowTodoMergeRepository.findByTodoId(group.getId()).size(),
                    "合并关系应仅剩 a2 一条");
        }
        // 已撤销记录的合并关系必须被清理，不能残留指向已删除待办的孤儿关系
        assertEquals(2, factory.flowTodoMergeRepository.findAll().size(),
                "合并关系总数应为 b1、b2 各 1 条");

        // when a1 重新提交
        resubmit(a1, workflow, data, a1StartRecordId);

        // then 合并待办恢复为两条
        for (User approver : List.of(b1, b2)) {
            FlowTodoRecord group = singleGroupOf(approver, bNodeId);
            assertEquals(2, group.getMargeCount(), approver.getName() + " 的合并组应恢复为 2 条记录");
            assertEquals(2, countTodoRecords(approver, bNodeId),
                    approver.getName() + " 在 B 节点应恢复为 2 条待办记录");
        }
        assertEquals(2, factory.flowTodoRecordRepository.findAll().size(),
                "b1、b2 应各保留 1 条合并待办组");
    }

    // ─── 辅助方法 ───

    private FlowTodoRecord singleGroupOf(User approver, String nodeId) {
        List<FlowTodoRecord> groups = factory.flowTodoRecordRepository.findByOperatorId(approver.getUserId()).stream()
                .filter(group -> nodeId.equals(group.getNodeId()))
                .toList();
        assertEquals(1, groups.size(), approver.getName() + " 在合并节点应有且仅有 1 条合并待办组");
        return groups.get(0);
    }

    private long countTodoRecords(User approver, String nodeId) {
        return factory.flowRecordRepository.findTodoByOperator(approver.getUserId()).stream()
                .filter(record -> nodeId.equals(record.getNodeId()))
                .count();
    }

    private String bNodeId(Workflow workflow) {
        return workflow.getNodes().stream()
                .filter(node -> node.getName().equals("B审批"))
                .findFirst().orElseThrow().getId();
    }

    private Workflow buildWorkflow(User createdBy, String code) {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code(code)
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new RevokeStrategy(true, RevokeStrategy.Type.REVOKE_CURRENT))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(CustomAction.defaultAction())
                        .build())
                .build();

        ApprovalNode bNode = ApprovalNode.builder()
                .name("B审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission(code, "name", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(FlowGroovyScriptFactory
                                .createOperatorLoadScript("def run(request){return [2,3]}").getKey()))
                        // 或签：b1、b2 各自持有待办记录，两条流程在各自审批人下分别合并
                        .addStrategy(new MultiOperatorAuditStrategy(MultiOperatorAuditStrategy.Type.ANY, 0f))
                        .addStrategy(new RecordMergeStrategy(true))
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();

        return WorkflowBuilder.builder()
                .title("测试流程")
                .code(code)
                .createdOperator(createdBy)
                .form(form)
                .addNode(startNode)
                .addNode(bNode)
                .addNode(endNode)
                .build();
    }

    /**
     * 发起并提交到 B 节点，返回发起节点记录id
     */
    private long createAndSubmit(User user, Workflow workflow, Map<String, Object> data) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction startAction = startNode.actionManager().getActions().get(0);

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(data);
        createRequest.setActionId(startAction.id());
        createRequest.setOperatorId(user.getUserId());
        long recordId = factory.flowService.create(createRequest);

        submit(startAction, user, data, recordId);
        return recordId;
    }

    /**
     * 撤回后发起记录回到待办，重新提交
     */
    private void resubmit(User user, Workflow workflow, Map<String, Object> data, long startRecordId) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction startAction = startNode.actionManager().getActions().get(0);
        submit(startAction, user, data, startRecordId);
    }

    private void submit(IFlowAction action, User user, Map<String, Object> data, long recordId) {
        FlowActionRequest actionRequest = new FlowActionRequest();
        actionRequest.setFormData(data);
        actionRequest.setRecordId(recordId);
        actionRequest.setAdvice(new FlowAdviceBody(action.id(), "同意", user.getUserId()));
        factory.flowService.action(actionRequest);
    }
}
