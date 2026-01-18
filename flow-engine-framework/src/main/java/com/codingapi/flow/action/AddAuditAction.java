package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class AddAuditAction extends BaseAction {

    public AddAuditAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "加签";
        this.type = ActionType.ADD_AUDIT;
        this.display = new ActionDisplay(this.title);
    }

    public static AddAuditAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, AddAuditAction.class);
    }
}
