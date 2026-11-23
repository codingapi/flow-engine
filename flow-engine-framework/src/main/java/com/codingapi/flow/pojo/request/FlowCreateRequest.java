package com.codingapi.flow.pojo.request;

import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.operator.OperatorKey;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程发起请求
 */
@Data
public class FlowCreateRequest {


    /**
     * 工作id
     */
    private String workCode;
    /**
     * 表单数据
     */
    private Map<String, Object> formData;

    /**
     * 流程动作
     */
    private String actionId;

    /**
     * 操作者
     */
    private long operatorId;

    /**
     * 操作者身份标识（issue #288）
     * <p>框架不解释身份语义，只透传；为空表示默认身份，与历史行为一致。
     */
    private String operatorIdentity;

    /**
     * 父流程id,系统自动赋值
     */
    private long parentRecordId;

    /**
     * 发起人手动设定的操作人映射（节点ID -> 操作人ID列表）
     * 用于 INITIATOR_SELECT 模式的节点
     * <p>存量字段，仅表达人员 id（默认身份）；需要携带身份时使用 {@link #operatorSelectOperators}。
     */
    private Map<String, List<Long>> operatorSelectMap;

    /**
     * 发起人手动设定的操作人映射，携带身份（issue #288 衍生 / #292）
     * <p>节点ID -> 选定操作人列表，每项为 {@code (人员id, 身份)}；同一人员的两个身份是两项，不会被合并。
     * 该字段优先于 {@link #operatorSelectMap}，两者都不传则不落分配关系。
     */
    private Map<String, List<OperatorKey>> operatorSelectOperators;

    /**
     * 手动传入的流程标题（子流程实例区分用）；为空时回落流程定义标题
     */
    private String workTitle;


    public FlowActionRequest toActionRequest(long recordId) {
        FlowActionRequest flowActionRequest = new FlowActionRequest();
        flowActionRequest.setFormData(this.getFormData());
        flowActionRequest.setRecordId(recordId);
        FlowAdviceBody adviceBody = new FlowAdviceBody(this.getActionId(), null, this.getOperatorId());
        adviceBody.setOperatorIdentity(this.getOperatorIdentity());
        adviceBody.setOperatorSelectMap(this.getOperatorSelectMap());
        adviceBody.setOperatorSelectOperators(this.getOperatorSelectOperators());
        flowActionRequest.setAdvice(adviceBody);
        flowActionRequest.verify();
        return flowActionRequest;
    }


    public void verify() {
        if (workCode == null) {
            throw FlowValidationException.required("workCode");
        }
        if (formData == null || formData.isEmpty()) {
            throw FlowValidationException.required("formData");
        }
        if (operatorId <= 0) {
            throw FlowValidationException.required("operatorId");
        }
        if (actionId == null || actionId.isEmpty()) {
            throw FlowValidationException.required("actionId");
        }
    }

    public boolean isSubProcess() {
        return this.parentRecordId!=0;
    }
}
