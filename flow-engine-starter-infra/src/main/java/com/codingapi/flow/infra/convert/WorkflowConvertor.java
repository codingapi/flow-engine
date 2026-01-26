package com.codingapi.flow.infra.convert;

import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.flow.workflow.Workflow;

public class WorkflowConvertor {

    public static WorkflowEntity convert(Workflow workflow){
        if(workflow==null){
            return null;
        }

        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(workflow.getId());
        entity.setCode(workflow.getCode());
        entity.setTitle(workflow.getTitle());
        entity.setCreatedOperator(workflow.getCreatedOperator());
        entity.setCreatedTime(workflow.getCreatedTime());
        entity.setForm(workflow.getForm());
        entity.setOperatorCreateScript(workflow.getOperatorCreateScript());
        entity.setNodes(workflow.getNodes());
        entity.setEdges(workflow.getEdges());
        entity.setSchema(workflow.getSchema());
        entity.setStrategies(workflow.getStrategies());
        return entity;
    }

    public static Workflow convert(WorkflowEntity entity){
        if(entity==null){
            return null;
        }
        return new Workflow(entity.getId(), entity.getCode(), entity.getTitle(), entity.getCreatedOperator(), entity.getCreatedTime(), entity.getForm(), entity.getOperatorCreateScript(), entity.getNodes(), entity.getEdges(), entity.getSchema(), entity.getStrategies());
    }
}
