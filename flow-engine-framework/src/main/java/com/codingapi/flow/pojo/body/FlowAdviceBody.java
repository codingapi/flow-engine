package com.codingapi.flow.pojo.body;

import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.session.FlowSession;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 流程审批意见
 */
@Data
@NoArgsConstructor
public class FlowAdviceBody {

    /**
     * 流程动作
     */
    private String actionId;
    /**
     * 审批意见
     */
    private String advice;

    /**
     * 人工选择节点
     */
    private String manualNodeId;

    /**
     * 签名key
     */
    private String signKey;
    /**
     * 操作者
     */
    private long operatorId;

    /**
     * 退回节点
     */
    private String backNodeId;

    /**
     * 转办人员
     */
    private List<Long> forwardOperatorIds;

    /**
     * 操作人手动选择映射（节点ID -> 操作人ID列表）
     */
    private Map<String, List<Long>> operatorSelectMap;


    public FlowAdviceBody(String actionId, String advice, long operatorId) {
        this.actionId = actionId;
        this.advice = advice;
        this.operatorId = operatorId;
    }


    public FlowAdviceBody(FlowSession flowSession) {
        this.actionId = flowSession.getCurrentAction().id();
        this.advice = flowSession.getAdvice().getAdvice();
        this.operatorId = flowSession.getCurrentOperator().getUserId();
        this.signKey = flowSession.getAdvice().getSignKey();
        this.backNodeId = flowSession.getAdvice().getBackNode() != null ? flowSession.getAdvice().getBackNode().getId() : null;
        this.manualNodeId = flowSession.getAdvice().getManualNode() != null ? flowSession.getAdvice().getManualNode().getId() : null;
        this.forwardOperatorIds = flowSession.getAdvice().getForwardOperators() != null ? flowSession.getAdvice().getForwardOperators().stream().map(IFlowOperator::getUserId).toList() : null;
        this.operatorSelectMap = flowSession.getAdvice().getOperatorSelectMap();
    }


    public FlowAdviceBody(String actionId, long operatorId) {
        this(actionId, null, operatorId);
    }

    public void verify() {
        if (actionId == null || actionId.isEmpty()) {
            throw FlowValidationException.required("actionId");
        }
        if (operatorId <= 0) {
            throw FlowValidationException.required("operatorId");
        }
    }
}
