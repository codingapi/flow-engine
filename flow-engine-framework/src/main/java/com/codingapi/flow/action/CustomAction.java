package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class CustomAction extends BaseAction{

    public CustomAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "自定义";
        this.type = ActionType.CUSTOM;
        this.display = new ActionDisplay(this.title);
    }

    public static CustomAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, CustomAction.class);
    }
}
