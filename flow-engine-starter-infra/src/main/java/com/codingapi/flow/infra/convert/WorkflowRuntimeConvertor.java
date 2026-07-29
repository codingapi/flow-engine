package com.codingapi.flow.infra.convert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.flow.infra.entity.WorkflowRuntimeEntity;

import java.util.HashMap;
import java.util.Map;

public class WorkflowRuntimeConvertor {

    public static WorkflowRuntimeEntity convert(WorkflowRuntime workflowRuntime) {
        if (workflowRuntime == null) {
            return null;
        }
        WorkflowRuntimeEntity entity = new WorkflowRuntimeEntity();
        if (workflowRuntime.getId() > 0) {
            entity.setId(workflowRuntime.getId());
        }
        entity.setWorkflow(workflowRuntime.getWorkflow());
        entity.setWorkId(workflowRuntime.getWorkId());
        entity.setWorkCode(workflowRuntime.getWorkCode());
        entity.setWorkVersion(workflowRuntime.getWorkVersion());
        entity.setWorkTitle(workflowRuntime.getWorkTitle());
        entity.setCreateTime(workflowRuntime.getCreateTime());
        entity.setScripts(workflowRuntime.getScripts() == null ? null : JSON.toJSONString(workflowRuntime.getScripts()));
        return entity;
    }

    public static WorkflowRuntime convert(WorkflowRuntimeEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<String, String> scripts = null;
        if (entity.getScripts() != null) {
            scripts = JSON.parseObject(entity.getScripts(), new TypeReference<Map<String, String>>() {
            });
        }
        if (scripts == null) {
            scripts = new HashMap<>();
        }
        return new WorkflowRuntime(entity.getId(), entity.getWorkId(), entity.getWorkCode(), entity.getWorkVersion(), entity.getWorkTitle(), entity.getCreateTime(), entity.getWorkflow(), scripts);
    }
}
