package com.codingapi.flow.node.nodes;

import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.script.node.RouterNodeScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 路由分支节点
 */
public class RouterNode extends BaseFlowNode {

    public static final String NODE_TYPE = NodeType.ROUTER.name();
    public static final String DEFAULT_NAME = "路由节点";

    @Setter
    private RouterNodeScript routerNodeScript;

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public RouterNode(String id, String name) {
        super(id, name);
    }

    public RouterNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
        routerNodeScript = RouterNodeScript.defaultNodeScript();
    }


    @Override
    public List<IFlowNode> filterBranches(List<IFlowNode> nodeList, FlowSession flowSession) {
        String nextNodeId = routerNodeScript.execute(flowSession);
        Workflow workflow = flowSession.getWorkflow();
        IFlowNode nextNode = workflow.getFlowNode(nextNodeId);
        if (nextNode == null) {
            throw FlowExecutionException.routerNodeNotFound(nextNodeId);
        }
        return List.of(nextNode);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", routerNodeScript.getScript());
        return map;
    }

    public static RouterNode formMap(Map<String, Object> map) {
        RouterNode routerNode = BaseFlowNode.fromMap(map, RouterNode.class);
        routerNode.routerNodeScript = new RouterNodeScript((String) map.get("script"));
        return routerNode;
    }

    @Override
    public void verifyNode(FormMeta form) {
        super.verifyNode(form);
        if (routerNodeScript == null) {
            throw FlowConfigException.routerNodeScriptNull();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, RouterNode> {
        public Builder() {
            super(new RouterNode());
        }

        public Builder routerNodeScript(String script) {
            node.routerNodeScript = new RouterNodeScript(script);
            return this;
        }
    }
}
