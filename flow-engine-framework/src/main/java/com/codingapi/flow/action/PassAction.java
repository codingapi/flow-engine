package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class PassAction extends BaseAction{

    public PassAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "通过";
        this.type = ActionType.PASS;
        this.order = 1;
        this.display = new ActionDisplay(this.title);
    }

    public static PassAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, PassAction.class);
    }
}
