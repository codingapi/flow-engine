package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.strategy.INodeStrategy;
import com.codingapi.flow.strategy.RecordMergeStrategy;
import com.codingapi.flow.strategy.TimeoutStrategy;
import com.codingapi.flow.operator.NodeOperators;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;

import java.util.List;
import java.util.Map;

/**
 * 流程节点
 */
public interface IFlowNode {

    /**
     * 节点id
     */
    String getId();

    /**
     * 节点名称
     */
    String getName();

    /**
     * 节点视图
     */
    String getView();

    /**
     * 流程类型
     */
    String getType();

    /**
     * 节点动作
     */
    List<IFlowAction> actions();

    /**
     * 获取节点动作
     *
     * @param id 动作id
     */
    IFlowAction getActionById(String id);

    /**
     * 表单字段权限设置
     */
    List<FormFieldPermission> formFieldsPermissions();

    /**
     * 节点参与用户
     */
    NodeOperators operators(FlowSession flowSession);

    /**
     * 构建待办标题
     */
    String generateTitle(FlowSession flowSession);


    /**
     * 错误异常处理
     */
    ErrorThrow errorTrigger(FlowSession flowSession);

    /**
     * 节点验证
     */
    void verify(FormMeta form);


    /**
     * 转为map
     */
    Map<String, Object> toMap();


    /**
     * 节点策略
     */
    List<INodeStrategy> strategies();


    /**
     * 节点是否完成
     *
     * @param session        会话对象
     * @param action         节点动作
     * @param currentRecords 当前节点的记录
     * @return true:完成 false:未完成
     */
    boolean isDone(FlowSession session, IFlowAction action, List<FlowRecord> currentRecords);

    /**
     * 获取超时时间
     */
    default long getTimeoutTime() {
        List<INodeStrategy> strategies = this.strategies();
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof TimeoutStrategy) {
                return System.currentTimeMillis() + ((TimeoutStrategy) strategy).getTimeoutTime();
            }
        }
        return 0;
    }

    /**
     * 是否可合并
     */
    default boolean isMergeable() {
        List<INodeStrategy> strategies = this.strategies();
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof RecordMergeStrategy) {
                return ((RecordMergeStrategy) strategy).isMergeable();
            }
        }
        return false;
    }

}
