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

    /**
     * 覆盖审批人的身份标识（issue #288），由接口层统一注入当前会话声明的身份
     *
     * @param operatorIdentity 身份标识，{@code null} 与空串等价（默认身份）
     */
    public void updateOperatorIdentity(String operatorIdentity) {
        if (advice != null) {
            advice.setOperatorIdentity(operatorIdentity);
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

        // 传递操作人手动选择映射（含身份，issue #288 衍生 / #292）
        flowAdvice.setOperatorSelectMap(advice.getOperatorSelectMap());
        flowAdvice.setOperatorSelectOperators(advice.getOperatorSelectOperators());

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
