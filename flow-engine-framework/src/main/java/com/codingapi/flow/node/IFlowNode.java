package com.codingapi.flow.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;

import java.util.List;
import java.util.function.Consumer;

/**
 * 流程节点 <br/>
 * 流程执行的生命周期，流程在运行之前将会先构建 {@link com.codingapi.flow.action.IFlowAction} {@link IFlowNode} {@link FlowSession} 等对象 <br/>
 * 1. 流程的调用第一步将会执行 {@link com.codingapi.flow.action.IFlowAction#run(FlowSession)} 函数。<br/>
 * 2. 在{@link com.codingapi.flow.action.IFlowAction#run(FlowSession)} 中流程将需要判断当前流程{@link IFlowNode#isDone(FlowSession)} 是否已经办理完成。 <br/>
 * 3. 流程办理完成后将会分析流程对的下一节点对象 {@link com.codingapi.flow.action.BaseAction#triggerNode(FlowSession, Consumer)} ()},将递归掉分析执行下一节点 <br/>
 * 4. 在获取下一节点对象时，将会访问当节点的拦截策略 {@link IFlowNode#filterBranches(List, FlowSession)}，该函数将根据节点的配置进行匹配下一节点。 <br/>
 * 5. 获取到下一节点对象后，则会访问流程节点的 {@link IFlowNode#handle(FlowSession)} 函数分析流程是否继续执行。当函数返回true时则会继续循环调用匹配下一节点的逻辑，即triggerNode的递归逻辑。 <br/>
 * 6. 当{@link IFlowNode#handle(FlowSession)} 返回的是false时，则停止继续下一节点流程，开始执行当前节点的生成流程记录函数 {@link  IFlowNode#generateCurrentRecords(FlowSession)} <br/>
 * 7. 在构建出先的流程记录数据以后，在数据保存时还将会触发节点对流程记录对象的填充函数 {@link  IFlowNode#fillNewRecord(FlowSession, FlowRecord)} <br/>
 */
public interface IFlowNode extends IMapConvertor {

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
     *
     * @return 节点顺序
     */
    int getOrder();

    /**
     * 节点验证
     * 用于流程配置完成以后的验证时触发
     */
    void verifyNode(FormMeta form);

    /**
     * 是否执行节点
     * 当前流程节点执行完成以后，触发下一环节时执行的函数，当返回true时则将继续执行后续节点的handle流程，当返回false时则不继续执行后续流程，将执行当前节点的创建流程记录函数 {@link  IFlowNode#generateCurrentRecords(FlowSession)}
     *
     * @param session 会话
     * @return true: 继续执行下一个节点
     */
    boolean handle(FlowSession session);

    /**
     * 节点验证会话
     * 流程执行continueTrigger之前需要先对判断请求会话的参数是否满足节点参数要求
     */
    void verifySession(FlowSession session);

    /**
     * 构建当前节点下的流程记录，不需要创建记录的返回 空集合
     *
     * @param session 会话
     * @return 流程记录
     */
    List<FlowRecord> generateCurrentRecords(FlowSession session);

    /**
     * 获取节点操作对象管理器
     *
     * @return 节点操作对象管理器
     */
    ActionManager actionManager();

    /**
     * 获取节点策略管理器
     *
     * @return 节点策略管理器
     */
    NodeStrategyManager strategyManager();

    /**
     * 节点是否完成
     *
     * @param session 会话
     * @return true: 节点完成
     */
    boolean isDone(FlowSession session);

    /**
     * 填充流程记录，在保存流程记录时将会触发当前节点的填充流程记录函数。由于不同节点存储的流程数据会存在差异。
     *
     * @param session    会话
     * @param flowRecord 流程记录
     */
    void fillNewRecord(FlowSession session, FlowRecord flowRecord);


    /**
     * 过滤条件分支
     *
     * @param nodeList    当前节点下的所有条件
     * @param flowSession 当前会话
     * @return 匹配的节点
     */
    List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession);

}
