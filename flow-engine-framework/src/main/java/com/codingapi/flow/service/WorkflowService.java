package com.codingapi.flow.service;

import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.repository.WorkflowRuntimeRepository;
import com.codingapi.flow.repository.WorkflowVersionRepository;
import com.codingapi.flow.utils.Base64Utils;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowVersion;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程设计器服务
 */
@AllArgsConstructor
public class WorkflowService {

    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowRuntimeRepository workflowRuntimeRepository;


    /**
     * 保存流程版本
     *
     * @param currentVersion 当前版本
     * @param creatable      是否创建新的版本
     */
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


    /**
     * 获取流程运行时的流程配置
     *
     * @param runtimeId 运行时id
     * @return 运行时流程配置
     */
    public WorkflowRuntime getWorkflowRuntime(long runtimeId) {
        return workflowRuntimeRepository.get(runtimeId);
    }

    /**
     * 获取流程对象
     *
     * @param workId 流程编码
     * @return 流程对象
     */
    public Workflow getWorkflow(String workId) {
        return workflowRepository.get(workId);
    }

    /**
     * 删除流程版本
     *
     * @param versionId 版本id
     */
    public void deleteVersion(long versionId) {
        WorkflowVersion version = workflowVersionRepository.get(versionId);
        if (version != null && version.isCurrent()) {
            throw FlowExecutionException.removeWorkflowError();
        }
        workflowVersionRepository.delete(versionId);
    }


    /**
     * 切换流程版本
     *
     * @param versionId 版本id
     */
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

    /**
     * 更新流程版本名称
     * @param versionId 版本id
     * @param versionName 版本名称
     */
    public void updateVersionName(long versionId, String versionName) {
        WorkflowVersion workflowVersion = workflowVersionRepository.get(versionId);
        if (workflowVersion != null) {
            workflowVersion.setVersionName(versionName);
            workflowVersionRepository.save(workflowVersion);
        }
    }

    /**
     * 删除流程
     * @param workId 流程编码
     */
    public void delete(String workId) {
        workflowVersionRepository.delete(workId);
        workflowRepository.delete(workId);
    }

    /**
     * 保存流程
     * @param workflow 流程对象
     */
    public void saveWorkflow(Workflow workflow) {
        WorkflowVersion workflowVersion = new WorkflowVersion(workflow);
        this.saveWorkflowVersion(workflowVersion, false);
    }

    /**
     * 保存流程运行时
     * @param workflowRuntime 流程运行时
     */
    public void saveWorkflowRuntime(WorkflowRuntime workflowRuntime) {
        this.workflowRuntimeRepository.save(workflowRuntime);
    }


    /**
     * 根据运行时版本获取运行时配置
     * @param workId 流程编码
     * @param workVersion 流程版本
     * @return 流程运行时
     */
    public WorkflowRuntime getWorkflowRuntime(String workId, long workVersion) {
        return this.workflowRuntimeRepository.getByWorkId(workId, workVersion);
    }

    /**
     * 导入流程
     * @param body base64
     * @return 流程id
     */
    public String importWorkflow(String body) {
        String json = Base64Utils.toJson(body);
        Workflow workflow = Workflow.formJson(json);
        workflow.resetWorkflow();
        this.saveWorkflow(workflow);
        return workflow.getId();
    }
}
