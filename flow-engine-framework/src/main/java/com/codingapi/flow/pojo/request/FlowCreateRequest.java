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
     * 审批意见
     */
    private FlowAdviceBody advice;


    public void verify() {
        if (workId == null) {
            throw FlowValidationException.required("workId");
        }
        if (formData == null || formData.isEmpty()) {
            throw FlowValidationException.required("formData");
        }
        if (advice == null) {
            throw FlowValidationException.required("advice");
        }
        advice.verify();
    }

}
