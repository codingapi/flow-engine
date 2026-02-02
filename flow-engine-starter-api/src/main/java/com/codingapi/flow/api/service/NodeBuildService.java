package com.codingapi.flow.api.service;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.node.factory.NodeFactory;
import com.codingapi.flow.utils.RandomUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NodeBuildService {

    private final Map<NodeType, NodeType> nodeMapping = new HashMap<>();
    private final Map<NodeType, String> nodeNames = new HashMap<>();

    public NodeBuildService() {
        nodeMapping.put(NodeType.PARALLEL, NodeType.PARALLEL_BRANCH);
        nodeMapping.put(NodeType.INCLUSIVE, NodeType.INCLUSIVE_BRANCH);
        nodeMapping.put(NodeType.CONDITION, NodeType.CONDITION_BRANCH);

        nodeNames.put(NodeType.PARALLEL, "并行控制");
        nodeNames.put(NodeType.INCLUSIVE, "包含控制");
        nodeNames.put(NodeType.CONDITION, "条件控制");
    }

    public Map<String, Object> buildNode(String nodeType) {
        NodeType type = NodeType.valueOf(nodeType);
        NodeType mappingType = nodeMapping.get(type);
        if (mappingType != null) {
            Map<String, Object> current = new HashMap<>();
            current.put("id", RandomUtils.generateStringId());
            current.put("type", type.name());
            current.put("name", nodeNames.get(type));
            current.put("order", 1);
            List<Map<String, Object>> blocks = new ArrayList<>();
            current.put("blocks", blocks);
            for (int i = 0; i < 2; i++) {
                Map<String, Object> block = NodeFactory.getInstance().createNode(mappingType).toMap();
                blocks.add(block);
            }
            return current;
        } else {
            IFlowNode node = NodeFactory.getInstance().createNode(type);
            return node.toMap();
        }
    }
}
