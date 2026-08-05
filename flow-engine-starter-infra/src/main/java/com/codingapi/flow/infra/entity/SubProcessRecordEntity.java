package com.codingapi.flow.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Data;

@Data
@Entity
@Table(name = "t_flow_sub_process_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_sub_process_group", columnNames = "groupId"))
public class SubProcessRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String groupId;

    @Column(nullable = false)
    private String parentProcessId;

    @Column(nullable = false)
    private Long parentRecordId;

    @Column(nullable = false)
    private Long parentWorkRuntimeId;

    @Column(nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private Integer totalCount;

    @Lob
    @Column(nullable = false)
    private String instances;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private Long createTime;

    @Column(nullable = false)
    private Long finishTime;

    @Version
    private Long version;
}
