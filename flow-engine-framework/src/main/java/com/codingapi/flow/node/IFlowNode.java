package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;

import java.util.List;
import java.util.Map;

/**
 * 流程节点
 * 流程执行的生命周期
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
     * 流程类型
     */
    String getType();

    /**
     * 节点顺序,同一层级下的节点顺序，越小则优先级越高
     * @return 节点顺序
     */
    int getOrder();

    /**
     * 转化为map
     */
    Map<String, Object> toMap();

    /**
     * 节点验证
     * 用于流程配置完成以后的验证时触发
     */
    void verifyNode(FormMeta form);

    /**
     * 是否执行节点
     * 当前流程节点执行完成以后，触发下一环节时执行的函数，当返回true时则将继续执行后续流程，当返回false时则不继续执行后续流程，将执行当前节点的创建流程记录函数 {@link  IFlowNode#generateCurrentRecords(FlowSession)}
     * 同时 continueTrigger 函数也是条件分支的触发判定依据。{@link FlowSession#matchNextNodes()} 将会调用 {@link IFlowNode#filterBranches(List, FlowSession)} 匹配过滤条件
     * @param session 会话
     * @return true: 继续执行下一个节点
     */
    boolean continueTrigger(FlowSession session);

    /**
     * 节点验证会话
     * 流程执行continueTrigger之前需要先对判断请求会话的参数是否满足节点参数要求
     */
    void verifySession(FlowSession session);

    /**
     * 构建当前节点下的流程记录，不需要创建记录的返回 空集合
     * @param session 会话
     * @return 流程记录
     */
    List<FlowRecord> generateCurrentRecords(FlowSession session);

    /**
     * 获取节点操作对象管理器
     * @return 节点操作对象管理器
     */
    ActionManager actionManager();

    /**
     * 节点是否完成
     * 当前节点是否完成，由于IFlowAction无法判断节点是否完成，是否完成需要根据节点配置的多人审批规则来判定，因此在提交通过节点时
     * {@link com.codingapi.flow.action.PassAction#run(FlowSession)} 函数中会判断当前节点是否完成
     * 如果完成则将执行当前节点的生成流程记录函数 {@link  IFlowNode#continueTrigger(FlowSession)}
     * @param session 会话
     * @return true: 节点完成
     */
    boolean isDone(FlowSession session);

    /**
     * 填充流程记录，在保存流程记录时将会触发当前节点的填充流程记录函数。由于不同节点存储的流程数据会存在差异。
     * @param session 会话
     * @param flowRecord 流程记录
     */
    void fillNewRecord(FlowSession session,FlowRecord flowRecord);


    /**
     * 过滤条件分支
     * @param nodeList 当前节点下的所有条件
     * @param flowSession 当前会话
     * @return 匹配的节点
     */
    List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession);

}
