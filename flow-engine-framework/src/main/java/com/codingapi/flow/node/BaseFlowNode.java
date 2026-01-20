package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public abstract class BaseFlowNode implements IFlowNode {

    /**
     * 节点id
     */
    @Getter
    @Setter
    protected String id;
    /**
     * 节点名称
     */
    @Getter
    @Setter
    protected String name;


    @Override
    public void verifyNode(FormMeta form) {

    }

    @Override
    public boolean trigger(FlowSession session) {
        return true;
    }

    @Override
    public void verifySession(FlowSession session) {

    }

    @Override
    public List<FlowRecord> generateNextRecords(FlowSession session) {
        return List.of();
    }

    @Override
    public ActionManager actions() {
        return new ActionManager(new ArrayList<>());
    }
}
