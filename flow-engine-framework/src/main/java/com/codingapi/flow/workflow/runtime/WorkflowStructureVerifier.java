package com.codingapi.flow.workflow.runtime;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.workflow.Workflow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程结构一致性校验（issue #281）。
 *
 * <p>强制同步只能替换「配置级」内容（节点策略、按钮展示/隐藏脚本、标题等），
 * 不能改变运行实例已经走过的流程结构。因此以旧运行时快照为基准，与最新设计逐节点比对：</p>
 *
 * <ul>
 *     <li>节点集合：不允许新增、删除（节点id 必须完全一致）；</li>
 *     <li>节点类型、父子层级（blocks）、同层顺序（order）：必须一致；</li>
 *     <li>按钮动作：旧快照中已存在的动作必须保留且类型不变，允许新增动作；</li>
 *     <li>表单字段不做校验（由业务方保证兼容）。</li>
 * </ul>
 */
public class WorkflowStructureVerifier {

    private WorkflowStructureVerifier() {
    }

    /**
     * 比对旧运行时快照与最新流程设计的结构差异
     *
     * @param oldWorkflow 旧运行时快照对应的流程
     * @param newWorkflow 最新流程设计
     * @return 结构差异
     */
    public static WorkflowStructureDiff compare(Workflow oldWorkflow, Workflow newWorkflow) {
        Map<String, NodeStructure> oldNodes = flatten(oldWorkflow);
        Map<String, NodeStructure> newNodes = flatten(newWorkflow);

        WorkflowStructureDiff diff = new WorkflowStructureDiff();

        for (Map.Entry<String, NodeStructure> entry : oldNodes.entrySet()) {
            String nodeId = entry.getKey();
            NodeStructure target = newNodes.get(nodeId);
            if (target == null) {
                diff.addMissingNode(nodeId);
                continue;
            }
            if (!entry.getValue().sameStructure(target)) {
                diff.addChangedNode(nodeId);
            }
            for (IFlowAction oldAction : entry.getValue().actions) {
                IFlowAction newAction = target.getAction(oldAction.id());
                if (newAction == null || !newAction.type().equals(oldAction.type())) {
                    diff.addMissingAction(oldAction.id());
                }
            }
        }

        for (String nodeId : newNodes.keySet()) {
            if (!oldNodes.containsKey(nodeId)) {
                diff.addExtraNode(nodeId);
            }
        }

        return diff;
    }

    private static Map<String, NodeStructure> flatten(Workflow workflow) {
        Map<String, NodeStructure> structures = new LinkedHashMap<>();
        if (workflow != null) {
            flatten(workflow.getNodes(), null, structures);
        }
        return structures;
    }

    private static void flatten(List<IFlowNode> nodes, String parentNodeId, Map<String, NodeStructure> structures) {
        if (nodes == null) {
            return;
        }
        for (IFlowNode node : nodes) {
            List<IFlowAction> actions = node.actionManager() == null ? List.of() : node.actionManager().getActions();
            structures.put(node.getId(), new NodeStructure(node.getType(), parentNodeId, node.getOrder(),
                    actions == null ? List.of() : new ArrayList<>(actions)));
            flatten(node.blocks(), node.getId(), structures);
        }
    }

    /**
     * 单个节点的结构信息
     */
    private static class NodeStructure {

        private final String type;
        private final String parentNodeId;
        private final int order;
        private final List<IFlowAction> actions;

        private NodeStructure(String type, String parentNodeId, int order, List<IFlowAction> actions) {
            this.type = type;
            this.parentNodeId = parentNodeId;
            this.order = order;
            this.actions = actions;
        }

        private boolean sameStructure(NodeStructure target) {
            return this.order == target.order
                    && this.type.equals(target.type)
                    && java.util.Objects.equals(this.parentNodeId, target.parentNodeId);
        }

        private IFlowAction getAction(String actionId) {
            for (IFlowAction action : actions) {
                if (action.id() != null && action.id().equals(actionId)) {
                    return action;
                }
            }
            return null;
        }
    }
}
