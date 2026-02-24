package com.codingapi.flow.pojo.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 流程节点记录请求
 */
@Data
@NoArgsConstructor
public class FlowProcessNodeRequest {
    /**
     * 详情id，可以是workId或者是recordId
     */
    private String id;

    /**
     * 流程的操作人Id
     */
    private long operatorId;

    /**
     * 表单数据
     */
    private Map<String, Object> formData;

    public FlowProcessNodeRequest(long id, long operatorId, Map<String, Object> formData) {
        this.id = String.valueOf(id);
        this.operatorId = operatorId;
        this.formData = formData;
    }

    public FlowProcessNodeRequest(String id, long operatorId, Map<String, Object> formData) {
        this.id = id;
        this.operatorId = operatorId;
        this.formData = formData;
    }

    public boolean isCreateWorkflow() {
        return !id.matches("^[0-9]+$");
    }
}
