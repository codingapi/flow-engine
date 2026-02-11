package com.codingapi.flow.infra.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_flow_record")
public class FlowRecordEntity {
    /**
     * 记录id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
     * 父节点id
     */
    private Long fromId;
    /**
     * 表单数据
     */
    @Lob
    private String formData;
    /**
     * 消息标题
     */
    private String title;
    /**
     * 读取时间
     */
    private Long readTime;
    /**
     * 流程id
     * 每一次流程启动时生成，直到流程结束
     */
    private String processId;

    /**
     * 流程动作
     */
    private String actionId;

    /**
     * 动作类型
     */
    private String actionType;

    /**
     * 审批意见
     */
    private String advice;

    /**
     * 签名key
     */
    private String signKey;

    /**
     * 当前审批人
     */
    private Long currentOperatorId;

    /**
     * 当前审批人名称
     */
    private String currentOperatorName;

    /**
     * 代替的审批人
     */
    private Long forwardOperatorId;

    /**
     * 代替的审批人名称
     */
    private String forwardOperatorName;

    /**
     * 有那个节点退回的
     */
    private String returnNodeId;

    /**
     * 当前节点下的排序,用于多人审批时控制节点的执行顺序
     */
    private Integer nodeOrder;

    /**
     * 是否隐藏记录（用于多人审批时）
     */
    private Boolean hidden;

    /**
     * 是否被撤销，撤销的数据不能当作待办记录
     */
    private Boolean revoked;

    /**
     * 是否抄送
     */
    private Boolean notify;

    /**
     * 节点状态 | 待办、已办
     */
    private Integer recordState;
    /**
     * 流程状态 | 运行中、已完成、异常、删除
     */
    private Integer flowState;
    /**
     * 更新时间
     */
    private Long updateTime;
    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 完成时间
     */
    private Long finishTime;
    /**
     * 是否已读
     */
    private Boolean readable;
    /**
     * 发起者id
     */
    private Long createOperatorId;

    /**
     * 发起者名称
     */
    private String createOperatorName;

    /**
     * 异常信息
     */
    private String errMessage;

    /**
     * 超时到期时间
     */
    private Long timeoutTime;
    /**
     * 是否可合并
     */
    private Boolean mergeable;
    /**
     * 被干预的用户
     */
    private Long interferedOperatorId;

    /**
     * 被干预的用户名称
     */
    private String interferedOperatorName;

    /**
     * 委托记录id
     */
    private Long delegateId;

    /**
     * 并行id
     */
    private String parallelId;

    /**
     * 并行分支节点id
     */
    private String parallelBranchNodeId;

    /**
     * 并行分支数量
     */
    private Integer parallelBranchTotal;
}
