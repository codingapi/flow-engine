package com.codingapi.flow.workflow;

import com.codingapi.flow.edge.IFlowEdge;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.script.OperatorMatchScript;
import com.codingapi.flow.user.IFlowOperator;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 流程对象
 */
@Setter
@Getter
public class Workflow {

    /**
     * 流程id
     */
    private String id;
    /**
     * 流程编号
     */
    private String code;
    /**
     * 流程名称
     */
    private String title;

    /**
     * 创建者
     */
    private IFlowOperator createdOperator;

    /**
     * 创建时间
     */
    private long createdTime;

    /**
     * 流程表单
     */
    private FlowForm form;

    /**
     * 创建者脚本
     */
    private OperatorMatchScript operatorCreateScript;

    /**
     * 流程节点
     */
    private List<IFlowNode> nodes;

    /**
     * 流程关系
     */
    private List<IFlowEdge> edges;


    /**
     * 匹配创建者
     * @param flowOperator 创建者
     * @return 是否匹配
     */
    public boolean matchCreatedOperator(IFlowOperator flowOperator) {
        return operatorCreateScript.match(flowOperator);
    }

}
