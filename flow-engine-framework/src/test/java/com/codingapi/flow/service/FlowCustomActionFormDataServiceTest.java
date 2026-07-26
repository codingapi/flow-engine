package com.codingapi.flow.service;

import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowCustomActionFormDataServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void customActionScriptShouldPersistChangedFormData() {
        User applicant = new User(1, "applicant");
        User approver = new User(2, "approver");
        factory.userGateway.save(applicant);
        factory.userGateway.save(approver);
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("指定审批人流程")
                .code("designated-signer")
                .addField("标题", "title", DataType.STRING)
                .addField("指定审批人", "designated_signer_id", DataType.STRING)
                .build();

        StartNode startNode = StartNode.builder()
                .strategies(writableFormFields())
                .build();
        CustomAction customAction = createCustomPassAction();
        ApprovalNode approvalNode = ApprovalNode.builder()
                .name("审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("designated-signer", "title", PermissionType.WRITE)
                                .addPermission("designated-signer", "designated_signer_id", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy(
                                FlowGroovyScriptFactory.createOperatorLoadScript(
                                        "def run(request){return [2]}"
                                ).getKey()))
                        .build())
                .build();
        approvalNode.actionManager().getActions().add(customAction);

        Workflow workflow = WorkflowBuilder.builder()
                .title("指定审批人流程")
                .code("designated-signer")
                .createdOperator(applicant)
                .form(form)
                .addNode(startNode)
                .addNode(approvalNode)
                .addNode(EndNode.builder().build())
                .build();
        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> formData = new HashMap<>();
        formData.put("title", "测试");
        formData.put("designated_signer_id", "");
        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setFormData(formData);
        createRequest.setActionId(startNode.actionManager().getActionByType("PASS").id());
        createRequest.setOperatorId(applicant.getUserId());
        factory.flowService.create(createRequest);

        FlowRecord startRecord = factory.flowRecordRepository.findTodoByOperator(applicant.getUserId()).get(0);
        FlowActionRequest submitRequest = new FlowActionRequest();
        submitRequest.setFormData(formData);
        submitRequest.setRecordId(startRecord.getId());
        submitRequest.setAdvice(new FlowAdviceBody(
                startNode.actionManager().getActionByType("PASS").id(),
                "提交",
                applicant.getUserId()
        ));
        factory.flowService.action(submitRequest);

        FlowRecord approvalRecord = factory.flowRecordRepository.findTodoByOperator(approver.getUserId()).get(0);
        FlowActionRequest customActionRequest = new FlowActionRequest();
        customActionRequest.setFormData(formData);
        customActionRequest.setRecordId(approvalRecord.getId());
        customActionRequest.setAdvice(new FlowAdviceBody(customAction.id(), "确认", approver.getUserId()));
        factory.flowService.action(customActionRequest);

        assertEquals(
                String.valueOf(approver.getUserId()),
                approvalRecord.getFormData().get("designated_signer_id")
        );
    }

    private CustomAction createCustomPassAction() {
        String script = """
                // @CUSTOM_SCRIPT
                // @SCRIPT_TITLE 指定人通过塞id
                // @SCRIPT_META {"trigger":"PASS"}
                def run(request) {
                    def formData = request.getFormData()
                    formData.put("designated_signer_id", String.valueOf(request.getCurrentOperatorId()))
                    request.resetFormData(formData)
                    return 'PASS'
                }
                """;
        CustomAction defaultAction = CustomAction.defaultAction();
        Map<String, Object> actionData = defaultAction.toMap();
        actionData.put("script", FlowGroovyScriptFactory.createActionCustomScript(script).getKey());
        return CustomAction.fromMap(actionData);
    }

    private List<INodeStrategy> writableFormFields() {
        return NodeStrategyBuilder.builder()
                .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                        .addPermission("designated-signer", "title", PermissionType.WRITE)
                        .addPermission("designated-signer", "designated_signer_id", PermissionType.WRITE)
                        .build()))
                .build();
    }
}
