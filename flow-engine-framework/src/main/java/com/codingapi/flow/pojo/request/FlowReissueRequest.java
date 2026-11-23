package com.codingapi.flow.pojo.request;

import com.codingapi.flow.exception.FlowValidationException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 已完成流程的二次发起请求（独立接口，不属于常规审批动作）。
 *
 * <p>二次发起以「已正常完成的流程记录」为目标：复制源流程的表单数据创建一条全新的流程，
 * 新流程停在开始节点成为发起人的待办，由发起人调整数据后重新提交执行。
 * 源流程记录不做任何改写（不作废、不迁移数据）。</p>
 */
@Data
@NoArgsConstructor
public class FlowReissueRequest {

    /**
     * 被二次发起的源流程记录id（必须是已正常完成的流程记录）
     */
    private long sourceRecordId;

    /**
     * 操作者（必须是流程管理员）
     */
    private long operatorId;

    /**
     * 操作者身份标识（issue #288），为空表示默认身份
     */
    private String operatorIdentity;

    /**
     * 二次发起时使用的表单数据；为空时按源流程记录的表单数据复制
     */
    private Map<String, Object> formData;

    /**
     * 二次发起说明
     */
    private String advice;

    public FlowReissueRequest(long sourceRecordId, long operatorId) {
        this.sourceRecordId = sourceRecordId;
        this.operatorId = operatorId;
    }

    public void verify() {
        if (sourceRecordId <= 0) {
            throw FlowValidationException.required("sourceRecordId");
        }
        if (operatorId <= 0) {
            throw FlowValidationException.required("operatorId");
        }
    }
}
