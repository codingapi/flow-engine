package com.codingapi.flow.infra.convert;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.pojo.response.FlowRecordContent;
import com.codingapi.flow.record.FlowRecord;

public class FlowRecordContentConvertor {


    public static FlowRecordContent convert(FlowTodoRecordEntity todoRecord){
        FlowRecordContent content = new FlowRecordContent();
        content.setProcessId(todoRecord.getProcessId());
        content.setWorkflowRuntimeId(todoRecord.getWorkflowRuntimeId());
        content.setWorkTitle(todoRecord.getWorkTitle());
        content.setWorkCode(todoRecord.getWorkCode());
        content.setNodeId(todoRecord.getNodeId());
        content.setNodeType(todoRecord.getNodeType());
        content.setNodeName(todoRecord.getNodeName());
        content.setTitle(todoRecord.getTitle());
        content.setReadTime(todoRecord.getReadTime());
        content.setCurrentOperatorId(todoRecord.getCurrentOperatorId());
        content.setCurrentOperatorName(todoRecord.getCurrentOperatorName());
        content.setCreateTime(todoRecord.getCreateTime());
        content.setCreateOperatorId(todoRecord.getCreateOperatorId());
        content.setCreateOperatorName(todoRecord.getCreateOperatorName());
        content.setTodoKey(todoRecord.getTodoKey());
        content.setMargeCount(todoRecord.getMargeCount());
        content.setMergeable(todoRecord.getMergeable());
        content.setRecordId(todoRecord.getRecordId());
        content.setTimeoutTime(todoRecord.getTimeoutTime());
        content.setRecordState(FlowRecord.SATE_RECORD_TODO);
        content.setFlowState(FlowRecord.SATE_FLOW_RUNNING);
        content.setNotify(false);
        return content;
    }


    public static FlowRecordContent convert(FlowRecordEntity record){
        FlowRecordContent content = new FlowRecordContent();
        content.setProcessId(record.getProcessId());
        content.setWorkflowRuntimeId(record.getWorkRuntimeId());
        content.setWorkTitle(record.getWorkTitle());
        content.setWorkCode(record.getWorkCode());
        content.setNodeId(record.getNodeId());
        content.setNodeType(record.getNodeType());
        content.setNodeName(record.getNodeName());
        content.setTitle(record.getTitle());
        content.setReadTime(record.getReadTime());
        content.setCurrentOperatorId(record.getCurrentOperatorId());
        content.setCurrentOperatorName(record.getCurrentOperatorName());
        content.setCreateTime(record.getCreateTime());
        content.setCreateOperatorId(record.getCreateOperatorId());
        content.setCreateOperatorName(record.getCreateOperatorName());
        content.setMergeable(record.getMergeable());
        content.setRecordId(record.getId());
        content.setTimeoutTime(record.getTimeoutTime());
        content.setRecordState(record.getRecordState());
        content.setFlowState(record.getFlowState());
        content.setNotify(record.getNotify());
        return content;
    }
}
