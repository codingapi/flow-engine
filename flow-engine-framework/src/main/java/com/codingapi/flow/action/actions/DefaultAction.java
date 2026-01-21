package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 *  默认动作，用于无需要操作的节点配置
 */
public class DefaultAction extends BaseAction {

    public DefaultAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "默认";
        this.type = ActionType.DELEGATE;
        this.display = new ActionDisplay(this.title);
    }


    public static DefaultAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, DefaultAction.class);
    }
}
