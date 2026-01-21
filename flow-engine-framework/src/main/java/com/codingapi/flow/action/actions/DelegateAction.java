package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 *  委派
 */
public class DelegateAction extends BaseAction {

    public DelegateAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "委派";
        this.type = ActionType.DELEGATE;
        this.display = new ActionDisplay(this.title);
    }

    public static DelegateAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, DelegateAction.class);
    }
}
