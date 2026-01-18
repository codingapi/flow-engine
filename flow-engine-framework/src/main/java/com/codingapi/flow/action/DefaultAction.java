package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class DefaultAction extends BaseAction {

    public DefaultAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "默认";
        this.type = ActionType.DELEGATE;
        this.display = new ActionDisplay(this.title);
    }


    public static DefaultAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, DefaultAction.class);
    }
}
