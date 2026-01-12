package com.codingapi.flow.workflow;

import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.node.*;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowBuilderTest {

    @Test
    void buildBasicWorkflow() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
                .build();

        StartNode startNode = new StartNode();
        EndNode endNode = new EndNode();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals("请假流程", workflow.getTitle());
        assertEquals("leave", workflow.getCode());
        assertNotNull(workflow.getCreatedOperator());
        assertEquals(1, workflow.getCreatedOperator().getUserId());
        assertNotNull(workflow.getForm());
        assertEquals("请假流程", workflow.getForm().getName());
        assertNotNull(workflow.getNodes());
        assertEquals(2, workflow.getNodes().size());
    }

    @Test
    void buildWorkflowWithApprovalNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("审批流程")
                .code("approval")
                .addField("申请内容", "content", "string")
                .build();

        StartNode startNode = new StartNode();
        ApprovalNode approvalNode = new ApprovalNode("主管审批");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("审批流程")
                .code("approval")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(3, workflow.getNodes().size());
        assertEquals(ApprovalNode.NODE_TYPE, approvalNode.getType());
    }

    @Test
    void buildWorkflowWithHandleNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("办理流程")
                .code("handle")
                .build();

        StartNode startNode = new StartNode();

        HandleNode handleNode = new HandleNode("办理");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("办理流程")
                .code("handle")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(handleNode)
                .addNode(endNode)
                .addEdge(new FlowEdge(startNode.getId(), handleNode.getId()))
                .addEdge(new FlowEdge(handleNode.getId(), endNode.getId()))
                .build();

        assertNotNull(workflow);
        assertEquals(3, workflow.getNodes().size());
        assertEquals(HandleNode.NODE_TYPE, handleNode.getType());
        assertEquals("办理", handleNode.getName());
    }

    @Test
    void buildWorkflowWithConditionBranchNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("条件分支流程")
                .code("condition_branch")
                .addField("金额", "amount", "int")
                .build();

        StartNode startNode = new StartNode();

        ConditionBranchNode conditionNode = new ConditionBranchNode("金额判断");
        ApprovalNode approvalNode1 = new ApprovalNode("经理审批");
        ApprovalNode approvalNode2 = new ApprovalNode("总监审批");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("条件分支流程")
                .code("condition_branch")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(conditionNode)
                .addNode(approvalNode1)
                .addNode(approvalNode2)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(5, workflow.getNodes().size());
        assertEquals(ConditionBranchNode.NODE_TYPE, conditionNode.getType());
    }

    @Test
    void buildWorkflowWithParallelBranchNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("并行分支流程")
                .code("parallel_branch")
                .build();

        StartNode startNode = new StartNode();

        ParallelBranchNode parallelNode = new ParallelBranchNode("并行审批");

        ApprovalNode approvalNode1 = new ApprovalNode("财务审批");

        ApprovalNode approvalNode2 = new ApprovalNode("法务审批");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("并行分支流程")
                .code("parallel_branch")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(parallelNode)
                .addNode(approvalNode1)
                .addNode(approvalNode2)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(5, workflow.getNodes().size());
        assertEquals(ParallelBranchNode.NODE_TYPE, parallelNode.getType());
    }

    @Test
    void buildWorkflowWithNotifyNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("通知流程")
                .code("notify")
                .build();

        StartNode startNode = new StartNode();

        NotifyNode notifyNode = new NotifyNode("通知相关人员");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("通知流程")
                .code("notify")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(notifyNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(3, workflow.getNodes().size());
        assertEquals(NotifyNode.NODE_TYPE, notifyNode.getType());
    }

    @Test
    void buildWorkflowWithDelayNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("延迟流程")
                .code("delay")
                .build();

        StartNode startNode = new StartNode();

        DelayNode delayNode = new DelayNode("延迟处理");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("延迟流程")
                .code("delay")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(delayNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(3, workflow.getNodes().size());
        assertEquals(DelayNode.NODE_TYPE, delayNode.getType());
    }

    @Test
    void buildWorkflowWithSubProcessNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("子流程流程")
                .code("subprocess")
                .build();

        StartNode startNode = new StartNode();

        SubProcessNode subProcessNode = new SubProcessNode("调用子流程");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("子流程流程")
                .code("subprocess")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(subProcessNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(3, workflow.getNodes().size());
        assertEquals(SubProcessNode.NODE_TYPE, subProcessNode.getType());
    }

    @Test
    void buildWorkflowWithTriggerNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("触发器流程")
                .code("trigger")
                .build();

        StartNode startNode = new StartNode();

        TriggerNode triggerNode = new TriggerNode("触发器执行");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("触发器流程")
                .code("trigger")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(triggerNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(3, workflow.getNodes().size());
        assertEquals(TriggerNode.NODE_TYPE, triggerNode.getType());
    }

    @Test
    void buildWorkflowWithRouterBranchNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("路由分支流程")
                .code("router_branch")
                .build();

        StartNode startNode = new StartNode();

        RouterBranchNode routerNode = new RouterBranchNode("路由选择");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("路由分支流程")
                .code("router_branch")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(routerNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(3, workflow.getNodes().size());
        assertEquals(RouterBranchNode.NODE_TYPE, routerNode.getType());
    }

    @Test
    void buildWorkflowWithInclusiveBranchNode() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("包容分支流程")
                .code("inclusive_branch")
                .build();

        StartNode startNode = new StartNode();

        InclusiveBranchNode inclusiveNode = new InclusiveBranchNode("包容分支");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("包容分支流程")
                .code("inclusive_branch")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(inclusiveNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals(3, workflow.getNodes().size());
        assertEquals(InclusiveBranchNode.NODE_TYPE, inclusiveNode.getType());
    }

    @Test
    void buildComplexWorkflow() {
        User user = new User(1, "lorne");

        FlowForm form = FlowFormBuilder.builder()
                .name("复杂流程")
                .code("complex")
                .addField("申请人", "applicant", "string")
                .addField("申请金额", "amount", "int")
                .addField("申请理由", "reason", "string")
                .build();

        StartNode startNode = new StartNode();

        NotifyNode notifyNode = new NotifyNode("通知发起人");

        ConditionBranchNode conditionNode = new ConditionBranchNode("金额判断");

        ApprovalNode approvalNode1 = new ApprovalNode("部门经理审批");

        ApprovalNode approvalNode2 = new ApprovalNode("总经理审批");

        ParallelBranchNode parallelNode = new ParallelBranchNode("并行审批");

        ApprovalNode financeApproval = new ApprovalNode("财务审批");

        ApprovalNode legalApproval = new ApprovalNode("法务审批");

        DelayNode delayNode = new DelayNode("延迟通知");

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("复杂流程")
                .code("complex")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(notifyNode)
                .addNode(conditionNode)
                .addNode(approvalNode1)
                .addNode(approvalNode2)
                .addNode(parallelNode)
                .addNode(financeApproval)
                .addNode(legalApproval)
                .addNode(delayNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertEquals("复杂流程", workflow.getTitle());
        assertEquals("complex", workflow.getCode());
        assertNotNull(workflow.getNodes());
        assertEquals(10, workflow.getNodes().size());
        
        boolean hasAllNodeTypes = workflow.getNodes().stream()
                .anyMatch(node -> node.getType().equals(StartNode.NODE_TYPE));
        assertTrue(hasAllNodeTypes);
        
        boolean hasApprovalNode = workflow.getNodes().stream()
                .anyMatch(node -> node.getType().equals(ApprovalNode.NODE_TYPE));
        assertTrue(hasApprovalNode);
    }

    @Test
    void buildWorkflowWithSubForm() {
        User user = new User(1, "lorne");

        FlowForm subForm = FlowFormBuilder.builder()
                .name("报销明细")
                .code("expense_detail")
                .addField("明细项", "item", "string")
                .addField("金额", "amount", "int")
                .build();

        FlowForm form = FlowFormBuilder.builder()
                .name("报销流程")
                .code("expense")
                .addField("报销人", "applicant", "string")
                .addField("报销总额", "total", "int")
                .addSubForm(subForm)
                .build();

        StartNode startNode = new StartNode();

        ApprovalNode approvalNode = new ApprovalNode();

        EndNode endNode = new EndNode();

        Workflow workflow = WorkflowBuilder.builder()
                .title("报销流程")
                .code("expense")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(endNode)
                .build();

        assertNotNull(workflow);
        assertNotNull(workflow.getForm());
        assertNotNull(workflow.getForm().getSubForms());
        assertEquals(1, workflow.getForm().getSubForms().size());
        assertEquals("报销明细", workflow.getForm().getSubForms().get(0).getName());
    }
}
