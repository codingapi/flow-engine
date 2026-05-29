package com.codingapi.flow.service;

import com.codingapi.flow.cache.WorkflowRuntimeCache;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.repository.WorkflowRuntimeRepository;
import com.codingapi.flow.repository.WorkflowVersionRepository;
import com.codingapi.flow.utils.Base64Utils;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowVersion;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import com.codingapi.springboot.script.repository.GroovyScriptRepositoryContext;
import com.codingapi.springboot.script.scanner.GroovyScriptAnnotationScannerUtils;
import com.codingapi.springboot.script.scanner.GroovyScriptFieldResult;
import com.codingapi.springboot.script.temp.TempGroovyScriptContext;
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
    public void saveWorkflowVersion(WorkflowVersion currentVersion, boolean creatable, boolean enable) {
        List<WorkflowVersion> updateList = new ArrayList<>();

        currentVersion.enableVersion();
        if (currentVersion.getId() == 0) {
            // 新创建的版本，替换脚本
            WorkflowGroovyScriptUtils.resetGroovyScripts(currentVersion);
        }
        updateList.add(currentVersion);

        List<WorkflowVersion> historyVersions = workflowVersionRepository.findVersion(currentVersion.getWorkId());
        if (historyVersions != null) {

            if (!creatable) {
                historyVersions.stream().filter(WorkflowVersion::isCurrent).findFirst().ifPresent(current -> {
                    currentVersion.setId(current.getId());
                    currentVersion.setVersionName(current.getVersionName());
                });
            }

            for (WorkflowVersion version : historyVersions) {
                if (version.getId() != currentVersion.getId()) {
                    version.disableVersion();
                    updateList.add(version);
                }
            }
        }

        workflowVersionRepository.saveAll(updateList);
        Workflow workflow = currentVersion.toWorkflow();
        workflow.filterPermissions();

        if (enable) {
            try {
                workflow.enable();
            } catch (Exception ignore) {
                workflow.disable();
            }
        }
        workflowRepository.save(workflow);

        WorkflowGroovyScriptUtils.saveGroovyScripts(workflow);

    }




    /**
     * 获取流程运行时的流程配置
     *
     * @param runtimeId 运行时id
     * @return 运行时流程配置
     */
    public WorkflowRuntime getWorkflowRuntime(long runtimeId) {
        return WorkflowRuntimeCache.getInstance().get(runtimeId, () -> workflowRuntimeRepository.get(runtimeId));
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
        WorkflowGroovyScriptUtils.deleteGroovyScripts(version);
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
        Workflow workflow = currentVersion.toWorkflow();
        workflow.filterPermissions();
        workflowRepository.save(workflow);

    }

    /**
     * 更新流程版本名称
     *
     * @param versionId   版本id
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
     *
     * @param workId 流程编码
     */
    public void delete(String workId) {
        Workflow workflow = workflowRepository.get(workId);
        WorkflowGroovyScriptUtils.deleteGroovyScripts(workflow);
        workflowVersionRepository.delete(workId);
        workflowRepository.delete(workId);
    }

    /**
     * 保存流程
     *
     * @param workflow 流程对象
     */
    public void saveWorkflow(Workflow workflow) {
        this.saveWorkflow(workflow, true);
    }

    /**
     * 保存流程
     *
     * @param workflow 流程对象
     */
    public void saveWorkflow(Workflow workflow, boolean enable) {
        WorkflowVersion workflowVersion = new WorkflowVersion(workflow);
        this.saveWorkflowVersion(workflowVersion, false, enable);
    }

    /**
     * 保存流程运行时
     *
     * @param workflowRuntime 流程运行时
     */
    public void saveWorkflowRuntime(WorkflowRuntime workflowRuntime) {
        this.workflowRuntimeRepository.save(workflowRuntime);
        WorkflowRuntimeCache.getInstance().sync(workflowRuntime);
    }


    /**
     * 根据运行时版本获取运行时配置
     *
     * @param workId      流程编码
     * @param workVersion 流程版本
     * @return 流程运行时
     */
    public WorkflowRuntime getWorkflowRuntime(String workId, long workVersion) {
        return this.workflowRuntimeRepository.getByWorkId(workId, workVersion);
    }

    /**
     * 导入流程
     *
     * @param body base64
     * @return 流程id
     */
    public String importWorkflow(String body, IFlowOperator createOperator) {
        String json = Base64Utils.toJson(body);
        Workflow workflow = Workflow.formJson(json);
        workflow.resetWorkflow(createOperator);
        // 替换脚本
        WorkflowGroovyScriptUtils.resetGroovyScripts(workflow);
        this.saveWorkflow(workflow, false);
        return workflow.getId();
    }


    private static class WorkflowGroovyScriptUtils{
        /**
         * 同步保存脚本对象
         *
         * @param target 目标对象
         */
        public static void saveGroovyScripts(Object target) {
            if (target != null) {
                List<String> keys = GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(target).getKeys();
                for (String key : keys) {
                    GroovyScript groovyScript = TempGroovyScriptContext.getInstance().getGroovyScript(key);
                    if (groovyScript != null) {
                        groovyScript.save();
                    }
                }
            }
        }

        /**
         * 同步删除脚本数据
         *
         * @param target 目标对象
         */
        public static void deleteGroovyScripts(Object target) {
            if (target != null) {
                List<String> keys = GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(target).getKeys();
                for (String key : keys) {
                    GroovyScriptRepositoryContext.getInstance().delete(key);
                }
            }
        }

        /**
         * 替换脚本数据对象
         *
         * @param target 目标对象
         */
        public static void resetGroovyScripts(Object target) {
            if (target != null) {
                GroovyScriptFieldResult result = GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(target);
                result.update((key) -> {
                    GroovyScript groovyScript = GroovyScriptCacheContext.getInstance().getGroovyScript(key);
                    if (groovyScript != null) {
                        GroovyScript latestScript = groovyScript.copy(FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey());
                        latestScript.save();
                        return latestScript.getKey();
                    }
                    return key;
                });
            }
        }
    }


}
