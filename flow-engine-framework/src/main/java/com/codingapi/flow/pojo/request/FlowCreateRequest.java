package com.codingapi.flow.pojo.request;

import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import lombok.Data;

import java.util.Map;

/**
 * 流程发起请求
 */
@Data
public class FlowCreateRequest {

    /**
     * 工作id
     */
    private String workId;
    /**
     * 表单数据
     */
    private Map<String, Object> formData;

    /**
     * 流程动作
     */
    private String actionId;

    /**
     * 操作者
     */
    private long operatorId;

    public FlowActionRequest toActionRequest(long recordId) {
        FlowActionRequest flowActionRequest = new FlowActionRequest();
        flowActionRequest.setFormData(this.getFormData());
        flowActionRequest.setRecordId(recordId);
        flowActionRequest.setAdvice(new FlowAdviceBody(this.getActionId(), null, this.getOperatorId()));
        flowActionRequest.verify();
        return flowActionRequest;
    }


    public void verify() {
        if (workId == null) {
            throw FlowValidationException.required("workId");
        }
        if (formData == null || formData.isEmpty()) {
            throw FlowValidationException.required("formData");
        }
        if (operatorId <= 0) {
            throw FlowValidationException.required("operatorId");
        }
        if (actionId == null || actionId.isEmpty()) {
            throw FlowValidationException.required("actionId");
        }
    }

}
