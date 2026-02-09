package com.codingapi.flow.manager;

import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.FormMetaBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RouterStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowNodeEdgeManagerTest {

    @Test
    void getNextNodes() {

        User user = new User(1, "user");


        FormMeta form = FormMetaBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", "string")
                .addField("请假天数", "days", "int")
                .addField("请假事由", "reason", "string")
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();


        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(3)]}"))
                        .build()
                )
                .build();

        RouterNode routerNode = RouterNode.builder()
                .name("路由节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new RouterStrategy(String.format("def run(request){return '%s'}", bossNode.getId())))
                        .build())
                .build();

        ApprovalNode departNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [$bind.getOperatorById(2)]}"))
                        .build()
                )
                .build();


        ConditionBranchNode departConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .blocks(departNode, routerNode)
                .build();

        ConditionBranchNode bossConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') > 3}")
                .order(2)
                .blocks(bossNode)
                .build();

        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制节点")
                .blocks(departConditionNode, bossConditionNode)
                .build();


        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(conditionNode)
                .addNode(endNode)
                .build();


        // 测试departNode获取routerNode节点的情况
        List<IFlowNode> nextNodes = workflow.nextNodes(departNode);
        assertEquals(1, nextNodes.size());
        IFlowNode nextNode = nextNodes.get(0);
        assertEquals(nextNode.getId(),routerNode.getId());


        // 测试conditionNode获取branch的节点情况
        nextNodes = workflow.nextNodes(conditionNode);
        assertEquals(2, nextNodes.size());
        nextNode = nextNodes.get(0);
        assertEquals(nextNode.getId(),departConditionNode.getId());


        // 测试routerNode获取end的节点情况
        nextNodes = workflow.nextNodes(routerNode);
        assertEquals(1, nextNodes.size());
        nextNode = nextNodes.get(0);
        assertEquals(nextNode.getId(),endNode.getId());

        // 测试routerNode获取end的节点情况
        nextNodes = workflow.nextNodes(bossNode);
        assertEquals(1, nextNodes.size());
        nextNode = nextNodes.get(0);
        assertEquals(nextNode.getId(),endNode.getId());
    }
}