package com.codingapi.flow.action;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.List;
import java.util.Map;

public class SaveAction extends BaseAction{

    public SaveAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "保存";
        this.type = ActionType.SAVE;
        this.display = new ActionDisplay(this.title);
    }

    public static SaveAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, SaveAction.class);
    }


    @Override
    public boolean isDone(FlowSession session, FlowRecord currentRecord, List<FlowRecord> currentRecords) {
        // 保存按钮，不创建后续流程
        return false;
    }
}
