package com.codingapi.flow.script.request;

import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Groovy脚本请求对象抽象基类
 * 从FlowSession中提取数据供脚本使用
 */
@Getter
@Setter
public abstract class BaseGroovyRequest {

    /**
     * 当前操作人姓名
     */
    protected String operatorName;

    /**
     * 当前操作人ID
     */
    protected Integer operatorId;

    /**
     * 是否流程管理员
     */
    protected Boolean isFlowManager;

    /**
     * 流程标题
     */
    protected String workflowTitle;

    /**
     * 流程编码
     */
    protected String workflowCode;

    /**
     * 流程编号
     */
    protected String workCode;

    /**
     * 流程创建人姓名
     */
    protected String creatorName;

    /**
     * 表单字段值
     */
    protected Map<String, Object> formData;

    /**
     * 从FlowSession构建请求对象（模板方法模式）
     * @param session 流程会话（不能为null）
     */
    public BaseGroovyRequest(FlowSession session) {
        // 提取操作人信息
        if (session.getCurrentOperator() != null) {
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

        // 提取表单数据
        if (session.getFormData() != null) {
            this.formData = session.getFormData().toMapData();
        }
    }

    /**
     * 获取表单字段值（Groovy脚本调用）
     * @param key 字段key
     * @return 字段值
     */
    public Object getFormData(String key) {
        if (formData == null) {
            return null;
        }
        return formData.get(key);
    }
}
