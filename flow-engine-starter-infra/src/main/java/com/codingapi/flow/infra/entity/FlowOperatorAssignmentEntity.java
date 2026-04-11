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
     * 操作人 ID 列表（JSON 格式，如 [1,2,3]）
     */
    @Lob
    @Column(nullable = false)
    private String operatorIds;
}
