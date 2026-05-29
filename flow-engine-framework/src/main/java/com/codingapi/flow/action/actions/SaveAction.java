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

    public SaveAction() {
        this.id = FlowIDGeneratorGatewayContext.getInstance().generateActionId();
        this.title = "保存";
        this.enable = true;
        this.type = ActionType.SAVE.name();
        this.display = new ActionDisplay(this.title);
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
