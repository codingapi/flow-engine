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

import java.util.ArrayList;
import java.util.List;

/**
 * 节点动作管理
 */
@AllArgsConstructor
public class ActionManager {

    @Getter
    private final List<IFlowAction> actions;

    public List<IFlowAction> filterActions(FlowSession flowSession) {
        if (this.actions == null || this.actions.isEmpty()) {
            return new ArrayList<>();
        }
        List<IFlowAction> actions = new ArrayList<>();
        for (IFlowAction action : this.actions) {
            if (action.enable() && action.show(flowSession)) {
                actions.add(action);
            }
        }
        return actions;
    }


    public void verify(FlowForm flowForm) {
        // no verify 
    }


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

        // 通过操作
        if (flowAction instanceof PassAction) {
            // 校验表单字段
            session.verifyFormData();

            // 是否必须填写审批意见
            verifyAdviceRequired(nodeStrategyManager, flowAdvice);

            // 是否必须签名
            if (nodeStrategyManager.isSignRequired()) {
                if (!StringUtils.hasText(flowAdvice.getSignKey())) {
                    throw FlowValidationException.required("signKey");
                }
            }
        }

        // 拒绝操作
        if (flowAction instanceof RejectAction) {
            // 是否必须填写审批意见
            verifyAdviceRequired(nodeStrategyManager, flowAdvice);
        }

        // 转办操作、委托操作
        if (flowAction instanceof TransferAction || flowAction instanceof DelegateAction) {
            // 是否必须填写审批意见（意见记录在当前办理记录上）
            verifyAdviceRequired(nodeStrategyManager, flowAdvice);
            if (flowAdvice.getForwardOperators() == null || flowAdvice.getForwardOperators().isEmpty()) {
                throw FlowValidationException.required("forwardOperators");
            }
        }

        // 加签操作（加签意见暂不持久化，因此不校验意见必填）
        if (flowAction instanceof AddAuditAction) {
            if (flowAdvice.getForwardOperators() == null || flowAdvice.getForwardOperators().isEmpty()) {
                throw FlowValidationException.required("forwardOperators");
            }
        }

        // 减签操作（意见记录在执行人自己的记录上，不校验意见必填）
        if (flowAction instanceof ReduceAuditAction) {
            if (flowAdvice.getForwardOperators() == null || flowAdvice.getForwardOperators().isEmpty()) {
                throw FlowValidationException.required("forwardOperators");
            }
        }

        // 退回操作
        if (flowAction instanceof ReturnAction) {
            // 是否必须填写审批意见（意见记录在当前办理记录上）
            verifyAdviceRequired(nodeStrategyManager, flowAdvice);
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

    /**
     * 校验审批意见是否必填。
     *
     * <p>节点开启「隐藏审批意见」时，{@link NodeStrategyManager#isAdviceRequired()} 已放宽为 false，
     * 避免用户没有输入入口却被服务端拦截。适用于所有会记录审批意见的动作
     * （通过、拒绝、退回、转办、委派）。
     *
     * @param nodeStrategyManager 节点策略
     * @param flowAdvice          审批意见
     */
    private void verifyAdviceRequired(NodeStrategyManager nodeStrategyManager, FlowAdvice flowAdvice) {
        if (nodeStrategyManager.isAdviceRequired() && !StringUtils.hasText(flowAdvice.getAdvice())) {
            throw FlowValidationException.required("advice");
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
