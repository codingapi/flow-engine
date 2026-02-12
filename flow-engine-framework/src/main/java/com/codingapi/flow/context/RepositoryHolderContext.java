package com.codingapi.flow.context;

import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.record.FlowTodoMarge;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.impl.FlowActionService;
import com.codingapi.flow.service.impl.FlowDelayTriggerService;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class RepositoryHolderContext {

    @Getter
    private final static RepositoryHolderContext instance = new RepositoryHolderContext();

    private RepositoryHolderContext() {
    }

    @Getter
    private WorkflowRepository workflowRepository;
    @Getter
    private WorkflowBackupRepository workflowBackupRepository;
    @Getter
    private FlowRecordRepository flowRecordRepository;
    @Getter
    private FlowTodoRecordRepository flowTodoRecordRepository;
    @Getter
    private FlowTodoMargeRepository flowTodoMargeRepository;
    @Getter
    private FlowOperatorGateway flowOperatorGateway;
    @Getter
    private ParallelBranchRepository parallelBranchRepository;
    @Getter
    private DelayTaskRepository delayTaskRepository;
    @Getter
    private UrgeIntervalRepository urgeIntervalRepository;

    /**
     * 是否已经注册成功
     */
    public boolean isRegistered() {
        return parallelBranchRepository != null
                && delayTaskRepository != null
                && workflowBackupRepository != null
                && flowRecordRepository != null
                && flowTodoRecordRepository != null
                && flowTodoMargeRepository != null
                && flowOperatorGateway != null
                && workflowRepository != null
                && urgeIntervalRepository != null;
    }


    public void verify() {
        if (!isRegistered()) {
            throw FlowStateException.repositoryNotRegistered();
        }
    }

    public void register(WorkflowRepository workflowRepository,
                         WorkflowBackupRepository workflowBackupRepository,
                         FlowRecordRepository flowRecordRepository,
                         FlowTodoRecordRepository flowTodoRecordRepository,
                         FlowTodoMargeRepository flowTodoMargeRepository,
                         FlowOperatorGateway flowOperatorGateway,
                         ParallelBranchRepository parallelBranchRepository,
                         DelayTaskRepository delayTaskRepository,
                         UrgeIntervalRepository urgeIntervalRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowBackupRepository = workflowBackupRepository;
        this.flowRecordRepository = flowRecordRepository;
        this.flowTodoRecordRepository = flowTodoRecordRepository;
        this.flowTodoMargeRepository = flowTodoMargeRepository;
        this.flowOperatorGateway = flowOperatorGateway;
        this.parallelBranchRepository = parallelBranchRepository;
        this.delayTaskRepository = delayTaskRepository;
        this.urgeIntervalRepository = urgeIntervalRepository;
    }


    /**
     * 构建延迟触发执行服务
     *
     * @param task 延迟任务
     * @return 延迟触发执行服务
     */
    public FlowDelayTriggerService createDelayTriggerService(DelayTask task) {
        this.verify();
        return new FlowDelayTriggerService(task,
                flowOperatorGateway,
                flowRecordRepository,
                workflowBackupRepository);
    }


    /**
     * 构建流程动作服务
     *
     * @param flowSession 流程会话
     * @return 流程动作服务
     */
    public FlowActionService createFlowActionService(FlowSession flowSession) {
        this.verify();
        return new FlowActionService(flowSession.toActionRequest());
    }


    /**
     * 构建流程服务
     *
     * @return 流程服务
     */
    public FlowService createFlowService() {
        this.verify();
        return new FlowService(workflowRepository,
                flowOperatorGateway,
                flowRecordRepository,
                flowTodoRecordRepository,
                flowTodoMargeRepository,
                workflowBackupRepository,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository);
    }

    public FlowRecord getRecordById(long id) {
        return flowRecordRepository.get(id);
    }

    public List<IFlowOperator> findOperatorByIds(List<Long> ids) {
        return flowOperatorGateway.findByIds(ids);
    }


    public IFlowOperator getOperatorById(long id) {
        return flowOperatorGateway.get(id);
    }


    public void saveDelayTask(DelayTask delayTask) {
        delayTaskRepository.save(delayTask);
    }

    public void deleteDelayTask(DelayTask delayTask) {
        delayTaskRepository.delete(delayTask);
    }


    public void saveRecords(List<FlowRecord> flowRecords) {
        FlowRecordRepositoryService flowRecordRepositoryService = new FlowRecordRepositoryService(flowRecords);
        flowRecordRepositoryService.saveAll();
    }

    public void saveRecord(FlowRecord flowRecord) {
        FlowRecordRepositoryService flowRecordRepositoryService = new FlowRecordRepositoryService(flowRecord);
        flowRecordRepositoryService.saveAll();
    }

    public List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId) {
        return flowRecordRepository.findCurrentNodeRecords(fromId, nodeId);
    }

    public List<FlowRecord> findProcessRecords(String processId) {
        return flowRecordRepository.findProcessRecords(processId);
    }

    public List<FlowRecord> findAfterRecords(String processId, long currentId) {
        return flowRecordRepository.findAfterRecords(processId, currentId);
    }

    public int getParallelBranchTriggerCount(String parallelId) {
        return parallelBranchRepository.getTriggerCount(parallelId);
    }

    public void addParallelTriggerCount(String parallelId) {
        parallelBranchRepository.addTriggerCount(parallelId);
    }

    public void clearParallelTriggerCount(String parallelId) {
        parallelBranchRepository.clearTriggerCount(parallelId);
    }

    public List<DelayTask> findDelayTasks() {
        return delayTaskRepository.findAll();
    }


    private static class FlowRecordRepositoryService {

        private final List<FlowRecord> flowRecords;
        private final FlowTodoRecordRepository flowTodoRecordRepository;
        private final FlowTodoMargeRepository flowTodoMargeRepository;
        private final FlowRecordRepository flowRecordRepository;


        public FlowRecordRepositoryService(List<FlowRecord> flowRecords) {
            this.flowTodoRecordRepository = RepositoryHolderContext.getInstance().getFlowTodoRecordRepository();
            this.flowTodoMargeRepository = RepositoryHolderContext.getInstance().getFlowTodoMargeRepository();
            this.flowRecordRepository = RepositoryHolderContext.getInstance().getFlowRecordRepository();
            this.flowRecords = flowRecords;
        }

        public FlowRecordRepositoryService(FlowRecord flowRecord) {
            this.flowTodoRecordRepository = RepositoryHolderContext.getInstance().getFlowTodoRecordRepository();
            this.flowTodoMargeRepository = RepositoryHolderContext.getInstance().getFlowTodoMargeRepository();
            this.flowRecordRepository = RepositoryHolderContext.getInstance().getFlowRecordRepository();
            this.flowRecords = new ArrayList<>();
            this.flowRecords.add(flowRecord);
        }


        private void saveTodoMargeRecords() {
            List<FlowTodoRecord> flowTodoRecords = new ArrayList<>();
            for (FlowRecord flowRecord : flowRecords) {
                if (flowRecord.isTodo()) {
                    FlowTodoRecord todoMargeRecord = null;
                    if (flowRecord.isMergeable()) {
                        todoMargeRecord = flowTodoRecordRepository.getByMageKey(flowRecord.getMergeKey());
                        if (todoMargeRecord == null) {
                            todoMargeRecord = new FlowTodoRecord(flowRecord);
                        } else {
                            todoMargeRecord.update(flowRecord);
                            todoMargeRecord.addMargeCount();
                        }
                    } else {
                        todoMargeRecord = new FlowTodoRecord(flowRecord);
                    }
                    flowTodoRecords.add(todoMargeRecord);
                }
            }
            if (!flowTodoRecords.isEmpty()) {
                flowTodoRecordRepository.saveAll(flowTodoRecords);
            }

            if (!flowTodoRecords.isEmpty()) {
                List<FlowTodoMarge> relationList = new ArrayList<>();
                for (FlowTodoRecord margeRecord : flowTodoRecords) {
                    if(margeRecord.isMergeable()) {
                        relationList.add(new FlowTodoMarge(margeRecord));
                    }
                }
                flowTodoMargeRepository.saveAll(relationList);
            }
        }

        private void saveRecords() {
            if (!flowRecords.isEmpty()) {
                flowRecordRepository.saveAll(flowRecords);
            }
        }


        private void removeTodoMargeRecords() {
            for (FlowRecord flowRecord : flowRecords) {
                if (flowRecord.isDone()) {
                    if (flowRecord.isMergeable()) {
                        FlowTodoRecord todoMargeRecord = flowTodoRecordRepository.getByMageKey(flowRecord.getMergeKey());
                        if(todoMargeRecord!=null) {
                            List<FlowTodoMarge> margeRelations = flowTodoMargeRepository.findByTodoId(todoMargeRecord.getId());
                            if(margeRelations!=null && !margeRelations.isEmpty()) {
                                for (FlowTodoMarge margeRelation : margeRelations) {
                                    if (margeRelation.isRecord(flowRecord.getId())) {
                                        flowTodoMargeRepository.remove(margeRelation);
                                        todoMargeRecord.divMargeCount();
                                        if (todoMargeRecord.hasMargeCount()) {
                                            flowTodoRecordRepository.save(todoMargeRecord);
                                        } else {
                                            flowTodoRecordRepository.remove(todoMargeRecord);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        FlowTodoRecord todoMargeRecord = flowTodoRecordRepository.getByMageKey(flowRecord.getMergeKey());
                        if (todoMargeRecord != null) {
                            flowTodoRecordRepository.remove(todoMargeRecord);
                        }
                    }
                }
            }
        }

        public void saveAll() {
            this.saveRecords();
            this.saveTodoMargeRecords();
            this.removeTodoMargeRecords();
        }


    }

}
