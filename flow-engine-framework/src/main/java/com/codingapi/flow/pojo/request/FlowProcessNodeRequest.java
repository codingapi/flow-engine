package com.codingapi.flow.pojo.request;

import com.codingapi.flow.operator.OperatorIdentity;
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
     * 详情id，可以是workCode或者是recordId
     */
    private String id;

    /**
     * 流程的操作人Id
     */
    private long operatorId;

    /**
     * 流程操作人的身份标识（issue #288），为空表示默认身份
     */
    private String operatorIdentity;

    /**
     * 表单数据
     */
    private Map<String, Object> formData;


    public FlowProcessNodeRequest(long id, long operatorId, Map<String, Object> formData) {
        this(id, operatorId, OperatorIdentity.DEFAULT, formData);
    }

    public FlowProcessNodeRequest(String id, long operatorId, Map<String, Object> formData) {
        this(id, operatorId, OperatorIdentity.DEFAULT, formData);
    }

    public FlowProcessNodeRequest(long id, long operatorId, String operatorIdentity, Map<String, Object> formData) {
        this(String.valueOf(id), operatorId, operatorIdentity, formData);
    }

    public FlowProcessNodeRequest(String id, long operatorId, String operatorIdentity, Map<String, Object> formData) {
        this.id = id;
        this.operatorId = operatorId;
        this.operatorIdentity = operatorIdentity;
        this.formData = formData;
    }

}
