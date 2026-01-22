package com.codingapi.flow.node.factory;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.BranchNodeBranchNode;
import com.codingapi.flow.node.nodes.InclusiveBranchNode;
import com.codingapi.flow.node.nodes.ParallelBranchNode;
import com.codingapi.flow.node.nodes.RouterNode;
import com.codingapi.flow.node.nodes.DelayNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
import com.codingapi.flow.node.nodes.TriggerNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.HandleNode;
import com.codingapi.flow.node.nodes.NotifyNode;
import com.codingapi.flow.node.nodes.StartNode;
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
        nodesClasses.put(BranchNodeBranchNode.NODE_TYPE, BranchNodeBranchNode.class);
        nodesClasses.put(DelayNode.NODE_TYPE, DelayNode.class);
        nodesClasses.put(EndNode.NODE_TYPE, EndNode.class);
        nodesClasses.put(HandleNode.NODE_TYPE, HandleNode.class);
        nodesClasses.put(InclusiveBranchNode.NODE_TYPE, InclusiveBranchNode.class);
        nodesClasses.put(NotifyNode.NODE_TYPE, NotifyNode.class);
        nodesClasses.put(RouterNode.NODE_TYPE, RouterNode.class);
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
