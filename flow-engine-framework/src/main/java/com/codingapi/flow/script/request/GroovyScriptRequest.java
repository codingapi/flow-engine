package com.codingapi.flow.script.request;

import com.codingapi.flow.form.FormData;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 *  流程groovy脚本请求对象 request
 *  def run(request){
 *      request.getOperatorName()
 *  }
 */
public class GroovyScriptRequest {

    /**
     * 流程标题
     */
    @Getter
    private String workflowTitle;

    /**
     * 流程编码
     */
    @Getter
    private String workflowCode;

    /**
     * 当前节点名称
     */
    @Getter
    private String nodeName;

    /**
     * 当前节点类型
     */
    @Getter
    private String nodeType;

    /**
     * 表单字段值
     */
    @Getter
    private Map<String, Object> formData;


    /**
     * 流程创建人
     */
    @Getter
    private IFlowOperator createdOperator;

    /**
     * 当前审批人
     */
    @Getter
    private IFlowOperator currentOperator;

    /**
     * 流程提交Id
     */
    @Getter
    private IFlowOperator submitOperator;

    /**
     * 流程提交人姓名
     */
    @Getter
    private String submitOperatorName;


    private final FlowSession flowSession;

    /**
     * 从FlowSession构建请求对象（模板方法模式）
     * @param session 流程会话（不能为null）
     */
    public GroovyScriptRequest(FlowSession session) {
        this.flowSession = session;
        // 提取操作人信息
        if (session.getCurrentOperator() != null) {
            this.currentOperator = session.getCurrentOperator();
        }

        // 提取创建人信息
        if (session.getCreatedOperator() != null) {
            this.createdOperator = session.getCreatedOperator();
        }

        // 提取提交人信息
        if (session.getSubmitOperator() != null) {
            this.submitOperator = session.getSubmitOperator();
        }

        // 提取流程信息
        if (session.getWorkflow() != null) {
            this.workflowTitle = session.getWorkflow().getTitle();
            this.workflowCode = session.getWorkflow().getCode();

        }

        // 提取节点信息
        if (session.getCurrentNode() != null) {
            this.nodeName = session.getCurrentNode().getName();
            this.nodeType = session.getCurrentNode().getType();
        }

        // 提取表单数据
        if (session.getFormData() != null) {
            this.formData = session.getFormData().toMapData();
        }

    }

    /**
     * 获取节点信息
     * @param nodeId 节点id
     * @return 节点
     */
    public IFlowNode getNode(String nodeId){
        return flowSession.getNode(nodeId);
    }

    /**
     * 是否流程管理员
     */
    public boolean isFlowManager(){
        return this.currentOperator.isFlowManager();
    }

    /**
     * 是否模拟测试
     */
    public boolean isMock(){
        return this.flowSession.isMock();
    }


    /**
     * 流程创建者Id
     */
    public long getCreatedOperatorId(){
        return this.createdOperator.getUserId();
    }

    /**
     * 流程创建者名称
     */
    public String getCreatedOperatorName(){
        return this.createdOperator.getName();
    }


    /**
     * 流程审批者Id
     */
    public long getCurrentOperatorId(){
        return this.currentOperator.getUserId();
    }

    /**
     * 流程审批者名称
     */
    public String getCurrentOperatorName(){
        return this.currentOperator.getName();
    }

    /**
     * 流程审批者Id
     */
    public long getSubmitOperatorId(){
        if(this.submitOperator!=null) {
            return this.submitOperator.getUserId();
        }
        return 0;
    }

    /**
     * 流程审批者名称
     */
    public String getSubmitOperatorName(){
        if(this.submitOperator!=null) {
            return this.submitOperator.getName();
        }else {
            return null;
        }
    }



    /**
     * 获取开始节点
     * @return 开始节点
     */
    public IFlowNode getStartNode(){
        return flowSession.getStartNode();
    }


    /**
     * 转换为当前流程的请求对象
     * @return 流程请求对象
     */
    public FlowCreateRequest toCreateRequest(){
        return flowSession.toCreateRequest();
    }

    /**
     * 创建流程请求，用于自流程的创建
     * @param workId 流程设计id
     * @param actionId 动作类型
     * @param formData 流程数据
     */
    public FlowCreateRequest toCreateRequest(String workId,
                                             long operatorId,
                                             String actionId,
                                             String formData){
        return flowSession.toCreateRequest(workId, operatorId, actionId, formData);
    }

    /**
     * 创建流程请求，用于自流程的创建
     * @param workId 流程设计id
     * @param actionId 动作类型
     * @param formData 流程数据
     */
    public FlowCreateRequest toCreateRequest(String workId,
                                             long operatorId,
                                             String actionId,
                                             Map<String,Object> formData){
        return flowSession.toCreateRequest(workId, operatorId, actionId, formData);
    }


    /**
     * 获取表单字段值（Groovy脚本调用）
     * @param fieldCode 字段Code
     * @return 字段值
     */
    public Object getFormData(String fieldCode) {
        return flowSession.getFormData(fieldCode);
    }

    /**
     * 获取子表单的数据
     * @param subFormCode 子表单code
     * @return 子表单数据列表
     */
    public List<Map<String,Object>> getSubFormData(String subFormCode){
        return flowSession.getFormData().getSubDataBody(subFormCode)
                .stream()
                .map(FormData.DataBody::toMapData)
                .toList();
    }
}
