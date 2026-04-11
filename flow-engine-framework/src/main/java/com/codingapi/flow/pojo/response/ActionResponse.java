package com.codingapi.flow.pojo.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ActionResponse {

    /**
     * 响应类型
     */
    public enum ResponseType {
        /**
         * 人工节点选择（选择分支）
         */
        MANUAL_NODE_SELECT,
        /**
         * 操作人选择（为节点设定审批人）
         */
        OPERATOR_SELECT
    }

    /**
     * 响应类型，默认为人工节点选择（向后兼容）
     */
    private final ResponseType responseType;

    /**
     * 可选节点
     */
    private final List<NodeOption> options;

    public ActionResponse(List<NodeOption> options) {
        this.responseType = ResponseType.MANUAL_NODE_SELECT;
        this.options = options;
    }

    public ActionResponse(ResponseType responseType, List<NodeOption> options) {
        this.responseType = responseType;
        this.options = options;
    }
}
