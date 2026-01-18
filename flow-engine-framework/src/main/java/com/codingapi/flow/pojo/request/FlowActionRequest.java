package com.codingapi.flow.pojo.request;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.context.GatewayContext;
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


    public FlowAdvice toFlowAdvice(Workflow workflow, IFlowAction flowAction) {
        FlowAdvice flowAdvice = new FlowAdvice();
        flowAdvice.setAdvice(advice.getAdvice());
        flowAdvice.setSignKey(advice.getSignKey());
        flowAdvice.setAction(flowAction);
        if (advice.getTransferOperatorIds() != null && !advice.getTransferOperatorIds().isEmpty()) {
            flowAdvice.setTransferOperators(GatewayContext.getInstance().findByIds(advice.getTransferOperatorIds()));
        }
        if (StringUtils.hasText(advice.getBackNodeId())) {
            flowAdvice.setBackNode(workflow.getNode(advice.getBackNodeId()));
        }

        return flowAdvice;
    }

    public void verify() {
        if (recordId <= 0) {
            throw new IllegalArgumentException("recordId can not be null");
        }
        if (formData == null || formData.isEmpty()) {
            throw new IllegalArgumentException("formData can not be null");
        }
        if (advice == null) {
            throw new IllegalArgumentException("advice can not be null");
        }
        advice.verify();
    }
}
