package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class CustomAction extends BaseAction{

    public CustomAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "自定义";
        this.type = ActionType.CUSTOM;
        this.order = 1;
        this.display = new ActionDisplay(this.title);
    }

    public static CustomAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, CustomAction.class);
    }
}
