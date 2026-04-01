package com.codingapi.flow.pojo.request;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.workflow.Workflow;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 流程发起请求
 */
@Data
public class FlowActionRequest {

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


    public void updateOperatorId(long operatorId) {
        if (advice != null) {
            advice.setOperatorId(operatorId);
        }
    }


    public FlowAdvice toFlowAdvice(Workflow workflow, IFlowAction flowAction) {
        FlowAdvice flowAdvice = new FlowAdvice();
        flowAdvice.setAdvice(advice.getAdvice());
        flowAdvice.setSignKey(advice.getSignKey());
        flowAdvice.setAction(flowAction);
        if (advice.getForwardOperatorIds() != null && !advice.getForwardOperatorIds().isEmpty()) {
            flowAdvice.setForwardOperators(GatewayContext.getInstance().findByIds(advice.getForwardOperatorIds()));
        }
        if (StringUtils.hasText(advice.getBackNodeId())) {
            flowAdvice.setBackNode(workflow.getFlowNode(advice.getBackNodeId()));
        }

        if (StringUtils.hasText(advice.getManualNodeId())) {
            flowAdvice.setManualNode(workflow.getFlowNode(advice.getManualNodeId()));
        }

        return flowAdvice;
    }

    public void verify() {
        if (recordId <= 0) {
            throw FlowValidationException.required("recordId");
        }
        if (formData == null || formData.isEmpty()) {
            throw FlowValidationException.required("formData");
        }
        if (advice == null) {
            throw FlowValidationException.required("advice");
        }
        advice.verify();
    }
}
