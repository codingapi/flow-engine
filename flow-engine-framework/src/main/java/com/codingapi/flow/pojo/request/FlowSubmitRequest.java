package com.codingapi.flow.pojo.request;

import com.codingapi.flow.pojo.body.FlowAdviceBody;
import lombok.Data;

import java.util.Map;

/**
 * 流程发起请求
 */
@Data
public class FlowSubmitRequest {

    /**
     * 记录id
     */
    private long recordId;
    /**
     * 表单数据
     */
    private Map<String, Object> formData;
    /**
     * 审批意见
     */
    private FlowAdviceBody advice;


    public void verify() {
        if (recordId <= 0) {
            throw new IllegalArgumentException("recordId can not be null");
        }
        if (formData == null || formData.isEmpty()) {
            throw new IllegalArgumentException("formData can not be null");
        }
        if (advice == null) {
            throw new IllegalArgumentException("advice can not be null");
        }
        advice.verify();
    }
}
