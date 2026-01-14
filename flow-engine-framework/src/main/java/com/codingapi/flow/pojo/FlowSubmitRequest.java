package com.codingapi.flow.pojo;

import com.codingapi.flow.form.FormData;
import com.codingapi.flow.record.FlowAdvice;
import lombok.Data;

/**
 * 流程发起请求
 */
@Data
public class FlowSubmitRequest {

    private long recordId;
    private FormData formData;
    private FlowAdvice advice;
}
