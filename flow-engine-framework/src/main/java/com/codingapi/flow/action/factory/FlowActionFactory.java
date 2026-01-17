package com.codingapi.flow.action.factory;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.FlowAction;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class FlowActionFactory {

    @Getter
    private final static FlowActionFactory instance = new FlowActionFactory();

    private final Map<ActionType, String> typeTitles = new HashMap<>();

    private FlowActionFactory() {
        this.init();
    }

    private void init() {
        this.typeTitles.put(ActionType.PASS, "同意");
        this.typeTitles.put(ActionType.REJECT, "拒绝");
        this.typeTitles.put(ActionType.RETURN, "退回");
        this.typeTitles.put(ActionType.TRANSFER, "转办");
        this.typeTitles.put(ActionType.DELEGATE, "委派");
        this.typeTitles.put(ActionType.ADD_SIGN, "加签");
    }

    public FlowAction create(ActionType type) {
        FlowAction action = new FlowAction();
        action.setId(RandomUtils.generateStringId());
        action.setType(type);
        action.setOrder(1);
        action.setTitle(this.typeTitles.get(type));
        return action;
    }

    public FlowAction defaultAction() {
        FlowAction action = new FlowAction();
        action.setId(RandomUtils.generateStringId());
        action.setType(ActionType.DEFAULT);
        action.setOrder(1);
        action.setTitle("默认");
        return action;
    }

}
