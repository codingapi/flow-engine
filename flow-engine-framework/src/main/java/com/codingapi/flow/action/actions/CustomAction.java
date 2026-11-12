package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.script.action.ActionCustomScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 自定义动作
 * <p>
 * 分为前端动作和后端动作两种，针对后端动作会执行 {@link CustomAction#script}的逻辑
 * 针对前端会在前端抛出 {@link CustomAction#triggerFrontEvent} 事件
 *
 */
public class CustomAction extends BaseAction {

    public static final String DEFAULT_TITLE = "自定义";

    /**
     * 后端处理脚本
     */
    private ActionCustomScript script;

    /**
     * 前端执行动作
     */
    private String triggerFrontEvent;


    public static CustomAction defaultAction() {
        CustomAction action = new CustomAction();
        action.setId(FlowIDGeneratorGatewayContext.getInstance().generateActionId());
        action.setTitle(DEFAULT_TITLE);
        action.setEnable(true);
        action.setType(ActionType.CUSTOM.name());
        action.setDisplay(ActionDisplay.defaultDisplay(DEFAULT_TITLE));
        action.script = ActionCustomScript.defaultScript();
        return action;
    }


    @Override
    public void run(FlowSession flowSession) {
        if (StringUtils.hasText(triggerFrontEvent)) {
            throw new FlowExecutionException("custom.action", "前端脚本后端无法执行");
        }
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
        if (StringUtils.hasText(script)) {
            action.script = new ActionCustomScript(script);
        }
        action.triggerFrontEvent = (String) data.get("triggerFrontEvent");

        return action;
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = super.toMap();
        if (script != null) {
            data.put("script", script.getScript());
            data.put("triggerType", script.getTriggerType());
        }
        if (triggerFrontEvent != null) {
            data.put("triggerFrontEvent", triggerFrontEvent);
        }
        return data;
    }
}
