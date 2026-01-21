package com.codingapi.flow.strategy;

import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.session.FlowSession;

import java.util.HashMap;
import java.util.Map;

public class NodeTitleStrategy implements INodeStrategy {

    /**
     * 审批人配置脚本
     */
    private NodeTitleScript nodeTitleScript;

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
        Map<String, Object> map = new HashMap<>();
        map.put("type", strategyType());
        map.put("script", nodeTitleScript.getScript());
        return map;
    }

    public static NodeTitleStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        NodeTitleStrategy strategy = new NodeTitleStrategy();
        strategy.setOperatorScript((String) map.get("script"));
        return strategy;
    }


}
