package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class RejectAction extends BaseAction{

    public RejectAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "拒绝";
        this.type = ActionType.REJECT;
        this.display = new ActionDisplay(this.title);
    }

    public static RejectAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, RejectAction.class);
    }
}
