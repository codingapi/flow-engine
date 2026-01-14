package com.codingapi.flow.action;

/**
 * 流程按钮配置
 */
public interface IFlowAction {

    /**
     * 按钮名称
     */
    default String title() {
        FlowActionType currentType = this.type();
        return switch (currentType) {
            case PASS -> "同意";
            case REJECT -> "拒绝";
            case RETURN -> "退回";
            case CANCEL -> "撤销";
            case TRANSFER -> "转办";
            case CUSTOM -> "自定义";
        };
    }

    /**
     * 按钮类型
     */
    FlowActionType type();

}
