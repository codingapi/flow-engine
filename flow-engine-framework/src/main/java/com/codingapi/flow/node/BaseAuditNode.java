package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.PassAction;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.builder.IFormFieldPermissionsNode;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.node.manager.FieldPermissionManager;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.INodeStrategy;
import com.codingapi.flow.strategy.MultiOperatorAuditStrategy;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseAuditNode extends BaseFlowNode implements IFlowNode, IFormFieldPermissionsNode {

    public static final String DEFAULT_VIEW = "default";

    /**
     * 渲染视图
     */
    @Getter
    @Setter
    private String view;

    /**
     * 审批人配置脚本
     */
    private OperatorLoadScript operatorScript;

    /**
     * 节点待办标题脚本
     */
    private NodeTitleScript nodeTitleScript;

    /**
     * 异常触发脚本
     */
    private ErrorTriggerScript errorTriggerScript;

    /**
     * 表单字段权限
     */
    @Setter
    private List<FormFieldPermission> formFieldPermissions;

    /**
     * 节点策略
     */
    @Setter
    private List<INodeStrategy> nodeStrategies;


    public BaseAuditNode(String id, String name, List<IFlowAction> actions, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldPermissions, List<INodeStrategy> nodeStrategies) {
        super(id, name, actions);
        this.view = view;
        this.operatorScript = operatorScript;
        this.nodeTitleScript = nodeTitleScript;
        this.errorTriggerScript = errorTriggerScript;
        this.formFieldPermissions = formFieldPermissions;
        this.nodeStrategies = nodeStrategies;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("id", id);
        map.put("view", view);
        map.put("operatorScript", operatorScript.getScript());
        map.put("nodeTitleScript", nodeTitleScript.getScript());
        map.put("errorTriggerScript", errorTriggerScript.getScript());
        map.put("formFieldPermissions", formFieldPermissions);
        map.put("type", getType());
        map.put("actions", actions.stream().map(IFlowAction::toMap).toList());
        map.put("nodeStrategies", nodeStrategies.stream().map(INodeStrategy::toMap).toList());
        return map;
    }



    /**
     * 设置审批人配置脚本
     *
     * @param operatorScript 审批人配置脚本
     */
    public void setOperatorScript(String operatorScript) {
        this.operatorScript = new OperatorLoadScript(operatorScript);
    }

    /**
     * 设置节点待办标题脚本
     *
     * @param nodeTitleScript 节点待办标题脚本
     */
    public void setNodeTitleScript(String nodeTitleScript) {
        this.nodeTitleScript = new NodeTitleScript(nodeTitleScript);
    }

    /**
     * 错误触发脚本
     *
     * @param errorTriggerScript 错误触发脚本
     */
    public void setErrorTriggerScript(String errorTriggerScript) {
        this.errorTriggerScript = new ErrorTriggerScript(errorTriggerScript);
    }


    public FieldPermissionManager formFieldsPermissionsManager() {
        return new FieldPermissionManager(formFieldPermissions);
    }

    public ActionManager actions() {
        return new ActionManager(actions);
    }


    public OperatorManager operators(FlowSession flowSession) {
        return new OperatorManager(operatorScript.execute(flowSession));
    }

    public String generateTitle(FlowSession flowSession) {
        return nodeTitleScript.execute(flowSession);
    }

    public ErrorThrow errorTrigger(FlowSession flowSession) {
        return errorTriggerScript.execute(flowSession);
    }

    public void addAction(IFlowAction action) {
        if (this.actions == null) {
            this.actions = new ArrayList<>();
        }
        this.actions.add(action);
    }

    public void verifyNode(FormMeta form) {
        if (!StringUtils.hasText(view)) {
            throw new IllegalArgumentException("view can not be null");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name can not be null");
        }
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("id can not be null");
        }
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("actions can not be null");
        }
        if (operatorScript == null) {
            throw new IllegalArgumentException("operator can not be null");
        }
        if (nodeTitleScript == null) {
            throw new IllegalArgumentException("nodeTitle can not be null");
        }
        FieldPermissionManager fieldPermissionManager = this.formFieldsPermissionsManager();
        fieldPermissionManager.verifyPermissions(form);
    }

    public StrategyManager strategies() {
        return new StrategyManager(nodeStrategies);
    }


    @Override
    public boolean continueTrigger(FlowSession session) {
        return false;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {
        StrategyManager strategyManager = this.strategies();
        flowRecord.setTitle(this.generateTitle(session));
        flowRecord.setTimeoutTime(strategyManager.getTimeoutTime());
        flowRecord.setMergeable(strategyManager.isMergeable());
    }

    @Override
    public boolean isDone(FlowSession session) {
        List<FlowRecord> currentRecords = session.getCurrentNodeRecords();
        FlowRecord currentRecord = session.getCurrentRecord();
        // 多人审批
        if (currentRecords.size() > 1) {
            StrategyManager strategyManager = this.strategies();
            MultiOperatorAuditStrategy.Type multiOperatorAuditStrategyType = strategyManager.getMultiOperatorAuditStrategyType();
            // 顺序审批
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.SEQUENCE) {
                int currentOrder = currentRecord.getNodeOrder();
                int maxNodeOrder = currentRecords.size() - 1;
                return currentOrder >= maxNodeOrder;
            }
            // 或签
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.ANY) {
                return true;
            }
            // 并签
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.MERGE) {
                float percent = strategyManager.getMultiOperatorAuditMergePercent();
                long total = currentRecords.size();
                // 尚未办理的数量为所有待办数-1，1是当前办理的这条记录
                long todoCount = currentRecords.stream().filter(FlowRecord::isTodo).count() - 1;
                long doneCount = total - todoCount;
                return doneCount >= total * percent;
            }
        }
        return true;
    }


    /**
     * 生成当前节点的记录
     *
     * @param session 触发会话
     * @return 生成当前节点的记录
     */
    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {

        if(this.isWaitParallelRecord(session)){
            return List.of();
        }

        List<FlowRecord> records = new ArrayList<>();
        FlowRecord currentRecord = session.getCurrentRecord();
        OperatorManager operatorManager = this.operators(session);
        List<IFlowOperator> operators = operatorManager.getOperators();
        for (int order = 0; order < operators.size(); order++) {
            IFlowOperator operator = operators.get(order);
            FlowRecord flowRecord = new FlowRecord(session.updateSession(operator), this.id,  order);
            records.add(flowRecord);
        }
        if (operators.size() > 1) {
            StrategyManager strategyManager = this.strategies();
            MultiOperatorAuditStrategy.Type multiOperatorAuditStrategyType = strategyManager.getMultiOperatorAuditStrategyType();
            // 如果是顺序审批，则隐藏掉后续的人员的审批记录
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.SEQUENCE) {
                for (int i = 1; i < records.size(); i++) {
                    FlowRecord record = records.get(i);
                    record.hidden();
                }
            }
            // 如果是随机审批，则隐藏掉后续的人员的审批记录
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.RANDOM_ONE) {
                int index = RandomUtils.randomInt(operators.size());

                List<FlowRecord> newRecords = new ArrayList<>();
                for (FlowRecord record : records) {
                    if (record.getNodeOrder() == index) {
                        record.resetNodeOrder(0);
                        newRecords.add(record);
                    }
                }
                return newRecords;
            }
        }
        return records;
    }

    @Override
    public void verifySession(FlowSession session) {
        super.verifySession(session);
        FlowRecord flowRecord = session.getCurrentRecord();
        Workflow workflow = session.getWorkflow();
        // 数据验证
        FieldPermissionManager fieldPermissionManager = this.formFieldsPermissionsManager();
        fieldPermissionManager.verifyFormData(workflow.getForm(), flowRecord.getFormData(), session.getFormData().toMapData());

        FlowAdvice flowAdvice = session.getAdvice();
        IFlowAction flowAction = flowAdvice.getAction();

        StrategyManager strategyManager = this.strategies();
        // 是否必须填写审批意见
        if (strategyManager.isEnableAdvice()) {
            if (!StringUtils.hasText(flowAdvice.getAdvice())) {
                throw new IllegalArgumentException("advice can not be null");
            }
        }
        //  通过操作
        if (flowAction instanceof PassAction) {
            // 是否必须签名
            if (strategyManager.isEnableSignable()) {
                if (!StringUtils.hasText(flowAdvice.getSignKey())) {
                    throw new IllegalArgumentException("signKey can not be null");
                }
            }
        }
    }

}
