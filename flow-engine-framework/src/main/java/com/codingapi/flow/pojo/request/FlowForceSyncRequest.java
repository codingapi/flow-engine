package com.codingapi.flow.pojo.request;

import com.codingapi.flow.exception.FlowValidationException;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 运行中流程实例强制同步最新流程设计请求（独立接口，不属于常规审批动作）。
 *
 * <p>强制同步只换绑运行时快照引用：选中实例的记录改指向最新设计对应的快照，
 * 未选中的实例保持原快照不变。同步前会校验选中实例的结构与最新设计一致。</p>
 */
@Data
@NoArgsConstructor
public class FlowForceSyncRequest {

    /**
     * 流程编码
     */
    private String workCode;

    /**
     * 选中同步的流程实例唯一标识列表（未选中的实例不会被修改）
     */
    private List<String> processIds;

    /**
     * 操作者
     */
    private long operatorId;

    /**
     * 操作者身份标识（issue #288），为空表示默认身份
     */
    private String operatorIdentity;

    public FlowForceSyncRequest(String workCode, List<String> processIds, long operatorId) {
        this.workCode = workCode;
        this.processIds = processIds;
        this.operatorId = operatorId;
    }

    public void verify() {
        if (!StringUtils.hasText(workCode)) {
            throw FlowValidationException.required("workCode");
        }
        if (processIds == null || processIds.isEmpty()) {
            throw FlowValidationException.required("processIds");
        }
        if (processIds.stream().anyMatch(processId -> !StringUtils.hasText(processId))) {
            throw FlowValidationException.required("processIds");
        }
        if (operatorId <= 0) {
            throw FlowValidationException.required("operatorId");
        }
    }
}
