package com.codingapi.flow.action;

import com.codingapi.flow.utils.RandomUtils;
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


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final FlowAction flowAction;

        public Builder() {
            flowAction = new FlowAction();
            flowAction.order = 1;
            flowAction.id = RandomUtils.generateStringId();
        }

        public Builder id(String id) {
            flowAction.id = id;
            return this;
        }

        public Builder title(String title) {
            flowAction.title = title;
            return this;
        }

        public Builder style(String style) {
            flowAction.style = style;
            return this;
        }

        public Builder type(ActionType type) {
            flowAction.type = type;
            return this;
        }

        public Builder order(int order) {
            flowAction.order = order;
            return this;
        }

        public Builder icon(String icon) {
            flowAction.icon = icon;
            return this;
        }

        public Builder script(String script) {
            flowAction.script = script;
            return this;
        }

        public FlowAction build() {
            return flowAction;
        }
    }

}
