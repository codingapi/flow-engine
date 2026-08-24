package com.codingapi.flow.pojo.request;

import com.codingapi.flow.exception.FlowValidationException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 子流程数据重置请求（独立接口，不属于常规审批动作）。
 *
 * <p>重置以「已完成的子流程聚合记录」为目标：由选中的子流程实例流程id定位其所属聚合组，
 * 将该组标记为已取代并重建选中实例，主流程退回子流程节点重新等待。
 * 请求不需要指定子流程节点，节点信息由选中实例所属的聚合记录推导，
 * 重置始终是子流程从头重走一次，不存在跳转到子流程内部某节点的语义。</p>
 */
@Data
@NoArgsConstructor
public class FlowSubProcessResetRequest {

    /**
     * 主流程当前操作记录id（执行重置的待办记录）
     */
    private long recordId;

    /**
     * 操作者
     */
    private long operatorId;

    /**
     * 选中重建的子流程实例流程id列表（须同属一个已完成聚合组，且非空）
     */
    private List<String> resetInstanceProcessIds;

    /**
     * 重置说明（记录在被作废的当前记录上，供审计追溯）
     */
    private String advice;

    public FlowSubProcessResetRequest(long recordId, long operatorId, List<String> resetInstanceProcessIds) {
        this.recordId = recordId;
        this.operatorId = operatorId;
        this.resetInstanceProcessIds = resetInstanceProcessIds;
    }

    public void verify() {
        if (recordId <= 0) {
            throw FlowValidationException.required("recordId");
        }
        if (operatorId <= 0) {
            throw FlowValidationException.required("operatorId");
        }
        if (resetInstanceProcessIds == null || resetInstanceProcessIds.isEmpty()) {
            throw FlowValidationException.required("resetInstanceProcessIds");
        }
    }
}
