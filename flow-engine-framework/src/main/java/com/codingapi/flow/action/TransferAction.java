package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class TransferAction extends BaseAction{

    public TransferAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "转办";
        this.type = ActionType.TRANSFER;
        this.order = 1;
        this.display = new ActionDisplay(this.title);
    }

    public static TransferAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, TransferAction.class);
    }
}
