package com.codingapi.flow.action;

import lombok.Data;

/**
 * 流程按钮配置
 */
@Data
public class FlowAction {

    /**
     * 编号
     */
    private String id;
    /**
     * 名称
     */
    private String title;
    /**
     * 样式
     */
    private String style;
    /**
     * 类型
     */
    private ActionType type;
    /**
     * 排序
     */
    private int order;
    /**
     * 图标
     */
    private String icon;
    /**
     * 脚本
     */
    private String script;


}
