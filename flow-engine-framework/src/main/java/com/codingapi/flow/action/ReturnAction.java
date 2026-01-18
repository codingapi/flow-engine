package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class ReturnAction extends BaseAction {

    public ReturnAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "退回";
        this.type = ActionType.RETURN;
        this.display = new ActionDisplay(this.title);
    }

    public static ReturnAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, ReturnAction.class);
    }
}
