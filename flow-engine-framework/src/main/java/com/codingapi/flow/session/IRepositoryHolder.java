package com.codingapi.flow.session;

import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.domain.UrgeInterval;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.service.impl.FlowActionService;
import com.codingapi.flow.service.impl.FlowDelayTriggerService;

import java.util.List;

/**
 *  资源持有对象
 */
public interface IRepositoryHolder {

    /**
     * 获取流程设计服务
     */
    WorkflowService getWorkflowService();

    /**
     * 获取流程记录服务
     */
    FlowRecordService getFlowRecordService();

    /**
     * 获取流程操作人服务
     */
    FlowOperatorGateway getFlowOperatorGateway();

    /**
     * 构建延迟触发执行服务
     *
     * @param task 延迟任务
     * @return 延迟触发执行服务
     */
    FlowDelayTriggerService createDelayTriggerService(DelayTask task);


    /**
     * 构建流程动作服务
     *
     * @param flowSession 流程会话
     * @return 流程动作服务
     */
    FlowActionService createFlowActionService(FlowSession flowSession);


    /**
     * 构建流程服务
     *
     * @return 流程服务
     */
    FlowService createFlowService();

    /**
     * 获取流程详情
     *
     * @param id 流程id
     * @return 流程详情
     */
    FlowRecord getRecordById(long recordId);

    /**
     * 获取流程操作人
     *
     * @param ids 流程人Id
     * @return 流程操作人
     */
    List<IFlowOperator> findOperatorByIds(List<Long> ids);


    /**
     * 获取流程操作人
     *
     * @param id 人员id
     * @return 流程操作人
     */
    IFlowOperator getOperatorById(long id);


    /**
     * 保存延迟任务
     *
     * @param delayTask 延迟任务
     */
    void saveDelayTask(DelayTask delayTask);

    /**
     * 删除延迟任务
     *
     * @param delayTask 延迟任务
     */
    void deleteDelayTask(DelayTask delayTask);


    /**
     * 流程记录列表
     *
     * @param flowRecords 流程记录
     */
    void saveRecords(List<FlowRecord> flowRecords);

    /**
     * 流程记录
     *
     * @param flowRecord 流程记录
     */
    void saveRecord(FlowRecord flowRecord);

    /**
     * 查询当前的节点下的流程记录
     *
     * @param fromId 上级流程
     * @param nodeId 节点id
     * @return 流程记录
     */
    List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId);

    /**
     * 查询当前流程标识下的流程记录
     *
     * @param processId 当前流程标识
     * @return 流程记录
     */
    List<FlowRecord> findProcessRecords(String processId);

    /**
     * 查询后续的流程记录
     *
     * @param processId 当前流程标识
     * @param currentId 当前流程id
     * @return 流程记录
     */
    List<FlowRecord> findAfterRecords(String processId, long currentId);

    /**
     * 获取并行分支的触发总数
     *
     * @param parallelId 并行id
     * @return 数量
     */
    int getParallelBranchTriggerCount(String parallelId);

    /**
     * 添加并发流程的触发总数
     *
     * @param parallelId 并行id
     */
    void addParallelTriggerCount(String parallelId);

    /**
     * 清空流程并发的触发总数
     *
     * @param parallelId 并行id
     */
    void clearParallelTriggerCount(String parallelId);


    /**
     * 保存催办控制
     *
     * @param interval 催办间隔控制
     */
    void saveUrgeInterval(UrgeInterval interval);


    /**
     * 获取最新的催办控制对象
     *
     * @param processId 任务唯一标识
     * @param recordId  当前流程id
     * @return 催办间隔控制
     */
    UrgeInterval getLatestUrgeInterval(String processId, long recordId);

    /**
     * 获取延迟任务
     *
     * @return 延迟任务列表
     */
    List<DelayTask> findDelayTasks();

    /**
     * 保存节点操作人手动分配信息
     *
     * @param processId   流程实例唯一标识
     * @param nodeId      节点 ID
     * @param operatorIds 操作人 ID 列表
     */
    void saveOperatorAssignment(String processId, String nodeId, List<Long> operatorIds);

    /**
     * 查询节点已分配的操作人 ID 列表
     *
     * @param processId 流程实例唯一标识
     * @param nodeId    节点 ID
     * @return 操作人 ID 列表（未分配时返回空列表）
     */
    List<Long> findAssignedOperatorIds(String processId, String nodeId);
}
