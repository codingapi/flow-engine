package com.codingapi.flow.infra.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity
@Table(name = "t_flow_workflow")
@DynamicUpdate
public class WorkflowEntity {

    /**
     * 流程id
     */
    @Id
    private String id;
    /**
     * 流程编号
     */
    @Column(unique = true)
    private String code;
    /**
     * 流程名称
     */
    private String title;

    /**
     * 创建者
     */
    private Long createdOperator;

    /**
     * 创建时间
     */
    private Long createdTime;


    /**
     * 更新时间
     */
    private Long updatedTime;

    /**
     * 流程表单
     */
    @Lob
    private String form;

    /**
     * 创建者脚本
     */
    @Lob
    private String operatorCreateScript;

    /**
     * 流程节点
     */
    @Lob
    private String nodes;

    /**
     * 流程设计
     */
    @Lob
    private String schema;

    /**
     * 流程策略
     */
    @Lob
    private String strategies;

    /**
     * 启用状态
     */
    private Boolean enable;
}
