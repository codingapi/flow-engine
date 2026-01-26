package com.codingapi.flow.infra.entity.convert;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.edge.FlowEdge;
import jakarta.persistence.AttributeConverter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class FlowEdgeListConvertor implements AttributeConverter<List<FlowEdge>, String> {

    @Override
    public String convertToDatabaseColumn(List<FlowEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(edges);
    }

    @Override
    public List<FlowEdge> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return new ArrayList<>();
        }
        return JSON.parseArray(dbData, FlowEdge.class);
    }
}
