package com.codingapi.flow.api.pojo;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.workflow.Workflow;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowMeta {

    private String workId;
    private String workCode;
    private List<ActionOption> actions;
    private FlowForm form;

    public WorkflowMeta(Workflow workflow) {
        this.workId = workflow.getId();
        this.form = workflow.getForm();
        this.workCode = workflow.getCode();
        IFlowNode startNode = workflow.getStartNode();
        ActionManager actionManager = startNode.actionManager();
        // 子流程配置的触发动作只返回启用的动作，禁用的动作不作为候选
        this.actions = actionManager.getActions().stream()
                .filter(IFlowAction::enable)
                .map(ActionOption::new)
                .toList();
    }


    @Data
    public static class ActionOption {
        private String actionId;
        private String title;
        private String type;

        public ActionOption(IFlowAction action) {
            this.actionId = action.id();
            this.title = action.title();
            this.type = action.type();
        }
    }
}
