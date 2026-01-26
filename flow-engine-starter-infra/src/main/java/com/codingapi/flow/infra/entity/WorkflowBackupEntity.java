package com.codingapi.flow.infra.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_flow_workflow_backup")
public class WorkflowBackupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    private Long workVersion;
    /**
     * 流程名称
     */
    private String workTitle;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 流程字节码
     */
    @Lob
    private String workflow;

}
