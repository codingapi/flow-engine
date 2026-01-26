package com.codingapi.flow.infra.entity.convert;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;
import com.codingapi.flow.strategy.workflow.WorkflowStrategyFactory;
import jakarta.persistence.AttributeConverter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowStrategyListConvertor implements AttributeConverter<List<IWorkflowStrategy>, String> {

    @Override
    public String convertToDatabaseColumn(List<IWorkflowStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(strategies.stream().map(IWorkflowStrategy::toMap).toList());
    }

    @Override
    public List<IWorkflowStrategy> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return new ArrayList<>();
        }
        List<Map> maps = JSON.parseArray(dbData, Map.class);
        return maps.stream().map(map -> WorkflowStrategyFactory.getInstance().createStrategy(map)).toList();
    }
}
