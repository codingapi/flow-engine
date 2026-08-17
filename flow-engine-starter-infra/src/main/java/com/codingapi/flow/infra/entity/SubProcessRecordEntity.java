package com.codingapi.flow.infra.entity;

import com.codingapi.flow.domain.SubProcessRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Data;

/**
 * 子流程执行记录持久化实体，对应表 {@code t_flow_sub_process_record}。
 *
 * <p>与领域对象 {@link SubProcessRecord} 一一对应：实例列表以 JSON 文本存储于
 * {@link #instances}，聚合状态以枚举名存储于 {@link #state}。</p>
 */
@Data
@Entity
@Table(name = "t_flow_sub_process_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_sub_process_group", columnNames = "groupId"))
public class SubProcessRecordEntity {

    /**
     * 主键，由序列生成（替代数据库自增，配合 hibernate 批插降低批量往返）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flow_sub_process_record_seq")
    @SequenceGenerator(name = "flow_sub_process_record_seq", sequenceName = "t_flow_sub_process_record_seq", allocationSize = 50)
    private Long id;

    /**
     * 本次子流程执行的分组id，唯一约束键
     */
    @Column(nullable = false)
    private String groupId;

    /**
     * 父流程（主流程）的流程id
     */
    @Column(nullable = false)
    private String parentProcessId;

    /**
     * 父流程中子流程节点的执行记录id
     */
    @Column(nullable = false)
    private Long parentRecordId;

    /**
     * 父流程（主流程）的运行实例id，用于恢复父流程运行时上下文
     */
    @Column(nullable = false)
    private Long parentWorkRuntimeId;

    /**
     * 父流程中子流程节点的节点id
     */
    @Column(nullable = false)
    private String nodeId;

    /**
     * 本次创建的子流程实例总数
     */
    @Column(nullable = false)
    private Integer totalCount;

    /**
     * 子流程实例列表（JSON 文本，对应 {@link SubProcessRecord.Instance}）
     */
    @Lob
    @Column(nullable = false)
    private String instances;

    /**
     * 聚合状态，存储 {@link SubProcessRecord.State} 的枚举名
     */
    @Column(nullable = false)
    private String state;

    /**
     * 创建时间（毫秒时间戳）
     */
    @Column(nullable = false)
    private Long createTime;

    /**
     * 结束时间（毫秒时间戳），未结束时为 0
     */
    @Column(nullable = false)
    private Long finishTime;

    /**
     * 乐观锁版本号（由 JPA {@link Version} 自动维护）
     */
    @Version
    private Long version;
}