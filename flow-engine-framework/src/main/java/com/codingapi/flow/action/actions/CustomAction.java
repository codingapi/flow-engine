package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.script.action.CustomScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.utils.RandomUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 自定义
 */
public class CustomAction extends BaseAction {


    private CustomScript script;


    public void setCustomScript(String script) {
        if (StringUtils.hasText(script)) {
            this.script = new CustomScript(script);
        }
    }

    public CustomAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "自定义";
        this.enable = true;
        this.type = ActionType.CUSTOM.name();
        this.display = new ActionDisplay(this.title);
        this.script = CustomScript.defaultScript();
    }

    @Override
    public void run(FlowSession flowSession) {
        IRepositoryHolder repositoryHolder = flowSession.getRepositoryHolder();
        String actionType = script.execute(flowSession);
        IFlowNode currentNode = flowSession.getCurrentNode();
        ActionManager actionManager = currentNode.actionManager();

        IFlowAction nextAction = actionManager.getActionByType(actionType);

        if (nextAction == null) {
            throw FlowExecutionException.customActionNextNotFound();
        }

        FlowSession triggerSession = flowSession.updateSession(nextAction);
        repositoryHolder.createFlowActionService(triggerSession).action();
    }

    @Override
    public void copy(IFlowAction action) {
        super.copy(action);
        this.script = ((CustomAction) action).script;
    }

    public static CustomAction fromMap(Map<String, Object> data) {
        CustomAction action = BaseAction.fromMap(data, CustomAction.class);
        String script = (String) data.get("script");
        action.setCustomScript(script);
        return action;
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = super.toMap();
        if (script != null) {
            data.put("script", script.getScript());
        }
        return data;
    }
}
