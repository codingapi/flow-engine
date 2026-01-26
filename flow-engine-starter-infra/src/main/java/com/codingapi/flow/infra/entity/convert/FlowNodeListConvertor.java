package com.codingapi.flow.infra.entity.convert;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.factory.NodeFactory;
import jakarta.persistence.AttributeConverter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlowNodeListConvertor implements AttributeConverter<List<IFlowNode>, String> {

    @Override
    public String convertToDatabaseColumn(List<IFlowNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(nodes.stream().map(IFlowNode::toMap).toList());
    }

    @Override
    public List<IFlowNode> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return new ArrayList<>();
        }
        List<Map> list = JSON.parseArray(dbData, Map.class);
        return list.stream().map(map -> NodeFactory.getInstance().createNode(map)).toList();
    }
}
