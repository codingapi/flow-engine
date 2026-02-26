package com.codingapi.flow.script.runtime;

import lombok.Data;
import java.util.Map;

/**
 * 标题表达式Groovy脚本请求对象
 * 提供给NodeTitleScript使用的上下文数据
 */
@Data
public class TitleGroovyRequest {

    // ========== 操作人信息 ==========
    /**
     * 当前操作人姓名
     */
    private String operatorName;

    /**
     * 当前操作人ID
     */
    private Integer operatorId;

    /**
     * 是否流程管理员
     */
    private Boolean isFlowManager;

    // ========== 流程信息 ==========
    /**
     * 流程标题
     */
    private String workflowTitle;

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

    // ========== 创建人信息 ==========
    /**
     * 流程创建人姓名
     */
    private String creatorName;

    // ========== 表单数据 ==========
    /**
     * 表单字段值
     */
    private Map<String, Object> formData;

    // ========== 流程编号 ==========
    /**
     * 流程编号
     */
    private String workCode;

    /**
     * 获取表单字段值（Groovy脚本调用）
     */
    public Object getFormData(String key) {
        if (formData == null) {
            return null;
        }
        return formData.get(key);
    }
}
