package com.codingapi.flow.workflow;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.IAuditNode;
import com.codingapi.flow.node.IBranchNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.audit.EndNode;
import com.codingapi.flow.node.audit.StartNode;
import com.codingapi.flow.node.factory.NodeFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.node.OperatorMatchScript;
import com.codingapi.flow.session.FlowSession;
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
     * 流程关系
     */
    private List<FlowEdge> edges;

    /**
     * 流程设计
     */
    private String schema;

    /**
     * 开启干预
     */
    private boolean isInterfere;

    /**
     * 开启撤销
     */
    private boolean isRevoke;

    protected Workflow() {
        this.id = RandomUtils.generateStringId();
        this.code = RandomUtils.generateWorkflowCode();
        this.createdTime = System.currentTimeMillis();
        this.operatorCreateScript = OperatorMatchScript.any();
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.isInterfere = false;
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

    protected void setEdges(List<FlowEdge> edges) {
        this.edges = edges;
    }

    protected void setSchema(String schema) {
        this.schema = schema;
    }

    protected void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    protected void setIsInterfere(boolean isInterfere) {
        this.isInterfere = isInterfere;
    }

    protected void setIsRevoke(boolean isRevoke) {
        this.isRevoke = isRevoke;
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
        map.put("createdOperator", String.valueOf(createdOperator.getUserId()));
        map.put("form", form.toMap());
        map.put("operatorCreateScript", operatorCreateScript.getScript());
        map.put("nodes", nodes.stream().map(IFlowNode::toMap).toList());
        map.put("edges", edges);
        map.put("createdTime", String.valueOf(createdTime));
        map.put("schema", hasSchema ? schema : null);
        map.put("isInterfere", isInterfere);
        map.put("isRevoke", isRevoke);
        return JSON.toJSONString(map);
    }


    @SuppressWarnings("unchecked")
    public static Workflow formJson(String json) {
        Map<String, Object> data = JSON.parseObject(json);
        long createOperator = Long.parseLong((String) data.get("createdOperator"));
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
        Workflow workflow = new Workflow();
        workflow.setId((String) data.get("id"));
        workflow.setCode((String) data.get("code"));
        workflow.setTitle((String) data.get("title"));
        workflow.setSchema((String) data.get("schema"));
        workflow.setIsInterfere((boolean) data.get("isInterfere"));
        workflow.setIsRevoke((boolean) data.get("isRevoke"));
        workflow.setCreatedTime(Long.parseLong((String) data.get("createdTime")));
        workflow.setCreatedOperator(GatewayContext.getInstance().getFlowOperator(createOperator));
        workflow.setForm(FormMeta.fromMap((Map<String, Object>) data.get("form")));
        workflow.setOperatorCreateScript(new OperatorMatchScript((String) data.get("operatorCreateScript")));

        if (nodes != null) {
            List<IFlowNode> nodeList = new ArrayList<>();
            for (Map<String, Object> node : nodes) {
                IFlowNode flowNode = NodeFactory.getInstance().createNode(node);
                nodeList.add(flowNode);
            }
            workflow.setNodes(nodeList);
        }

        List<Map<String, Object>> edges = (List<Map<String, Object>>) data.get("edges");
        if (edges != null) {
            List<FlowEdge> edgeList = new ArrayList<>();
            for (Map<String, Object> edge : edges) {
                FlowEdge flowEdge = new FlowEdge((String) edge.get("from"), (String) edge.get("to"));
                edgeList.add(flowEdge);
            }
            workflow.setEdges(edgeList);
        }

        return workflow;
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
        this.verifyEdges();
    }

    private void verifyFields() {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("workflow id can not be null");
        }
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("workflow code can not be null");
        }
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("workflow title can not be null");
        }
        if (createdTime <= 0) {
            throw new IllegalArgumentException("workflow createdTime can not be null");
        }
        if (form == null) {
            throw new IllegalArgumentException("workflow form can not be null");
        }
        if (createdOperator == null) {
            throw new IllegalArgumentException("workflow createdOperator can not be null");
        }
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("workflow nodes can not be null");
        }
        if (edges == null || edges.isEmpty()) {
            throw new IllegalArgumentException("workflow edges can not be null");
        }
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
            throw new IllegalArgumentException("workflow nodes must have one start node and one end node");
        }

        for (IFlowNode node : nodes) {
            node.verify(form);
        }
    }

    private void verifyEdges() {
        for (FlowEdge edge : edges) {
            if (edge.getFrom().equals(edge.getTo())) {
                throw new IllegalArgumentException("workflow edges can not have same from and to");
            }
        }
        IFlowNode startNode = this.nodes.stream().filter(node -> node instanceof StartNode).findFirst().get();

        List<IFlowNode> nextNodes = edgeNext(startNode);
        for (IFlowNode nextNode : nextNodes) {
            this.verifyNextEdge(nextNode);
        }
    }

    private void verifyNextEdge(IFlowNode node) {
        if (node instanceof EndNode) {
            return;
        } else {
            List<IFlowNode> nextNodes = edgeNext(node);
            if (nextNodes.isEmpty()) {
                throw new IllegalArgumentException("workflow edges must have one end edge");
            }
            for (IFlowNode nextNode : nextNodes) {
                this.verifyNextEdge(nextNode);
            }
        }
    }


    public List<IFlowNode> edgeNext(IFlowNode node) {
        return edges.stream().filter(edge -> edge.getFrom().equals(node.getId()))
                .map(edge -> nodes.stream().filter(item -> item.getId().equals(edge.getTo())).findFirst().get()).toList();
    }

    public List<IAuditNode> nextNodes(FlowSession session) {
        List<IFlowNode> nodeList = edgeNext(session.getCurrentNode());
        return this.loadNextAuditNodes(nodeList, session);
    }


    private List<IAuditNode> loadNextAuditNodes(List<IFlowNode> nodeList, FlowSession session) {
        List<IAuditNode> auditNodeList = new ArrayList<>();
        for (IFlowNode node : nodeList) {
            if (node instanceof IAuditNode) {
                auditNodeList.add((IAuditNode) node);
            }
            if (node instanceof IBranchNode) {
                if (((IBranchNode) node).match(session)) {
                    List<IFlowNode> nextNodes = this.edgeNext(node);
                    auditNodeList.addAll(this.loadNextAuditNodes(nextNodes, session));
                }
            }
        }
        return auditNodeList;
    }


    public IAuditNode getAuditNode(String nodeId) {
        return nodes.stream()
                .filter(node -> node instanceof IAuditNode)
                .filter(node -> node.getId().equals(nodeId))
                .map(node -> (IAuditNode) node)
                .findFirst().orElse(null);
    }

    public IAuditNode getStartNode() {
        return nodes.stream().filter(node -> node instanceof StartNode)
                .map(node -> (IAuditNode) node)
                .findFirst().orElse(null);
    }

    public IAuditNode getEndNode() {
        return nodes.stream().filter(node -> node instanceof EndNode)
                .map(node -> (IAuditNode) node)
                .findFirst().orElse(null);
    }
}
