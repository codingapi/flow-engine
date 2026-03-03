package com.codingapi.flow.script.request;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.Setter;

/**
 * 人员加载Groovy脚本请求对象
 * 提供给OperatorLoadScript使用的上下文数据
 * 继承BaseGroovyRequest，从FlowSession自动提取通用数据
 */
@Getter
@Setter
public class OperatorLoadGroovyRequest extends BaseGroovyRequest {

    /**
     * 流程创建人
     */
    private IFlowOperator createdOperator;

    /**
     * 当前操作人（上一节点审批人）
     */
    private IFlowOperator currentOperator;

    /**
     * 从FlowSession构建OperatorLoadGroovyRequest
     * @param session 流程会话（不能为null）
     */
    public OperatorLoadGroovyRequest(FlowSession session) {
        super(session);
        // 提取创建人信息
        if (session.getWorkflow() != null && session.getWorkflow().getCreatedOperator() != null) {
            this.createdOperator = session.getWorkflow().getCreatedOperator();
        }
        this.currentOperator = session.getCurrentOperator();
    }
}
