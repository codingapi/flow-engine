package com.codingapi.flow.action;

import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class DefaultAction extends BaseAction {

    public DefaultAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "默认";
        this.type = ActionType.DEFAULT;
        this.order = 1;
    }


    @Override
    public void execute(FlowSession flowSession) {

    }

    public static DefaultAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, DefaultAction.class);
    }
}
