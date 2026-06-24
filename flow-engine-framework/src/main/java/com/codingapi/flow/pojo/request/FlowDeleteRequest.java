package com.codingapi.flow.pojo.request;

import com.codingapi.flow.exception.FlowValidationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程删除请求
 * <p>
 * 仅用于删除位于开始节点且尚未流转的流程实例
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowDeleteRequest {

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
