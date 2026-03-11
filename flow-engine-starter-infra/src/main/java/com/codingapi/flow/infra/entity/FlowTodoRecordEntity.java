package com.codingapi.flow.infra.entity;

import com.codingapi.flow.record.FlowRecord;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_flow_todo_record")
public class FlowTodoRecordEntity {

    /**
     * 合并记录id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程id
     * 每一次流程启动时生成，直到流程结束
     */
    private String processId;

    /**
     * 工作id
     */
    private Long workBackupId;
    /**
     * 流程编码
     */
    private String workCode;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 节点类型
     */
    private String nodeType;
    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 消息标题
     */
    private String title;
    /**
     * 读取时间
     */
    private Long readTime;

    /**
     * 当前审批人Id
     */
    private Long currentOperatorId;

    /**
     * 当前审批人名称
     */
    private String currentOperatorName;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 发起者id
     */
    private Long createOperatorId;
    /**
     * 发起者名称
     */
    private String createOperatorName;

    /**
     * 合并记录id,当存在合并记录数据时待办记录数量不会增加，内容会更新至最新的待办数据信息。
     * {@link FlowRecord#getTodoKey()}
     */
    private String mergeKey;

    /**
     * 合并记录数量
     */
    private Integer margeCount;

    /**
     * 是否可合并
     */
    private Boolean mergeable;

    /**
     * 待办记录id
     */
    private Long recordId;

    /**
     * 超时到期时间
     */
    private Long timeoutTime;
}
