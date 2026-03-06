package com.codingapi.flow.manager;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.*;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.HandleNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 节点动作管理
 */
@AllArgsConstructor
public class ActionManager {

    @Getter
    private final List<IFlowAction> actions;


    /**
     * 获取节点动作
     *
     */
    public IFlowAction getActionByType(String type) {
        for (IFlowAction action : actions) {
            if (action.type().equals(type)) {
                return action;
            }
        }
        return null;
    }

    /**
     * 获取节点动作
     *
     * @param id 动作id
     */
    public IFlowAction getActionById(String id) {
        for (IFlowAction action : actions) {
            if (action.id().equals(id)) {
                return action;
            }
        }
        return null;
    }

    public void verify(FlowForm form) {

    }

    public void verifySession(FlowSession session) {
        FlowAdvice flowAdvice = session.getAdvice();
        NodeStrategyManager nodeStrategyManager = session.getCurrentNode().strategyManager();

        IFlowAction flowAction = flowAdvice.getAction();
        // 保存操作,不做检查
        if (flowAction instanceof SaveAction) {
            return;
        }

        // 自定义的动作
        if (flowAction instanceof CustomAction) {
            return;
        }

        //  通过操作
        if (flowAction instanceof PassAction ) {
            session.getFormData().verify();

            // 是否必须填写审批意见
            if (nodeStrategyManager.isAdviceRequired()) {
                if (!StringUtils.hasText(flowAdvice.getAdvice())) {
                    throw FlowValidationException.required("advice");
                }
            }

            // 是否必须签名
            if (nodeStrategyManager.isSignRequired()) {
                if (!StringUtils.hasText(flowAdvice.getSignKey())) {
                    throw FlowValidationException.required("signKey");
                }
            }
        }

        //  通过操作
        if (flowAction instanceof RejectAction ) {
            // 是否必须填写审批意见
            if (nodeStrategyManager.isAdviceRequired()) {
                if (!StringUtils.hasText(flowAdvice.getAdvice())) {
                    throw FlowValidationException.required("advice");
                }
            }
        }

        // 加签操作、转办操作、委托操作
        if (flowAction instanceof AddAuditAction || flowAction instanceof TransferAction || flowAction instanceof DelegateAction) {
            if (flowAdvice.getForwardOperators() == null || flowAdvice.getForwardOperators().isEmpty()) {
                throw FlowValidationException.required("forwardOperators");
            }
        }

        // 退回操作
        if (flowAction instanceof ReturnAction) {
            if (flowAdvice.getBackNode() == null) {
                throw FlowValidationException.required("backNode");
            }
            if (flowAdvice.getBackNode().getType().equals(EndNode.NODE_TYPE)) {
                throw FlowValidationException.required("backNode");
            }
            IFlowNode backNode = flowAdvice.getBackNode();
            IFlowNode currentNode = session.getCurrentNode();
            if (currentNode.equals(backNode)) {
                throw FlowValidationException.required("backNode");
            }
            Workflow workflow = session.getWorkflow();
            // 退回节点不能是当前节点的后续节点
            if (workflow.isNextNode(currentNode, backNode)) {
                throw FlowValidationException.required("backNode");
            }
            if (!(backNode.getType().equals(StartNode.NODE_TYPE)
                    || backNode.getType().equals(ApprovalNode.NODE_TYPE)
                    || backNode.getType().equals(HandleNode.NODE_TYPE))) {
                throw FlowValidationException.required("backNode");
            }
        }


    }

    public IFlowAction getAction(Class<? extends IFlowAction> clazz) {
        for (IFlowAction action : actions) {
            if (action.getClass() == clazz) {
                return action;
            }
        }
        return null;
    }

    public IFlowAction getFirstAction() {
        return actions.get(0);
    }
}
