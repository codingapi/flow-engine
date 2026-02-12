package com.codingapi.flow.pojo.response;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.FormMeta;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程详情
 */
@Data
public class FlowContent {

    /**
     * 流程记录编号
     */
    private long recordId;
    /**
     * 流程编号
     */
    private String workflowCode;
    /**
     * 流程视图
     */
    private String view;

    /**
     * 表单元数据
     */
    private FormMeta form;
    /**
     * 流程记录
     */
    private List<Body> todos;

    /**
     * 流程按钮
     */
    private List<IFlowAction> actions;

    /**
     * 是否可合并
     */
    private boolean mergeable;

    /**
     * 发起者id
     */
    private long createOperatorId;

    /**
     * 历史记录
     */
    private List<History> histories;

    /**
     * 下一审批
     */
    private List<NextNode> nextNodes;

    /**
     * 流程图
     */
    @Data
    public static class NextNode{
        /**
         * 节点名称
         */
        private String nodeId;
        /**
         * 节点名称
         */
        private String nodeName;
        /**
         * 节点类型
         */
        private String nodeType;
    }

    @Data
    public static class History{
        /**
         * 流程编号
         */
        private long recordId;
        /**
         *  流程标题
         */
        private String title;

        /**
         * 审批意见
         */
        private String advice;

        /**
         * 签名key
         */
        private String signKey;

        /**
         * 节点名称
         */
        private String nodeName;

        /**
         * 节点id
         */
        private String nodeId;
        /**
         * 节点类型
         */
        private String nodeType;

        /**
         * 更新时间
         */
        private long updateTime;

        /**
         * 当前审批人Id
         */
        private long currentOperatorId;

        /**
         * 当前审批人名称
         */
        private String currentOperatorName;
    }

    @Data
    public static class Body {
        /**
         *  流程记录编号
         */
        private long recordId;
        /**
         *  流程标题
         */
        private String title;
        /**
         *  表单数据
         */
        private Map<String, Object> data;

        /**
         * 节点状态 | 待办、已办
         */
        private int recordState;
        /**
         * 流程状态 | 运行中、已完成、异常、删除
         */
        private int flowState;
    }
}
