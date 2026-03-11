package com.codingapi.flow.service;

import com.codingapi.flow.exception.FlowExecutionException;
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


    public void saveWorkflowVersion(WorkflowVersion currentVersion, boolean creatable) {
        List<WorkflowVersion> updateList = new ArrayList<>();

        currentVersion.enableVersion();
        updateList.add(currentVersion);

        List<WorkflowVersion> versionList = workflowVersionRepository.findVersion(currentVersion.getWorkId());
        if (versionList != null) {

            if (!creatable) {
                versionList.stream().filter(WorkflowVersion::isCurrent).findFirst().ifPresent(current -> {
                    currentVersion.setId(current.getId());
                    currentVersion.setVersionName(current.getVersionName());
                });
            }

            for (WorkflowVersion version : versionList) {
                if (version.getId() != currentVersion.getId()) {
                    version.disableVersion();
                    updateList.add(version);
                }
            }
        }

        workflowVersionRepository.saveAll(updateList);
        Workflow workflow = currentVersion.toWorkflow();
        workflowRepository.save(workflow);
    }


    public WorkflowRuntime getWorkflowRuntime(long id) {
        return workflowRuntimeRepository.get(id);
    }

    public Workflow getWorkflow(String workId) {
        return workflowRepository.get(workId);
    }

    public void deleteVersion(long versionId) {
        WorkflowVersion version = workflowVersionRepository.get(versionId);
        if (version != null && version.isCurrent()) {
            throw FlowExecutionException.removeWorkflowError();
        }
        workflowVersionRepository.delete(versionId);
    }


    public void changeVersion(long versionId) {
        WorkflowVersion currentVersion = workflowVersionRepository.get(versionId);
        List<WorkflowVersion> versionList = workflowVersionRepository.findVersion(currentVersion.getWorkId());
        if (versionList != null) {
            for (WorkflowVersion version : versionList) {
                if (currentVersion.getId() == version.getId()) {
                    version.enableVersion();
                } else {
                    version.disableVersion();
                }
            }
        }
        workflowVersionRepository.saveAll(versionList);
        workflowRepository.save(currentVersion.toWorkflow());

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
        this.saveWorkflowVersion(workflowVersion, false);
    }

    public void saveWorkflowRuntime(WorkflowRuntime workflowRuntime) {
        this.workflowRuntimeRepository.save(workflowRuntime);
    }

    public WorkflowRuntime getWorkflowRuntime(String workId, long updatedTime) {
        return this.workflowRuntimeRepository.getByWorkId(workId, updatedTime);
    }
}
