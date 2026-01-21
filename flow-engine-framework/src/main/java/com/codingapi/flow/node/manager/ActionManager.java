package com.codingapi.flow.node.manager;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.ReturnAction;
import com.codingapi.flow.action.actions.SaveAction;
import com.codingapi.flow.action.actions.TransferAction;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 *  节点动作管理
 */
@AllArgsConstructor
public class ActionManager {

    @Getter
    private final List<IFlowAction> actions;


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

    public void verify(FormMeta form) {

    }

    public void verifySession(FlowSession session) {
        FlowAdvice flowAdvice = session.getAdvice();

        IFlowAction flowAction = flowAdvice.getAction();
        // 保存操作,不做检查
        if (flowAction instanceof SaveAction) {
            return;
        }
        // 转办操作
        if (flowAction instanceof TransferAction) {
            if (flowAdvice.getTransferOperators() == null || flowAdvice.getTransferOperators().isEmpty()) {
                throw FlowValidationException.required("transferOperators");
            }
        }
        // 退回操作
        if (flowAction instanceof ReturnAction) {
            if (flowAdvice.getBackNode() == null) {
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
}
