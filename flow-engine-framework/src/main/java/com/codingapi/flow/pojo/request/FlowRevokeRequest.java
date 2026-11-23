package com.codingapi.flow.pojo.request;

import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.operator.OperatorIdentity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  流程撤回请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowRevokeRequest {

    /**
     * 记录id
     */
    private long recordId;
    /**
     * 操作者
     */
    private long operatorId;

    /**
     * 操作者身份标识（issue #288），为空表示默认身份
     */
    private String operatorIdentity;

    /**
     * 兼容历史调用的双参构造（默认身份）
     */
    public FlowRevokeRequest(long recordId, long operatorId) {
        this(recordId, operatorId, OperatorIdentity.DEFAULT);
    }

    public void verify() {
        if (recordId <= 0) {
            throw FlowValidationException.required("recordId");
        }
        if (operatorId <= 0) {
            throw FlowValidationException.required("operatorId");
        }
    }
}
