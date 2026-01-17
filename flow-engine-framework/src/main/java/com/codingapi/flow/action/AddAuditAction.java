package com.codingapi.flow.action;

import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class AddAuditAction extends BaseAction{

    public AddAuditAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "加签";
        this.type = ActionType.ADD_AUDIT;
        this.order = 1;
        this.display = new ActionDisplay(this.title);
    }

    @Override
    public void execute(FlowSession flowSession) {

    }

    public static AddAuditAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, AddAuditAction.class);
    }
}
