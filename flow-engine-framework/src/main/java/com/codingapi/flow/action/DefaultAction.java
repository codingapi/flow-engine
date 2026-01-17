package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class DefaultAction extends BaseAction {

    public DefaultAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "默认";
        this.type = ActionType.DELEGATE;
        this.order = 1;
        this.display = new ActionDisplay(this.title);
    }


    public static DefaultAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, DefaultAction.class);
    }
}
