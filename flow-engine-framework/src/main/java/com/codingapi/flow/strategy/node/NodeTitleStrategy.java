package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 节点标题策略配置
 */
@NoArgsConstructor
public class NodeTitleStrategy extends BaseStrategy {

    /**
     * 审批人配置脚本
     */
    private NodeTitleScript nodeTitleScript;

    public NodeTitleStrategy(String titleScript) {
        this.nodeTitleScript = new NodeTitleScript(titleScript);
    }


    @Override
    public void copy(INodeStrategy target) {
        this.nodeTitleScript = ((NodeTitleStrategy) target).nodeTitleScript;
    }

    public String generateTitle(FlowSession flowSession) {
        return nodeTitleScript.execute(flowSession);
    }

    public static NodeTitleStrategy defaultStrategy() {
        NodeTitleStrategy strategy = new NodeTitleStrategy();
        strategy.nodeTitleScript = NodeTitleScript.defaultScript();
        return strategy;
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", nodeTitleScript.getScript());
        return map;
    }

    public static NodeTitleStrategy fromMap(Map<String, Object> map) {
        NodeTitleStrategy strategy = IMapConvertor.fromMap(map, NodeTitleStrategy.class);
        if (strategy == null) return null;
        strategy.nodeTitleScript = new NodeTitleScript((String) map.get("script"));
        return strategy;
    }


}
