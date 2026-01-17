package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class SaveAction extends BaseAction{

    public SaveAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "保存";
        this.type = ActionType.SAVE;
        this.order = 1;
        this.display = new ActionDisplay(this.title);
    }

    public static SaveAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, SaveAction.class);
    }
}
