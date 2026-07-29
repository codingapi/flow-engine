package com.codingapi.flow.workflow.runtime;

import com.codingapi.flow.workflow.Workflow;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import com.codingapi.springboot.script.scanner.GroovyScriptAnnotationScannerUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时的流程配置
 */
@Getter
@AllArgsConstructor
public class WorkflowRuntime {

    @Setter
    private long id;
    /**
     * 工作id
     */
    private String workId;
    /**
     * 流程编码
     */
    private String workCode;

    /**
     * 工作版本
     */
    private long workVersion;
    /**
     * 流程名称
     */
    private String workTitle;
    /**
     * 创建时间
     */
    private long createTime;
    /**
     * 流程字节码
     */
    private String workflow;
    /**
     * 脚本快照(脚本key -&gt; 脚本内容)。
     * <p>
     * 运行时创建时固化流程引用的全部脚本内容，使流程设计阶段对脚本内容的修改
     * 不会影响已经在运行的流程。
     */
    private Map<String, String> scripts;


    public WorkflowRuntime(Workflow workflow) {
        this.workflow = workflow.toJson();
        this.workId = workflow.getId();
        this.workCode = workflow.getCode();
        this.workTitle = workflow.getTitle();
        this.createTime = System.currentTimeMillis();
        this.workVersion = workflow.getUpdatedTime();
        this.scripts = snapshotScripts(workflow);
    }


    public Workflow toWorkflow() {
        return Workflow.formJson(workflow);
    }

    /**
     * 扫描流程引用的全部脚本key，并从脚本缓存中固化其内容，形成运行时脚本快照。
     *
     * @param workflow 流程对象
     * @return 脚本key到脚本内容的映射
     */
    private static Map<String, String> snapshotScripts(Workflow workflow) {
        Map<String, String> scripts = new HashMap<>();
        List<String> keys = GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(workflow).getKeys();
        if (keys == null) {
            return scripts;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            GroovyScript groovyScript = GroovyScriptCacheContext.getInstance().getGroovyScript(key);
            if (groovyScript != null && groovyScript.getScript() != null) {
                scripts.put(key, groovyScript.getScript());
            }
        }
        return scripts;
    }
}
