package com.codingapi.flow.backup;

import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;
import lombok.Setter;

/**
 * 流程备份
 */
@Getter
public class WorkflowBackup {

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


    public WorkflowBackup(Workflow workflow) {
        this.workflow = workflow.toJson(false);
        this.workId = workflow.getId();
        this.workCode = workflow.getCode();
        this.workTitle = workflow.getTitle();
        this.createTime = System.currentTimeMillis();
        this.workVersion = workflow.getCreatedTime();
    }


    public Workflow toWorkflow() {
        return Workflow.formJson(workflow);
    }
}
