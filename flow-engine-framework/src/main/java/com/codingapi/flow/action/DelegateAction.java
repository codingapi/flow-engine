package com.codingapi.flow.action;

import com.codingapi.flow.session.FlowSession;

import java.util.Map;

public class DelegateAction extends BaseAction{

    public DelegateAction() {
    }

    @Override
    public void execute(FlowSession flowSession) {

    }

    public static DelegateAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, DelegateAction.class);
    }
}
