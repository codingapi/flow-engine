package com.codingapi.flow.session;

import com.codingapi.flow.form.FormData;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.user.IFlowOperator;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程会话对象
 */
@Getter
@AllArgsConstructor
public class FlowSession {

    /**
     * 当前操作者
     */
    private final IFlowOperator currentOperator;
    /**
     * 当前流程表单
     */
    private final FormMeta formMeta;
    /**
     * 当前流程设计
     */
    private final Workflow workflow;
    /**
     * 当前流程节点
     */
    private final IFlowNode currentNode;

    /**
     * 当前流程表单数据
     */
    private final FormData formData;


    /**
     * 获取流程的创建者
     */
    public IFlowOperator getCreatedOperator() {
        return workflow.getCreatedOperator();
    }

}
