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

}
