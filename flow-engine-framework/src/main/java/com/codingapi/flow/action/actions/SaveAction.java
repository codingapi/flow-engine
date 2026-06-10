package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;

import java.util.Map;

/**
 * 保存
 */
public class SaveAction extends BaseAction {


    public static final String DEFAULT_TITLE = "保存";


    public static SaveAction defaultAction() {
        SaveAction action = new SaveAction();
        action.setId(FlowIDGeneratorGatewayContext.getInstance().generateActionId());
        action.setTitle(DEFAULT_TITLE);
        action.setEnable(true);
        action.setType(ActionType.SAVE.name());
        action.setDisplay(ActionDisplay.defaultDisplay(DEFAULT_TITLE));
        return action;
    }

    public static SaveAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, SaveAction.class);
    }


    @Override
    public void run(FlowSession flowSession) {
        IRepositoryHolder repositoryHolder = flowSession.getRepositoryHolder();
        FlowRecord flowRecord = flowSession.getCurrentRecord();
        flowRecord.update(flowSession, false);
        repositoryHolder.saveRecord(flowRecord);
    }
}
