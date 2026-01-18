package com.codingapi.flow.node.manager;

import com.codingapi.flow.action.IFlowAction;
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

}
