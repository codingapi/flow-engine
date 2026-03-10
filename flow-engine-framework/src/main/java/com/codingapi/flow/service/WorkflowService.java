package com.codingapi.flow.service;

import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.repository.WorkflowRuntimeRepository;
import com.codingapi.flow.repository.WorkflowVersionRepository;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowVersion;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class WorkflowService {

    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowRuntimeRepository workflowRuntimeRepository;


    public void saveWorkflowVersion(WorkflowVersion workflowVersion) {
        List<WorkflowVersion> updateList = new ArrayList<>();

        workflowVersion.enableVersion();
        updateList.add(workflowVersion);

        List<WorkflowVersion> versionList = workflowVersionRepository.findVersion(workflowVersion.getWorkId());
        if (versionList != null) {
            versionList.stream().filter(WorkflowVersion::isCurrent).findFirst().ifPresent(current -> workflowVersion.setId(current.getId()));

            for (WorkflowVersion version : versionList) {
                if (version.getId() != workflowVersion.getId()) {
                    version.disableVersion();
                    updateList.add(version);
                }
            }
        }

        workflowVersionRepository.saveAll(updateList);
        Workflow workflow = workflowVersion.toWorkflow();
        workflowRepository.save(workflow);
    }


    public WorkflowRuntime getWorkflowRuntime(long id) {
        return workflowRuntimeRepository.get(id);
    }

    public Workflow getWorkflow(String workId) {
        return workflowRepository.get(workId);
    }


    public void changeVersion(long versionId) {
        WorkflowVersion workflowVersion = workflowVersionRepository.get(versionId);
        if (workflowVersion != null) {
            this.saveWorkflowVersion(workflowVersion);
        }
    }

    public void updateVersionName(long versionId, String versionName) {
        WorkflowVersion workflowVersion = workflowVersionRepository.get(versionId);
        if (workflowVersion != null) {
            workflowVersion.setVersionName(versionName);
            workflowVersionRepository.save(workflowVersion);
        }
    }

    public void delete(String workId) {
        workflowVersionRepository.delete(workId);
        workflowRepository.delete(workId);
    }

    public void saveWorkflow(Workflow workflow) {
        WorkflowVersion workflowVersion = new WorkflowVersion(workflow);
        this.saveWorkflowVersion(workflowVersion);
    }

    public void saveWorkflowRuntime(WorkflowRuntime workflowRuntime) {
        this.workflowRuntimeRepository.save(workflowRuntime);
    }

    public WorkflowRuntime getWorkflowRuntime(String workId, long updatedTime) {
        return this.workflowRuntimeRepository.getByWorkId(workId, updatedTime);
    }
}
