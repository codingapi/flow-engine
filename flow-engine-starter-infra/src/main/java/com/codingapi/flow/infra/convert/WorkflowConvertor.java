package com.codingapi.flow.infra.convert;

import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.flow.infra.entity.convert.*;
import com.codingapi.flow.workflow.Workflow;

public class WorkflowConvertor {

    private final static FlowOperatorConvertor flowOperatorConvertor = new FlowOperatorConvertor();
    private final static FlowNodeListConvertor flowNodeListConvertor = new FlowNodeListConvertor();
    private final static OperatorMatchScriptConvertor operatorMatchScriptConvertor = new OperatorMatchScriptConvertor();
    private final static WorkflowStrategyListConvertor workflowStrategyListConvertor = new WorkflowStrategyListConvertor();
    private final static FormMetaConvertor formMetaConvertor = new FormMetaConvertor();


    public static WorkflowEntity convert(Workflow workflow){
        if(workflow==null){
            return null;
        }

        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(workflow.getId());
        entity.setCode(workflow.getCode());
        entity.setTitle(workflow.getTitle());

        entity.setCreatedOperator(flowOperatorConvertor.convertToDatabaseColumn(workflow.getCreatedOperator()));
        entity.setCreatedTime(workflow.getCreatedTime());
        entity.setUpdatedTime(workflow.getUpdatedTime());
        entity.setForm(formMetaConvertor.convertToDatabaseColumn(workflow.getForm()));
        entity.setOperatorCreateScript(operatorMatchScriptConvertor.convertToDatabaseColumn(workflow.getOperatorCreateScript()));
        entity.setNodes(flowNodeListConvertor.convertToDatabaseColumn(workflow.getNodes()));
        entity.setSchema(workflow.getSchema());
        entity.setStrategies(workflowStrategyListConvertor.convertToDatabaseColumn(workflow.getStrategies()));
        entity.setEnable(workflow.isEnable());
        return entity;
    }

    public static Workflow convert(WorkflowEntity entity){
        if(entity==null){
            return null;
        }
        return new Workflow(entity.getId(),
                entity.getCode(),
                entity.getTitle(),
                flowOperatorConvertor.convertToEntityAttribute(entity.getCreatedOperator()),
                entity.getCreatedTime(),
                entity.getUpdatedTime(),
                formMetaConvertor.convertToEntityAttribute(entity.getForm()),
                operatorMatchScriptConvertor.convertToEntityAttribute(entity.getOperatorCreateScript()),
                flowNodeListConvertor.convertToEntityAttribute(entity.getNodes()),
                entity.getSchema(),
                workflowStrategyListConvertor.convertToEntityAttribute(entity.getStrategies()),
                entity.getEnable());
    }
}
