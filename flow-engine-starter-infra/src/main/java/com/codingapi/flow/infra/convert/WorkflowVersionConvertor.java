package com.codingapi.flow.infra.convert;

import com.codingapi.flow.infra.entity.WorkflowVersionEntity;
import com.codingapi.flow.infra.entity.convert.*;
import com.codingapi.flow.workflow.WorkflowVersion;

public class WorkflowVersionConvertor {

    private final static FlowOperatorConvertor flowOperatorConvertor = new FlowOperatorConvertor();
    private final static FlowNodeListConvertor flowNodeListConvertor = new FlowNodeListConvertor();
    private final static OperatorMatchScriptConvertor operatorMatchScriptConvertor = new OperatorMatchScriptConvertor();
    private final static WorkflowStrategyListConvertor workflowStrategyListConvertor = new WorkflowStrategyListConvertor();
    private final static FormMetaConvertor formMetaConvertor = new FormMetaConvertor();


    public static WorkflowVersionEntity convert(WorkflowVersion workflowVersion){
        if(workflowVersion==null){
            return null;
        }

        WorkflowVersionEntity entity = new WorkflowVersionEntity();
        if(workflowVersion.getId()>0) {
            entity.setId(workflowVersion.getId());
        }
        entity.setCode(workflowVersion.getCode());
        entity.setTitle(workflowVersion.getTitle());
        entity.setDescription(workflowVersion.getDescription());
        entity.setCurrent(workflowVersion.isCurrent());
        entity.setVersionName(workflowVersion.getVersionName());
        entity.setWorkId(workflowVersion.getWorkId());

        entity.setCreatedOperator(flowOperatorConvertor.convertToDatabaseColumn(workflowVersion.getCreatedOperator()));
        entity.setCreatedTime(workflowVersion.getCreatedTime());
        entity.setUpdatedTime(workflowVersion.getUpdatedTime());
        entity.setForm(formMetaConvertor.convertToDatabaseColumn(workflowVersion.getForm()));
        entity.setOperatorCreateScript(operatorMatchScriptConvertor.convertToDatabaseColumn(workflowVersion.getOperatorCreateScript()));
        entity.setNodes(flowNodeListConvertor.convertToDatabaseColumn(workflowVersion.getNodes()));
        entity.setStrategies(workflowStrategyListConvertor.convertToDatabaseColumn(workflowVersion.getStrategies()));
        entity.setEnable(workflowVersion.isEnable());
        return entity;
    }

    public static WorkflowVersion convert(WorkflowVersionEntity entity){
        if(entity==null){
            return null;
        }
        return new WorkflowVersion(
                entity.getId(),
                entity.getVersionName(),
                entity.getCurrent(),
                entity.getWorkId(),
                entity.getCode(),
                entity.getTitle(),
                entity.getDescription(),
                flowOperatorConvertor.convertToEntityAttribute(entity.getCreatedOperator()),
                entity.getCreatedTime(),
                entity.getUpdatedTime(),
                formMetaConvertor.convertToEntityAttribute(entity.getForm()),
                operatorMatchScriptConvertor.convertToEntityAttribute(entity.getOperatorCreateScript()),
                flowNodeListConvertor.convertToEntityAttribute(entity.getNodes()),
                workflowStrategyListConvertor.convertToEntityAttribute(entity.getStrategies()),
                entity.getEnable());
    }
}
