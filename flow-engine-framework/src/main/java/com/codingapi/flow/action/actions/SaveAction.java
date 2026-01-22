package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 保存
 */
public class SaveAction extends BaseAction {

    public SaveAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "保存";
        this.type = ActionType.SAVE;
        this.display = new ActionDisplay(this.title);
    }

    public static SaveAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, SaveAction.class);
    }


}
