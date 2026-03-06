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
     * 当前操作人姓名
     */
    @Getter
    private String operatorName;

    /**
     * 当前操作人ID
     */
    @Getter
    private long operatorId;

    /**
     * 是否流程管理员
     */
    @Getter
    private Boolean isFlowManager;

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
     * 流程编号
     */
    @Getter
    private String workCode;

    /**
     * 流程创建人姓名
     */
    @Getter
    private String creatorName;

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
     * 当前操作人（上一节点审批人）
     */
    @Getter
    private IFlowOperator currentOperator;


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
            this.operatorName = session.getCurrentOperator().getName();
            this.operatorId = (int) session.getCurrentOperator().getUserId();
            this.isFlowManager = session.getCurrentOperator().isFlowManager();
        }

        // 提取流程信息
        if (session.getWorkflow() != null) {
            this.workflowTitle = session.getWorkflow().getTitle();
            this.workflowCode = session.getWorkflow().getCode();
            if (session.getWorkflow().getCreatedOperator() != null) {
                this.creatorName = session.getWorkflow().getCreatedOperator().getName();
            }
        }

        // 提取节点信息
        if (session.getCurrentNode() != null) {
            this.nodeName = session.getCurrentNode().getName();
            this.nodeType = session.getCurrentNode().getType();
        }

        // 提取流程编号（从record获取）
        if (session.getCurrentRecord() != null) {
            this.workCode = session.getCurrentRecord().getWorkCode();
        }

        // 提取表单数据
        if (session.getFormData() != null) {
            this.formData = session.getFormData().toMapData();
        }

        // 提取创建人信息
        if (session.getWorkflow() != null && session.getWorkflow().getCreatedOperator() != null) {
            this.createdOperator = session.getWorkflow().getCreatedOperator();
        }

    }

    /**
     * 获取节点信息
     * @param nodeId 节点id
     * @return 节点
     */
    public IFlowNode getNode(String nodeId){
        return flowSession.getWorkflow().getFlowNode(nodeId);
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
