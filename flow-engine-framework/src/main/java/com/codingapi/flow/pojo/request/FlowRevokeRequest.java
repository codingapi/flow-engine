package com.codingapi.flow.pojo.request;

import com.codingapi.flow.exception.FlowValidationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public void verify() {
        if (recordId <= 0) {
            throw FlowValidationException.required("recordId");
        }
        if (operatorId <= 0) {
            throw FlowValidationException.required("operatorId");
        }
    }
}
