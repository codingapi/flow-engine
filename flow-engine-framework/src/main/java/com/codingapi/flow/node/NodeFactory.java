package com.codingapi.flow.node;

import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class NodeFactory {

    @Getter
    private final static NodeFactory instance = new NodeFactory();

    private final Map<String, Class<? extends IFlowNode>> nodesClasses = new HashMap<>();

    private NodeFactory() {
        this.initNodes();
    }

    private void initNodes() {
        nodesClasses.put(ApprovalNode.NODE_TYPE, ApprovalNode.class);
        nodesClasses.put(ConditionNodeBranchNode.NODE_TYPE, ConditionNodeBranchNode.class);
        nodesClasses.put(DelayNode.NODE_TYPE, DelayNode.class);
        nodesClasses.put(EndNode.NODE_TYPE, EndNode.class);
        nodesClasses.put(HandleNode.NODE_TYPE, HandleNode.class);
        nodesClasses.put(InclusiveBranchNode.NODE_TYPE, InclusiveBranchNode.class);
        nodesClasses.put(NotifyNode.NODE_TYPE, NotifyNode.class);
        nodesClasses.put(RouterBranchNode.NODE_TYPE, RouterBranchNode.class);
        nodesClasses.put(ParallelBranchNode.NODE_TYPE, ParallelBranchNode.class);
        nodesClasses.put(StartNode.NODE_TYPE, StartNode.class);
        nodesClasses.put(SubProcessNode.NODE_TYPE, SubProcessNode.class);
        nodesClasses.put(TriggerNode.NODE_TYPE, TriggerNode.class);
    }


    @SneakyThrows
    public IFlowNode createNode(Map<String, Object> map) {
        String type = (String) map.get("type");
        Class<? extends IFlowNode> clazz = nodesClasses.get(type);
        if (clazz != null) {
            Method formMap = clazz.getMethod("formMap", Map.class);
            return (IFlowNode) formMap.invoke(null, map);
        }
        return null;
    }
}
