package com.codingapi.flow.strategy;

import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.Map;

public class NodeTitleStrategy extends BaseStrategy {

    /**
     * 审批人配置脚本
     */
    private NodeTitleScript nodeTitleScript;

    public NodeTitleStrategy() {
        super(RandomUtils.generateStringId());
    }

    @Override
    public void copy(INodeStrategy target) {
        this.nodeTitleScript = ((NodeTitleStrategy) target).nodeTitleScript;
    }

    public void setOperatorScript(String script) {
        this.nodeTitleScript = new NodeTitleScript(script);
    }


    public String generateTitle(FlowSession flowSession) {
        return nodeTitleScript.execute(flowSession);
    }

    public static NodeTitleStrategy defaultStrategy() {
        NodeTitleStrategy strategy = new NodeTitleStrategy();
        strategy.setOperatorScript(NodeTitleScript.SCRIPT_DEFAULT);
        return strategy;
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", nodeTitleScript.getScript());
        return map;
    }

    public static NodeTitleStrategy fromMap(Map<String, Object> map) {
        NodeTitleStrategy strategy = BaseStrategy.fromMap(map, NodeTitleStrategy.class);
        if (strategy == null) return null;
        strategy.setOperatorScript((String) map.get("script"));
        return strategy;
    }


}
