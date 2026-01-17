package com.codingapi.flow.action;

import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class ReturnAction extends BaseAction{

    public ReturnAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "退回";
        this.type = ActionType.RETURN;
        this.order = 1;
        this.display = new ActionDisplay(this.title);
    }

    @Override
    public void execute(FlowSession flowSession) {

    }

    public static ReturnAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, ReturnAction.class);
    }
}
