package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class DelegateAction extends BaseAction{

    public DelegateAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "委派";
        this.type = ActionType.DELEGATE;
        this.display = new ActionDisplay(this.title);
    }

    public static DelegateAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, DelegateAction.class);
    }
}
