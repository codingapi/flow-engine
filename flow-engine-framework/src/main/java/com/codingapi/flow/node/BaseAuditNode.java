package com.codingapi.flow.node;

import com.codingapi.flow.action.*;
import com.codingapi.flow.action.factory.FlowActionFactory;
import com.codingapi.flow.context.RepositoryContext;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordFinishEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.fixed.EndNode;
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
import com.codingapi.flow.strategy.NodeStrategyFactory;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.springboot.framework.event.EventPusher;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseAuditNode extends BaseFlowNode implements IFlowNode {

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
     * 节点操作
     */
    @Setter
    private List<IFlowAction> actions;

    /**
     * 节点策略
     */
    @Setter
    private List<INodeStrategy> nodeStrategies;


    public BaseAuditNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldPermissions, List<IFlowAction> actions, List<INodeStrategy> nodeStrategies) {
        super(id, name);
        this.view = view;
        this.operatorScript = operatorScript;
        this.nodeTitleScript = nodeTitleScript;
        this.errorTriggerScript = errorTriggerScript;
        this.formFieldPermissions = formFieldPermissions;
        this.actions = actions;
        this.nodeStrategies = nodeStrategies;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("view", view);
        map.put("name", name);
        map.put("id", id);
        map.put("operatorScript", operatorScript.getScript());
        map.put("nodeTitleScript", nodeTitleScript.getScript());
        map.put("errorTriggerScript", errorTriggerScript.getScript());
        map.put("formFieldPermissions", formFieldPermissions);
        map.put("type", getType());
        map.put("actions", actions.stream().map(IFlowAction::toMap).toList());
        map.put("nodeStrategies", nodeStrategies.stream().map(INodeStrategy::toMap).toList());
        return map;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T extends BaseAuditNode> T formMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        node.setView((String) map.get("view"));
        node.setOperatorScript((String) map.get("operatorScript"));
        node.setNodeTitleScript((String) map.get("nodeTitleScript"));
        node.setErrorTriggerScript((String) map.get("errorTriggerScript"));
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) map.get("formFieldsPermissions");
        if (permissions != null) {
            List<FormFieldPermission> permissionList = new ArrayList<>();
            for (Map<String, Object> item : permissions) {
                FormFieldPermission permission = new FormFieldPermission();
                permission.setFormCode((String) item.get("formCode"));
                permission.setFieldName((String) item.get("fieldName"));
                permission.setType(PermissionType.valueOf((String) item.get("type")));
                permissionList.add(permission);
            }
            node.setFormFieldPermissions(permissionList);
        }

        List<Map<String, Object>> actions = (List<Map<String, Object>>) map.get("actions");
        if (actions != null) {
            List<IFlowAction> actionList = new ArrayList<>();
            for (Map<String, Object> item : actions) {
                IFlowAction action = FlowActionFactory.getInstance().createAction(item);
                actionList.add(action);
            }
            node.setActions(actionList);
        }

        List<Map<String, Object>> nodeStrategies = (List<Map<String, Object>>) map.get("nodeStrategies");
        if (nodeStrategies != null) {
            List<INodeStrategy> strategyList = new ArrayList<>();
            for (Map<String, Object> item : nodeStrategies) {
                INodeStrategy strategy = NodeStrategyFactory.getInstance().createStrategy(item);
                strategyList.add(strategy);
            }
            node.setNodeStrategies(strategyList);
        }

        return node;
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


    public FieldPermissionManager formFieldsPermissions() {
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
        if(this.actions == null){
            this.actions = new ArrayList<>();
        }
        this.actions.add(action);
    }

    public void verifyNode(FormMeta form) {
        this.verifyFields();
        if (!(this instanceof EndNode)) {
            FieldPermissionManager fieldPermissionManager = this.formFieldsPermissions();
            fieldPermissionManager.verifyPermissions(form);
        }
    }

    public StrategyManager strategies() {
        return new StrategyManager(nodeStrategies);
    }


    @Override
    public boolean trigger(FlowSession session){
        List<IFlowEvent> flowEvents = new ArrayList<>();
        FlowRecord flowRecord = session.getCurrentRecord();
        IFlowAction flowAction = session.getCurrentAction();
        List<FlowRecord> currentRecords = session.getCurrentNodeRecords();

        // 判断当前节点是否已经完成
        boolean done = this.isDone(session);
        if (done) {
            List<FlowRecord> records = flowAction.generateRecords(session);
            if (!records.isEmpty()) {
                for (FlowRecord record : records) {
                    if (record.isShow()) {
                        flowEvents.add(new FlowRecordTodoEvent(record));
                    }
                }
            }
            flowRecord.update(session.getFormData().toMapData(), session.getAdvice().getAdvice(), session.getAdvice().getSignKey(), true);
            // 判断是否结束
            if (records.size() == 1) {
                FlowRecord record = records.get(0);
                if (record.isNodeType(EndNode.NODE_TYPE)) {
                    boolean flowFinish = flowAction instanceof PassAction;
                    // 添加当前节点到记录中
                    records.add(flowRecord);
                    // 添加历史记录到记录中
                    List<FlowRecord> historyRecords = RepositoryContext.getInstance().findRecordsByProcessId(flowRecord.getProcessId());
                    records.addAll(historyRecords);
                    // 设置状态为完成
                    records.forEach(item -> {
                        item.finish(flowFinish);
                    });

                    // 流程是否正常结束
                    if (flowFinish) {
                        flowEvents.add(new FlowRecordFinishEvent(record));
                    }
                }
                // 添加流程结束事件
                flowEvents.add(new FlowRecordDoneEvent(record));
            }
            RepositoryContext.getInstance().saveRecords(records);
        } else {
            // 判断是否为串行多操作者
            if (this.strategies().isSequenceMultiOperator()) {
                int nextRecordNodeOrder = flowRecord.getNodeOrder() + 1;
                FlowRecord nextRecord = currentRecords.stream().filter(record -> record.getNodeOrder() == nextRecordNodeOrder).findFirst().orElse(null);
                if (nextRecord != null) {
                    // 展示下一个审批人的待办
                    nextRecord.show();
                    flowEvents.add(new FlowRecordTodoEvent(nextRecord));
                    RepositoryContext.getInstance().saveRecord(nextRecord);
                }
            }
            flowRecord.update(session.getFormData().toMapData(), session.getAdvice().getAdvice(), session.getAdvice().getSignKey(), false);
            RepositoryContext.getInstance().saveRecord(flowRecord);
        }

        // 推送待办事件
        for (IFlowEvent event : flowEvents) {
            EventPusher.push(event);
        }

        return false;
    }


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
     * 生成下一节点的记录
     *
     * @param session 触发会话
     * @return 下一节节点的记录
     */
    @Override
    public List<FlowRecord> generateNextRecords(FlowSession session) {
        List<FlowRecord> records = new ArrayList<>();
        FlowRecord currentRecord = session.getCurrentRecord();
        OperatorManager operatorManager = this.operators(session);
        List<IFlowOperator> operators = operatorManager.getOperators();
        for (int order = 0; order < operators.size(); order++) {
            IFlowOperator operator = operators.get(order);
            FlowRecord flowRecord = new FlowRecord(session.updateSession(operator), this.id, currentRecord.getProcessId(), currentRecord.getId(), order);
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
        FlowRecord flowRecord = session.getCurrentRecord();
        Workflow workflow = session.getWorkflow();
        // 数据验证
        FieldPermissionManager fieldPermissionManager = this.formFieldsPermissions();
        fieldPermissionManager.verifyFormData(workflow.getForm(),flowRecord.getFormData(),session.getFormData().toMapData());

        // 节点请求验证
        this.verifyFlowAdvice(session.getAdvice());
    }

    public void verifyFlowAdvice(FlowAdvice flowAdvice) {
        StrategyManager strategyManager = this.strategies();
        IFlowAction flowAction = flowAdvice.getAction();

        // 保存操作,不做检查
        if(flowAction instanceof SaveAction){
            return;
        }

        // 转办操作
        if(flowAction instanceof TransferAction){
            if(flowAdvice.getTransferOperators()==null || flowAdvice.getTransferOperators().isEmpty()){
                throw new IllegalArgumentException("transferOperators can not be null");
            }
        }

        // 退回操作
        if(flowAction instanceof ReturnAction){
            if(flowAdvice.getBackNode()==null ){
                throw new IllegalArgumentException("backNode can not be null");
            }
        }

        // 是否必须填写审批意见
        if(strategyManager.isEnableAdvice()){
            if(!StringUtils.hasText(flowAdvice.getAdvice())){
                throw new IllegalArgumentException("advice can not be null");
            }
        }
        //  通过操作
        if(flowAction instanceof PassAction) {
            // 是否必须签名
            if (strategyManager.isEnableSignable()) {
                if (!StringUtils.hasText(flowAdvice.getSignKey())) {
                    throw new IllegalArgumentException("signKey can not be null");
                }
            }
        }
    }

    private void verifyFields() {
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
    }
}
