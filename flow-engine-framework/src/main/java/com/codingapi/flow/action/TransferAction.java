package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 *  转办
 */
public class TransferAction extends BaseAction {

    public TransferAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "转办";
        this.type = ActionType.TRANSFER;
        this.display = new ActionDisplay(this.title);
    }

    public static TransferAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, TransferAction.class);
    }

}
