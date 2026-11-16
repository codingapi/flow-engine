package com.codingapi.flow.infra.entity;

import com.codingapi.flow.domain.FlowReissueRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

/**
 * 已完成流程二次发起关系持久化实体，对应表 {@code t_flow_record_reissue}。
 *
 * <p>与领域对象 {@link FlowReissueRecord} 一一对应：记录「哪条已完成的流程记录被二次发起过、
 * 新流程是哪一条」，据此可分析出实际被废止的流程。</p>
 */
@Data
@Entity
@Table(name = "t_flow_record_reissue")
public class FlowReissueRecordEntity {

    /**
     * 主键，由序列生成（替代数据库自增，配合 hibernate 批插降低批量往返）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flow_reissue_record_seq")
    @SequenceGenerator(name = "flow_reissue_record_seq", sequenceName = "t_flow_record_reissue_seq", allocationSize = 50)
    private Long id;

    /**
     * 被二次发起的源流程记录id
     */
    @Column(nullable = false)
    private Long sourceRecordId;

    /**
     * 被二次发起的源流程id
     */
    @Column(nullable = false)
    private String sourceProcessId;

    /**
     * 二次发起产生的新流程记录id
     */
    @Column(nullable = false)
    private Long newRecordId;

    /**
     * 二次发起产生的新流程id
     */
    @Column(nullable = false)
    private String newProcessId;

    /**
     * 流程编码
     */
    @Column(nullable = false)
    private String workCode;

    /**
     * 源流程所处的运行时版本id
     */
    @Column(nullable = false)
    private Long workRuntimeId;

    /**
     * 执行二次发起的操作人（流程管理员）
     */
    @Column(nullable = false)
    private Long operatorId;

    /**
     * 执行二次发起的操作人名称
     */
    @Column
    private String operatorName;

    /**
     * 操作时间（毫秒时间戳）
     */
    @Column(nullable = false)
    private Long createTime;

    /**
     * 二次发起说明
     */
    @Column
    private String advice;

    /**
     * 乐观锁版本号（由 JPA {@link Version} 自动维护）
     */
    @Version
    private Long version;
}
