package com.codingapi.flow.service;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程记录保存服务,负责保存流程记录和待办记录的合并关系
 */
class FlowRecordSaveService {

    private final List<FlowRecord> flowRecords;
    private final FlowRecordService flowRecordService;


    public FlowRecordSaveService(List<FlowRecord> flowRecords) {
        this.flowRecordService = RepositoryHolderContext.getInstance().getFlowRecordService();
        this.flowRecords = flowRecords;
    }

    public FlowRecordSaveService(FlowRecord flowRecord) {
        this.flowRecords = new ArrayList<>();
        this.flowRecords.add(flowRecord);
        this.flowRecordService = RepositoryHolderContext.getInstance().getFlowRecordService();
    }


    private void saveTodoMargeRecords() {
        List<FlowTodoRecord> flowTodoRecords = new ArrayList<>();
        for (FlowRecord flowRecord : flowRecords) {
            if (flowRecord.isTodo()) {
                FlowTodoRecord todoMargeRecord = flowRecordService.getFlowTodoByMergeKey(flowRecord.getMergeKey());
                if (todoMargeRecord == null) {
                    todoMargeRecord = new FlowTodoRecord(flowRecord);
                } else {
                    todoMargeRecord.update(flowRecord);
                    if (flowRecord.isMergeable()) {
                        todoMargeRecord.addMergeCount();
                    }
                }
                flowTodoRecords.add(todoMargeRecord);
            }
        }
        if (!flowTodoRecords.isEmpty()) {
            flowRecordService.saveFlowTodos(flowTodoRecords);
        }

        if (!flowTodoRecords.isEmpty()) {
            List<FlowTodoMerge> relationList = new ArrayList<>();
            for (FlowTodoRecord margeRecord : flowTodoRecords) {
                if (margeRecord.isMergeable()) {
                    relationList.add(new FlowTodoMerge(margeRecord));
                }
            }
            flowRecordService.saveFlowMerges(relationList);
        }
    }

    private void saveRecords() {
        if (!flowRecords.isEmpty()) {
            // 只保存非结束节点的记录,结束节点的记录由流程引擎自动生成,不允许外部修改
            List<FlowRecord> flowRecordList = flowRecords.stream().filter(FlowRecord::isNotEndNode).toList();
            flowRecordService.saveAll(flowRecordList);
        }
    }

    private void removeTodoMergeRecords() {
        for (FlowRecord flowRecord : flowRecords) {
            if (flowRecord.isDone()) {
                if (flowRecord.isMergeable()) {
                    FlowTodoRecord todoMargeRecord = flowRecordService.getFlowTodoByMergeKey(flowRecord.getMergeKey());
                    if (todoMargeRecord != null) {
                        List<FlowTodoMerge> margeRelations = flowRecordService.findFlowTodoMergeByTodoId(todoMargeRecord.getId());
                        if (margeRelations != null && !margeRelations.isEmpty()) {
                            for (FlowTodoMerge margeRelation : margeRelations) {
                                if (margeRelation.isRecord(flowRecord.getId())) {
                                    flowRecordService.deleteFlowMerge(margeRelation);
                                    todoMargeRecord.divMergeCount();
                                    if (todoMargeRecord.hasMergeCount()) {
                                        flowRecordService.saveFlowTodo(todoMargeRecord);
                                    } else {
                                        flowRecordService.deleteFlowTodo(todoMargeRecord);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    FlowTodoRecord flowTodoRecord = flowRecordService.getFlowTodoByMergeKey(flowRecord.getMergeKey());
                    if (flowTodoRecord != null) {
                        flowRecordService.deleteFlowTodo(flowTodoRecord);
                    }
                }
            }
        }
    }

    public void saveAll() {
        this.saveRecords();
        this.saveTodoMargeRecords();
        this.removeTodoMergeRecords();
    }
}
