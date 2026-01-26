package com.codingapi.flow.infra.entity;

import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.infra.entity.convert.*;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.node.OperatorMatchScript;
import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Data
@Entity
@Table(name = "t_flow_workflow")
@DynamicUpdate
public class WorkflowEntity {

    /**
     * 流程id
     */
    @Id
    private String id;
    /**
     * 流程编号
     */
    @Column(unique = true)
    private String code;
    /**
     * 流程名称
     */
    private String title;

    /**
     * 创建者
     */
    @Convert(converter = FlowOperatorConvertor.class)
    private IFlowOperator createdOperator;

    /**
     * 创建时间
     */
    private Long createdTime;

    /**
     * 流程表单
     */
    @Lob
    @Convert(converter = FormMetaConvertor.class)
    private FormMeta form;

    /**
     * 创建者脚本
     */
    @Lob
    @Convert(converter = OperatorMatchScriptConvertor.class)
    private OperatorMatchScript operatorCreateScript;

    /**
     * 流程节点
     */
    @Lob
    @Convert(converter = FlowNodeListConvertor.class)
    private List<IFlowNode> nodes;

    /**
     * 流程关系
     */
    @Lob
    @Convert(converter = FlowEdgeListConvertor.class)
    private List<FlowEdge> edges;

    /**
     * 流程设计
     */
    @Lob
    private String schema;

    /**
     * 流程策略
     */
    @Lob
    @Convert(converter = WorkflowStrategyListConvertor.class)
    private List<IWorkflowStrategy> strategies;
}
