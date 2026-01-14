package com.codingapi.flow.record;

import com.codingapi.flow.form.FormData;
import com.codingapi.flow.user.IFlowOperator;
import lombok.Getter;

@Getter
public class FlowRecord {

    // 待办、已办
    public static int SATE_RECORD_TODO = 0;
    public static int SATE_RECORD_DONE = 1;

    // 运行中、已完成、异常、删除
    public static int SATE_FLOW_RUNNING = 0;
    public static int SATE_FLOW_DONE = 1;
    public static int SATE_FLOW_ERROR = 2;
    public static int SATE_FLOW_DELETE = 3;

    /**
     * 记录id
     */
    private long id;
    /**
     * 工作id
     */
    private long workBackupId;
    /**
     * 流程编码
     */
    private String workCode;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 父节点id
     */
    private long fromId;
    /**
     * 表单数据
     */
    private FormData formData;
    /**
     * 消息标题
     */
    private String title;
    /**
     * 读取时间
     */
    private long readTime;
    /**
     *  流程id
     *  每一次流程启动时生成，直到流程结束
     */
    private String processId;
    /**
     * 审批意见
     */
    private FlowAdvice flowAdvice;
    /**
     * 配置信息
     */
    private FlowConfigure flowConfigure;
    /**
     * 节点状态 | 待办、已办
     */
    private int recordState;
    /**
     * 流程状态 | 运行中、已完成、异常、删除
     */
    private int flowState;
    /**
     * 更新时间
     */
    private long updateTime;
    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 完成时间
     */
    private long finishTime;
    /**
     * 是否已读
     */
    private boolean readable;
    /**
     * 发起者id
     */
    private IFlowOperator createOperator;
    /**
     * 异常信息
     */
    private String errMessage;

}
