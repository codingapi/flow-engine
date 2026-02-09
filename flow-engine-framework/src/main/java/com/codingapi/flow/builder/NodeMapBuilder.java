package com.codingapi.flow.builder;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.factory.FlowActionFactory;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.factory.NodeFactory;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.strategy.node.NodeStrategyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeMapBuilder {

    @SuppressWarnings("unchecked")
    public static List<FormFieldPermission> loadFormFieldPermissions(Map<String, Object> data) {
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) data.get("fieldPermissions");
        if (permissions != null) {
            List<FormFieldPermission> permissionList = new ArrayList<>();
            for (Map<String, Object> item : permissions) {
                FormFieldPermission permission = FormFieldPermission.fromMap(item);
                permissionList.add(permission);
            }
            return permissionList;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<IFlowAction> loadActions(Map<String, Object> data) {
        List<Map<String, Object>> actions = (List<Map<String, Object>>) data.get("actions");
        if (actions != null) {
            List<IFlowAction> actionList = new ArrayList<>();
            for (Map<String, Object> item : actions) {
                IFlowAction action = FlowActionFactory.getInstance().createAction(item);
                actionList.add(action);
            }
            return actionList;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<INodeStrategy> loadNodeStrategies(Map<String, Object> data) {
        List<Map<String, Object>> nodeStrategies = (List<Map<String, Object>>) data.get("strategies");
        if (nodeStrategies != null) {
            List<INodeStrategy> strategyList = new ArrayList<>();
            for (Map<String, Object> item : nodeStrategies) {
                INodeStrategy strategy = NodeStrategyFactory.getInstance().createStrategy(item);
                strategyList.add(strategy);
            }
            return strategyList;
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    public static List<IFlowNode> loadNodes(Map<String, Object> data){
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("blocks");
        if (nodes != null) {
            List<IFlowNode> nodeList = new ArrayList<>();
            for (Map<String, Object> node : nodes) {
                IFlowNode flowNode = NodeFactory.getInstance().createNode(node);
                nodeList.add(flowNode);
            }
            return nodeList;
        }
        return null;
    }


}
