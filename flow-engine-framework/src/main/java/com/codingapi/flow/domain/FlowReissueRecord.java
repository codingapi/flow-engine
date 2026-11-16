package com.codingapi.flow.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 已完成流程的二次发起关系记录。
 *
 * <p>用于记录「哪条已完成的流程记录被二次发起过、新流程是哪一条」，据此可分析出实际被废止的流程。
 * 二次发起不会改写源流程记录：新流程是重新创建的独立流程实例（新的流程id），
 * 源流程记录、待办与历史记录全部保持原样可查。</p>
 */
@Getter
public class FlowReissueRecord {

    /**
     * 主键
     */
    @Setter
    private long id;

    /**
     * 被二次发起的源流程记录id
     */
    private final long sourceRecordId;

    /**
     * 被二次发起的源流程id
     */
    private final String sourceProcessId;

    /**
     * 二次发起产生的新流程记录id
     */
    private final long newRecordId;

    /**
     * 二次发起产生的新流程id
     */
    private final String newProcessId;

    /**
     * 流程编码
     */
    private final String workCode;

    /**
     * 源流程所处的运行时版本id
     */
    private final long workRuntimeId;

    /**
     * 执行二次发起的操作人（流程管理员）
     */
    private final long operatorId;

    /**
     * 执行二次发起的操作人名称
     */
    private final String operatorName;

    /**
     * 操作时间（毫秒时间戳）
     */
    private final long createTime;

    /**
     * 二次发起说明
     */
    private final String advice;

    public FlowReissueRecord(long id,
                             long sourceRecordId,
                             String sourceProcessId,
                             long newRecordId,
                             String newProcessId,
                             String workCode,
                             long workRuntimeId,
                             long operatorId,
                             String operatorName,
                             long createTime,
                             String advice) {
        this.id = id;
        this.sourceRecordId = sourceRecordId;
        this.sourceProcessId = sourceProcessId;
        this.newRecordId = newRecordId;
        this.newProcessId = newProcessId;
        this.workCode = workCode;
        this.workRuntimeId = workRuntimeId;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.createTime = createTime;
        this.advice = advice;
    }
}
