package com.codingapi.flow.action;

public enum FlowActionType {
    // 通过
    PASS,
    // 拒绝
    REJECT,
    // 退回
    RETURN,
    // 撤销
    CANCEL,
    // 转办
    TRANSFER,
    // 自定义
    CUSTOM
}
