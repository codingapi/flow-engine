package com.codingapi.flow.node.config;

import com.codingapi.flow.node.BaseConfigNode;
import com.codingapi.flow.node.builder.ConfigNodeBuilder;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

/**
 * 子流程
 */
public class SubProcessNode extends BaseConfigNode {

    public static final String NODE_TYPE = "sub_process";
    public static final String DEFAULT_NAME = "子流程";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public SubProcessNode(String id, String name) {
        super(id, name);
    }

    public SubProcessNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static SubProcessNode formMap(Map<String, Object> map) {
        return BaseConfigNode.formMap(map, SubProcessNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ConfigNodeBuilder<Builder,SubProcessNode> {
        public Builder() {
            super(new SubProcessNode());
        }
    }
}
