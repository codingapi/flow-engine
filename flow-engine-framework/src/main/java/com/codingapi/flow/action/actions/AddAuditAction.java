package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 加签
 */
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
