package com.codingapi.flow.pojo.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ActionResponse {

    /**
     * 可选节点
     */
    private final List<NodeOption> options;

    public ActionResponse(List<NodeOption> options) {
        this.options = options;
    }
}
