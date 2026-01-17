package com.codingapi.flow.action;

public enum ActionType {
    // 默认，用于没有审批操作时配置
    DEFAULT,
    // 保存
    SAVE,
    // 通过，流程继续往下流转
    PASS,
    // 拒绝，拒绝时需要根据拒绝的配置流程来设置
    REJECT,
    // 加签，指定给其他人一块审批，以会签模式来处理
    ADD_AUDIT,
    // 委派，委派给其他人员来审批，当人员审批完成以后再流程给自己审批
    DELEGATE,
    // 退回，退回时需要设置退回的节点
    RETURN,
    // 转办，将流程转移给指定用户来审批，需要配置人员匹配范围
    TRANSFER,
    // 自定义，自定义按钮，需要配置脚本
    CUSTOM,
}
