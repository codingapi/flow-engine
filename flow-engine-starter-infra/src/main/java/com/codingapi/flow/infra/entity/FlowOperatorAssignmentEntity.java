package com.codingapi.flow.infra.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_flow_operator_assignment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_process_node",
                columnNames = {"processId", "nodeId"}
        ))
@Data
public class FlowOperatorAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程实例唯一标识
     */
    @Column(nullable = false)
    private String processId;

    /**
     * 节点 ID
     */
    @Column(nullable = false)
    private String nodeId;

    /**
     * 操作人分配列表（JSON 格式，issue #288 衍生 / #292）。
     *
     * <p>历史格式：{@code [1,2,3]}，全部按默认身份处理；
     * 含身份格式：{@code [{"operatorId":2,"identity":"department1"}]}。
     * 两种格式由仓储实现透明兼容，因此本列无需新增身份字段、无需升级 DDL。
     */
    @Lob
    @Column(nullable = false)
    private String operatorIds;
}
