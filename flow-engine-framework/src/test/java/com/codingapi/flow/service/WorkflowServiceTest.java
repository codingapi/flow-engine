package com.codingapi.flow.service;

import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FormField;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.repository.WorkflowRuntimeRepository;
import com.codingapi.flow.repository.WorkflowVersionRepository;
import com.codingapi.flow.repository.WorkflowRepositoryImpl;
import com.codingapi.flow.repository.WorkflowRuntimeRepositoryImpl;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.flow.workflow.WorkflowVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowServiceTest {

    @Test
    void should_align_historical_field_permissions_before_saving_version() {
        SnapshotWorkflowVersionRepository versionRepository = new SnapshotWorkflowVersionRepository();
        WorkflowRepository workflowRepository = new WorkflowRepositoryImpl();
        WorkflowRuntimeRepository runtimeRepository = new WorkflowRuntimeRepositoryImpl();
        WorkflowService service = new WorkflowService(versionRepository, workflowRepository, runtimeRepository);

        ApprovalNode approvalNode = ApprovalNode.defaultNode();
        FormFieldPermission legacyPermission = new FormFieldPermission();
        legacyPermission.setFormCode("leave");
        legacyPermission.setFieldCode("desc");
        legacyPermission.setType(PermissionType.READ);
        approvalNode.strategyManager()
                .getStrategy(FormFieldPermissionStrategy.class)
                .setFieldPermissions(List.of(legacyPermission));

        Workflow workflow = WorkflowBuilder.builder()
                .id("workflow-1")
                .code("leave-workflow")
                .title("请假流程")
                .form(formWithDescriptionField())
                .addNode(approvalNode)
                .build(false);

        service.saveWorkflow(workflow);

        FormFieldPermission permission = versionRepository.getSavedPermissions()
                .get(0);
        assertEquals("description", permission.getFieldCode());
        assertEquals(PermissionType.WRITE, permission.getType());
    }

    private FlowForm formWithDescriptionField() {
        FormField field = new FormField();
        field.setCode("description");
        field.setName("请假说明");
        field.setType("input");
        field.setDataType(DataType.STRING);

        FlowForm form = new FlowForm();
        form.setCode("leave");
        form.setName("请假单");
        form.setFields(List.of(field));
        form.setSubForms(List.of());
        return form;
    }

    private static class SnapshotWorkflowVersionRepository implements WorkflowVersionRepository {

        private List<FormFieldPermission> savedPermissions = List.of();

        private List<FormFieldPermission> getSavedPermissions() {
            return savedPermissions;
        }

        @Override
        public WorkflowVersion get(long id) {
            return null;
        }

        @Override
        public void delete(String workId) {
        }

        @Override
        public List<WorkflowVersion> findVersion(String workId) {
            return List.of();
        }

        @Override
        public void saveAll(List<WorkflowVersion> versionList) {
            versionList.forEach(this::save);
        }

        @Override
        public void save(WorkflowVersion workflowVersion) {
            savedPermissions = workflowVersion.toWorkflow()
                    .getNodes()
                    .get(0)
                    .strategyManager()
                    .getStrategy(FormFieldPermissionStrategy.class)
                    .getFieldPermissions()
                    .stream()
                    .map(this::copyPermission)
                    .toList();
        }

        @Override
        public void delete(long id) {
        }

        private FormFieldPermission copyPermission(FormFieldPermission permission) {
            FormFieldPermission copy = new FormFieldPermission();
            copy.setFormCode(permission.getFormCode());
            copy.setFieldCode(permission.getFieldCode());
            copy.setType(permission.getType());
            return copy;
        }
    }
}
