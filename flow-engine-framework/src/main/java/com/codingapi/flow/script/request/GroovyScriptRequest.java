package com.codingapi.flow.script.request;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.ScriptField;
import com.codingapi.springboot.script.annotation.ScriptFunction;
import com.codingapi.springboot.script.annotation.ScriptType;

import java.util.List;
import java.util.Map;

/**
 * 流程groovy脚本请求对象 request
 * def run(request){
 * request.getOperatorName()
 * }
 */
@ScriptType(description = "流程脚本请求对象")
public class GroovyScriptRequest {

    /**
     * 流程标题
     */
    private String workflowTitle;

    /**
     * 流程Id
     */
    private String workflowId;

    /**
     * 流程编码
     */
    private String workflowCode;

    /**
     * 当前节点名称
     */
    private String nodeName;

    /**
     * 当前节点类型
     */
    private String nodeType;

    /**
     * 表单字段值
     */
    private Map<String, Object> formData;

    /**
     * 流程创建人
     */
    private IFlowOperator createdOperator;

    /**
     * 当前审批人
     */
    private IFlowOperator currentOperator;

    /**
     * 流程审批人
     */
    private IFlowOperator submitOperator;


    private final FlowSession flowSession;

    /**
     * 从FlowSession构建请求对象（模板方法模式）
     *
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
            this.workflowId = session.getWorkflow().getId();
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

    @ScriptFunction(name = "getWorkflowTitle",description = "获取流程名称")
    public String getWorkflowTitle() {
        return workflowTitle;
    }

    @ScriptFunction(name = "getWorkflowId",description = "获取流程workId")
    public String getWorkflowId() {
        return workflowId;
    }

    @ScriptFunction(name = "getWorkflowCode",description = "获取流程workCode")
    public String getWorkflowCode() {
        return workflowCode;
    }

    @ScriptFunction(name = "getNodeName",description = "获取节点名称")
    public String getNodeName() {
        return nodeName;
    }

    @ScriptFunction(name = "getNodeType",description = "获取节点类型")
    public String getNodeType() {
        return nodeType;
    }

    @ScriptFunction(name = "getFormData",description = "获取当前流程数据")
    public Map<String, Object> getFormData() {
        return formData;
    }

    @ScriptFunction(name = "getCreatedOperator",description = "获取流程创建人信息")
    public IFlowOperator getCreatedOperator() {
        return createdOperator;
    }

    @ScriptFunction(name = "getCurrentOperator",description = "获取当前审批人信息")
    public IFlowOperator getCurrentOperator() {
        return currentOperator;
    }

    @ScriptFunction(name = "getSubmitOperator",description = "获取流程审批者信息")
    public IFlowOperator getSubmitOperator() {
        return submitOperator;
    }


    /**
     * 获取节点信息
     *
     * @param nodeId 节点id
     * @return 节点
     */
    @ScriptFunction(name = "getNode", description = "获取流程节点信息", parameters = {
            @ScriptField(name = "nodeId", description = "流程节点Id")
    })
    public IFlowNode getNode(String nodeId) {
        return flowSession.getNode(nodeId);
    }

    /**
     * 是否流程管理员
     */
    @ScriptFunction(name = "isFlowManager", description = "当前用户是否为流程管理员")
    public boolean isFlowManager() {
        return this.currentOperator.isFlowManager();
    }

    /**
     * 获取当前节点对象
     */
    @ScriptFunction(name = "getCurrentNode", description = "获取当前节点对象")
    public IFlowNode getCurrentNode() {
        return this.flowSession.getCurrentNode();
    }

    /**
     * 获取当前操作对象
     */
    @ScriptFunction(name = "getCurrentAction", description = "获取当前操作对象")
    public IFlowAction getCurrentAction() {
        return this.flowSession.getCurrentAction();
    }

    /**
     * 是否模拟测试
     */
    @ScriptFunction(name = "isMock", description = "是否模拟测试")
    public boolean isMock() {
        return this.flowSession.isMock();
    }

    /**
     * 流程创建者Id
     */
    @ScriptFunction(name = "getCreatedOperatorId", description = "获取流程创建者Id")
    public long getCreatedOperatorId() {
        return this.createdOperator.getUserId();
    }

    /**
     * 流程创建者名称
     */
    @ScriptFunction(name = "getCreatedOperatorName", description = "获取流程创建者名称")
    public String getCreatedOperatorName() {
        return this.createdOperator.getName();
    }


    /**
     * 流程审批者Id
     */
    @ScriptFunction(name = "getCurrentOperatorId", description = "获取流程审批者Id")
    public long getCurrentOperatorId() {
        return this.currentOperator.getUserId();
    }

    /**
     * 流程审批者名称
     */
    @ScriptFunction(name = "getCurrentOperatorName", description = "获取流程审批者名称")
    public String getCurrentOperatorName() {
        return this.currentOperator.getName();
    }

    /**
     * 流程审批者Id
     */
    @ScriptFunction(name = "getSubmitOperatorId", description = "获取流程审批者Id")
    public long getSubmitOperatorId() {
        return this.submitOperator.getUserId();
    }

    /**
     * 流程审批者名称
     */
    @ScriptFunction(name = "getSubmitOperatorName", description = "流程审批者名称")
    public String getSubmitOperatorName() {
        return this.submitOperator.getName();
    }


    /**
     * 获取开始节点
     *
     * @return 开始节点
     */
    @ScriptFunction(name = "getStartNode", description = "获取开始节点")
    public IFlowNode getStartNode() {
        return flowSession.getStartNode();
    }


    /**
     * 转换为当前流程的请求对象
     *
     * @return 流程请求对象
     */
    @ScriptFunction(name = "toCreateRequest", description = "转换为当前流程的请求对象")
    public FlowCreateRequest toCreateRequest() {
        return flowSession.toCreateRequest();
    }

    /**
     * 创建流程请求，用于自流程的创建
     *
     * @param workId   流程设计id
     * @param actionId 动作类型
     * @param formData 流程数据
     */
    @ScriptFunction(
            name = "toCreateRequest",
            description = "创建流程请求，用于自流程的创建",
            parameters = {
                    @ScriptField(name = "workId",description = "流程workId"),
                    @ScriptField(name = "operatorId",description = "流程发起人id"),
                    @ScriptField(name = "actionId",description = "流程动作actionId"),
                    @ScriptField(name = "formData",description = "流程数据formData（JSON格式）"),
            }
    )
    public FlowCreateRequest toCreateRequest(String workId,
                                             long operatorId,
                                             String actionId,
                                             String formData) {
        return flowSession.toCreateRequest(workId, operatorId, actionId, formData);
    }

    /**
     * 创建流程请求，用于自流程的创建
     *
     * @param workId   流程设计id
     * @param actionId 动作类型
     * @param formData 流程数据
     */
    @ScriptFunction(
            name = "toCreateRequest",
            description = "创建流程请求，用于自流程的创建",
            parameters = {
                    @ScriptField(name = "workId",description = "流程workId"),
                    @ScriptField(name = "operatorId",description = "流程发起人id"),
                    @ScriptField(name = "actionId",description = "流程动作actionId"),
                    @ScriptField(name = "formData",description = "流程数据formData（Map<String,Object>格式）"),
            }
    )
    public FlowCreateRequest toCreateRequest(String workId,
                                             long operatorId,
                                             String actionId,
                                             Map<String, Object> formData) {
        return flowSession.toCreateRequest(workId, operatorId, actionId, formData);
    }


    /**
     * 获取表单字段值（Groovy脚本调用）
     *
     * @param fieldCode 字段Code
     * @return 字段值
     */
    @ScriptFunction(
            name = "getFormData",
            description = "获取表单字段值",
            parameters = {
                    @ScriptField(name = "fieldCode",description = "表单字段编码"),
            }
    )
    public Object getFormData(String fieldCode) {
        return flowSession.getFormData(fieldCode);
    }

    /**
     * 获取子表单的数据
     *
     * @param subFormCode 子表单code
     * @return 子表单数据列表
     */
    @ScriptFunction(
            name = "getSubFormData",
            description = "获取子表单的数据，返回list格式数据",
            parameters = {
                    @ScriptField(name = "subFormCode",description = "字表编码"),
            }
    )
    public List<Map<String, Object>> getSubFormData(String subFormCode) {
        return flowSession.getFormData().getSubDataBody(subFormCode)
                .stream()
                .map(FormData.DataBody::toMapData)
                .toList();
    }
}
