package com.codingapi.flow.workflow.runtime;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * issue #281：运行实例强制同步的结构一致性校验。
 *
 * <p>校验基准：旧运行时快照中的节点结构（id / 类型 / 父子层级 / 同层顺序）必须与最新设计完全一致；
 * 旧快照中已存在的按钮动作必须保留且类型不变；策略、脚本、节点名称等配置允许变化。</p>
 */
class WorkflowStructureVerifierTest {

    private final User creator = new User(11, "a1");

    @BeforeEach
    void setUp() {
        UserGateway userGateway = new UserGateway();
        userGateway.save(creator);
        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);
    }

    @Test
    void should_be_consistent_after_json_round_trip() {
        // given 同一份设计经过 JSON 序列化/反序列化（等同于前端保存后的再次读取）
        Workflow workflow = buildWorkflow();
        Workflow copy = Workflow.formJson(workflow.toJson());

        // when
        WorkflowStructureDiff diff = WorkflowStructureVerifier.compare(workflow, copy);

        // then
        assertTrue(diff.isConsistent(), diff.describe());
    }

    @Test
    void should_allow_config_only_change() {
        // given 仅修改节点名称与策略配置（配置级变更）
        Workflow workflow = buildWorkflow();
        Workflow copy = Workflow.formJson(workflow.toJson());
        BaseFlowNode approvalNode = (BaseFlowNode) copy.getFlowNode("approval");
        approvalNode.setName("审批-改名后");

        // when
        WorkflowStructureDiff diff = WorkflowStructureVerifier.compare(workflow, copy);

        // then
        assertTrue(diff.isConsistent(), diff.describe());
    }

    @Test
    void should_report_missing_node_when_node_removed() {
        // given 新设计中删除了 approval 节点
        Workflow workflow = buildWorkflow();
        Workflow copy = Workflow.formJson(workflow.toJson());
        copy.getNodes().removeIf(node -> node.getId().equals("approval"));

        // when
        WorkflowStructureDiff diff = WorkflowStructureVerifier.compare(workflow, copy);

        // then
        assertFalse(diff.isConsistent());
        assertEquals(List.of("approval"), diff.getMissingNodeIds());
        assertTrue(diff.getExtraNodeIds().isEmpty());
    }

    @Test
    void should_report_extra_node_when_node_added() {
        // given 新设计中新增了节点（issue 明确不允许同步）
        Workflow workflow = buildWorkflow();
        Workflow copy = Workflow.formJson(workflow.toJson());
        List<com.codingapi.flow.node.IFlowNode> nodes = new ArrayList<>(copy.getNodes());
        nodes.add(ApprovalNode.builder().id("extra-approval").name("新增审批").build());
        copy.getNodes().clear();
        copy.getNodes().addAll(nodes);

        // when
        WorkflowStructureDiff diff = WorkflowStructureVerifier.compare(workflow, copy);

        // then
        assertFalse(diff.isConsistent());
        assertEquals(List.of("extra-approval"), diff.getExtraNodeIds());
    }

    @Test
    void should_report_changed_node_when_order_changed() {
        // given 同层节点顺序调整
        Workflow workflow = buildWorkflow();
        Workflow copy = Workflow.formJson(workflow.toJson());
        BaseFlowNode approvalNode = (BaseFlowNode) copy.getFlowNode("approval");
        approvalNode.setOrder(99);

        // when
        WorkflowStructureDiff diff = WorkflowStructureVerifier.compare(workflow, copy);

        // then
        assertFalse(diff.isConsistent());
        assertEquals(List.of("approval"), diff.getChangedNodeIds());
    }

    @Test
    void should_report_missing_action_when_action_removed() {
        // given 新设计删除了节点上的全部按钮动作
        Workflow workflow = buildWorkflow();
        Workflow copy = Workflow.formJson(workflow.toJson());
        BaseFlowNode approvalNode = (BaseFlowNode) copy.getFlowNode("approval");
        List<String> oldActionIds = approvalNode.actionManager().getActions().stream()
                .map(IFlowAction::id).toList();
        approvalNode.setActions(List.of());

        // when
        WorkflowStructureDiff diff = WorkflowStructureVerifier.compare(workflow, copy);

        // then
        assertFalse(diff.isConsistent());
        assertEquals(oldActionIds, diff.getMissingActionIds());
    }

    @Test
    void should_allow_new_action() {
        // given 新设计新增了自定义按钮（新增按钮允许）
        Workflow workflow = buildWorkflow();
        Workflow copy = Workflow.formJson(workflow.toJson());
        BaseFlowNode approvalNode = (BaseFlowNode) copy.getFlowNode("approval");
        approvalNode.setActions(List.of(CustomAction.defaultAction()));

        // when
        WorkflowStructureDiff diff = WorkflowStructureVerifier.compare(workflow, copy);

        // then
        assertTrue(diff.isConsistent(), diff.describe());
    }

    private Workflow buildWorkflow() {
        FlowForm form = FlowFormBuilder.builder()
                .name("测试流程")
                .code("force-sync")
                .addField("name", "name", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder().id("start").name("开始").build();
        ApprovalNode approvalNode = ApprovalNode.builder().id("approval").name("审批").order(1).build();
        EndNode endNode = EndNode.builder().id("end").name("结束").order(2).build();

        return WorkflowBuilder.builder()
                .id("wf-force-sync")
                .code("force-sync")
                .title("测试流程")
                .createdOperator(creator)
                .form(form)
                .nodes(new ArrayList<>(List.of(startNode, approvalNode, endNode)))
                .build();
    }
}
