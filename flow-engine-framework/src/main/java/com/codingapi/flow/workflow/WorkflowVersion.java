package com.codingapi.flow.workflow;

import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.node.OperatorMatchScript;
import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 流程版本
 */
@Getter
@AllArgsConstructor
public class WorkflowVersion {

    /**
     * 版本id
     */
    @Setter
    private long id;

    /**
     * 版本名称
     */
    @Setter
    private String versionName;

    /**
     * 当前版本
     */
    private boolean current;

    /**
     * 流程id
     */
    private String workId;

    /**
     * 流程编号
     */
    private String code;
    /**
     * 流程名称
     */
    private String title;

    /**
     * 流程描述
     */
    private String description;


    /**
     * 创建者
     */
    private IFlowOperator createdOperator;

    /**
     * 创建时间
     */
    private long createdTime;
    /**
     * 更新时间
     */
    private long updatedTime;

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
     * 流程策略
     */
    private List<IWorkflowStrategy> strategies;

    /**
     * 启用状态
     */
    private boolean enable;

    /**
     * 启用版本
     */
    public void enableVersion() {
        this.current = true;
    }

    /**
     * 禁用版本
     */
    public void disableVersion() {
        this.current = false;
    }


    public WorkflowVersion(Workflow workflow) {
        this.workId = workflow.getId();
        this.code = workflow.getCode();
        this.title = workflow.getTitle();
        this.description = workflow.getDescription();
        this.createdOperator = workflow.getCreatedOperator();
        this.createdTime = workflow.getCreatedTime();
        this.updatedTime = workflow.getUpdatedTime();
        this.form = workflow.getForm();
        this.operatorCreateScript = workflow.getOperatorCreateScript();
        this.nodes = workflow.getNodes();
        this.strategies = workflow.getStrategies();
        this.enable = workflow.isEnable();
        this.current = true;
    }


    public Workflow toWorkflow() {
        return new Workflow(this.workId,
                this.code,
                this.title,
                this.description,
                this.createdOperator,
                this.createdTime,
                this.updatedTime,
                this.form,
                this.operatorCreateScript,
                this.nodes,
                this.strategies,
                this.enable
        );
    }
}
