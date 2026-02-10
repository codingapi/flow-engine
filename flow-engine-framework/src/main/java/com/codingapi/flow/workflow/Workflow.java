package com.codingapi.flow.workflow;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.manager.FlowNodeManager;
import com.codingapi.flow.manager.WorkflowStrategyManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.factory.NodeFactory;
import com.codingapi.flow.node.helper.BackNodeHelper;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.node.OperatorMatchScript;
import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;
import com.codingapi.flow.strategy.workflow.InterfereStrategy;
import com.codingapi.flow.strategy.workflow.UrgeStrategy;
import com.codingapi.flow.strategy.workflow.WorkflowStrategyFactory;
import com.codingapi.flow.utils.RandomUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程对象
 */
@Getter
@AllArgsConstructor
public class Workflow {

    /**
     * 流程id
     */
    private String id;
    /**
     * 流程编号
     */
    private String code;
    /**
     * 流程名称
     */
    private String title;

    /**
     * 创建者
     */
    private IFlowOperator createdOperator;

    /**
     * 创建时间
     */
    private long createdTime;

    /**
     * 更新时间
     */
    private long updatedTime;

    /**
     * 流程表单
     */
    private FormMeta form;

    /**
     * 创建者脚本
     */
    private OperatorMatchScript operatorCreateScript;

    /**
     * 流程节点
     */
    private List<IFlowNode> nodes;


    /**
     * 流程设计
     */
    private String schema;

    /**
     * 流程策略
     */
    private List<IWorkflowStrategy> strategies;


    protected Workflow() {
        this.id = RandomUtils.generateStringId();
        this.code = RandomUtils.generateWorkflowCode();
        this.createdTime = System.currentTimeMillis();
        this.operatorCreateScript = OperatorMatchScript.any();
        this.nodes = new ArrayList<>();
        this.strategies = defaultStrategies();
        this.updateTime();
    }

    public void addDefaultNodesAndEdges() {
        this.nodes.addAll(defaultNodes());
    }

    private List<IFlowNode> defaultNodes() {
        List<IFlowNode> nodeList = new ArrayList<>();
        nodeList.add(new StartNode());
        nodeList.add(new EndNode());
        return nodeList;
    }


    private List<IWorkflowStrategy> defaultStrategies() {
        List<IWorkflowStrategy> strategyList = new ArrayList<>();
        strategyList.add(InterfereStrategy.defaultStrategy());
        strategyList.add(UrgeStrategy.defaultStrategy());
        return strategyList;
    }


    protected void setId(String id) {
        this.id = id;
    }

    protected void setCode(String code) {
        this.code = code;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    protected void setCreatedOperator(IFlowOperator createdOperator) {
        this.createdOperator = createdOperator;
    }

    protected void setForm(FormMeta form) {
        this.form = form;
    }

    protected void setOperatorCreateScript(OperatorMatchScript operatorCreateScript) {
        this.operatorCreateScript = operatorCreateScript;
    }

    protected void setNodes(List<IFlowNode> nodes) {
        this.nodes = nodes;
    }

    protected void setSchema(String schema) {
        this.schema = schema;
    }

    protected void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    protected void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    protected void setStrategies(List<IWorkflowStrategy> strategies) {
        this.strategies = strategies;
    }


    /**
     * 转换为json
     *
     * @return json
     */
    public String toJson(boolean hasSchema) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("code", code);
        map.put("title", title);
        if (createdOperator != null) {
            map.put("createdOperator", String.valueOf(createdOperator.getUserId()));
        }
        if (form != null) {
            map.put("form", form.toMap());
        }
        map.put("operatorCreateScript", operatorCreateScript.getScript());
        map.put("nodes", nodes.stream().map(IFlowNode::toMap).toList());
        map.put("createdTime", String.valueOf(createdTime));
        map.put("updatedTime", String.valueOf(updatedTime));
        map.put("schema", hasSchema ? schema : null);
        map.put("strategies", strategies.stream().map(IWorkflowStrategy::toMap).toList());
        return JSON.toJSONString(map);
    }


    @SuppressWarnings("unchecked")
    public static Workflow formJson(String json) {
        Map<String, Object> data = JSON.parseObject(json);
        long createOperator = Long.parseLong((String) data.get("createdOperator"));

        Workflow workflow = new Workflow();
        workflow.setId((String) data.get("id"));
        workflow.setCode((String) data.get("code"));
        workflow.setTitle((String) data.get("title"));
        workflow.setSchema((String) data.get("schema"));
        workflow.setCreatedTime(Long.parseLong((String) data.get("createdTime")));
        workflow.setUpdatedTime(Long.parseLong((String) data.get("updatedTime")));
        workflow.setCreatedOperator(GatewayContext.getInstance().getFlowOperator(createOperator));
        workflow.setForm(FormMeta.fromMap((Map<String, Object>) data.get("form")));
        workflow.setOperatorCreateScript(new OperatorMatchScript((String) data.get("operatorCreateScript")));

        List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
        if (nodes != null) {
            List<IFlowNode> nodeList = new ArrayList<>();
            for (Map<String, Object> node : nodes) {
                IFlowNode flowNode = NodeFactory.getInstance().createNode(node);
                nodeList.add(flowNode);
            }
           workflow.setNodes(nodeList);
        }

        List<Map<String, Object>> strategies = (List<Map<String, Object>>) data.get("strategies");
        if (strategies != null) {
            List<IWorkflowStrategy> strategyList = new ArrayList<>();
            for (Map<String, Object> item : strategies) {
                IWorkflowStrategy strategy = WorkflowStrategyFactory.getInstance().createStrategy(item);
                strategyList.add(strategy);
            }
            workflow.setStrategies(strategyList);
        }

        return workflow;
    }

    public WorkflowStrategyManager strategyManager() {
        return new WorkflowStrategyManager(strategies);
    }


    /**
     * 匹配创建者
     *
     * @param flowOperator 创建者
     * @return 是否匹配
     */
    public boolean matchCreatedOperator(IFlowOperator flowOperator) {
        return operatorCreateScript.execute(flowOperator);
    }

    /**
     * 验证流程
     */
    public void verify() {
        this.verifyFields();
        this.verifyNodes();
    }

    private void verifyFields() {
        if (!StringUtils.hasText(id)) {
            throw FlowValidationException.required("workflow id");
        }
        if (!StringUtils.hasText(code)) {
            throw FlowValidationException.required("workflow code");
        }
        if (!StringUtils.hasText(title)) {
            throw FlowValidationException.required("workflow title");
        }
        if (createdTime <= 0) {
            throw FlowValidationException.required("workflow createdTime");
        }
        if (form == null) {
            throw FlowValidationException.required("workflow form");
        }
        if (createdOperator == null) {
            throw FlowValidationException.required("workflow createdOperator");
        }
        if (nodes == null || nodes.isEmpty()) {
            throw FlowConfigException.nodeConfigError(title, "nodes can not be null");
        }
    }


    /**
     * 获取可以回退的节点列表
     */
    public List<IFlowNode> getNackNodes(IFlowNode node) {
        BackNodeHelper backNodeHelper = new BackNodeHelper(this, node);
        return backNodeHelper.getBackNodes();
    }


    private void verifyNodes() {
        int start = 0;
        int end = 0;
        for (IFlowNode node : nodes) {
            if (node instanceof StartNode) {
                start++;
            }
            if (node instanceof EndNode) {
                end++;
            }
        }
        if (start != 1 || end != 1) {
            throw FlowConfigException.nodeConfigError(title, "must have one start node and one end node");
        }

        for (IFlowNode node : nodes) {
            node.verifyNode(form);
        }
    }

    public List<IFlowNode> nextNodes(IFlowNode node) {
        FlowNodeManager flowNodeManager = new FlowNodeManager(nodes);
        return flowNodeManager.getNextNodes(node);
    }

    public IFlowNode getFlowNode(String nodeId) {
        FlowNodeManager nodeManager = new FlowNodeManager(nodes);
        return nodeManager.getFlowNode(nodeId);
    }

    public IFlowNode getStartNode() {
        return nodes.stream().filter(node -> node instanceof StartNode)
                .findFirst().orElse(null);
    }

    public IFlowNode getEndNode() {
        return nodes.stream().filter(node -> node instanceof EndNode)
                .findFirst().orElse(null);
    }

    /**
     * 判断是否是后续的节点
     *
     * @param currentNode 当前节点
     * @param nextNode    退回节点
     * @return 是否是后续的节点
     */
    public boolean isNextNode(IFlowNode currentNode, IFlowNode nextNode) {
        List<IFlowNode> nextNodes = nextNodes(currentNode);
        for (IFlowNode node : nextNodes) {
            if (node.equals(nextNode)) {
                return true;
            } else {
                if (this.isNextNode(node, nextNode)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateTime() {
        this.updatedTime = System.currentTimeMillis();
    }
}
