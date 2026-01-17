package com.codingapi.flow.action.factory;

import com.codingapi.flow.action.*;
import com.codingapi.flow.node.*;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class FlowActionFactory {

    @Getter
    private final static FlowActionFactory instance = new FlowActionFactory();

    private final Map<String, Class<? extends IFlowAction>> nodesClasses = new HashMap<>();

    private FlowActionFactory() {
        this.initNodes();
    }

    private void initNodes() {
        nodesClasses.put(ActionType.REJECT.name(), RejectAction.class);
        nodesClasses.put(ActionType.ADD_AUDIT.name(), AddAuditAction.class);
        nodesClasses.put(ActionType.PASS.name(), PassAction.class);
        nodesClasses.put(ActionType.DEFAULT.name(), DefaultAction.class);
        nodesClasses.put(ActionType.TRANSFER.name(), TransferAction.class);
        nodesClasses.put(ActionType.RETURN.name(), ReturnAction.class);
        nodesClasses.put(ActionType.DELEGATE.name(), DelegateAction.class);
        nodesClasses.put(ActionType.CUSTOM.name(), CustomAction.class);
    }


    @SneakyThrows
    public IFlowAction createAction(Map<String, Object> map) {
        String type = (String) map.get("type");
        Class<? extends IFlowAction> clazz = nodesClasses.get(type);
        if (clazz != null) {
            Method formMap = clazz.getMethod("formMap", Map.class);
            return (IFlowAction) formMap.invoke(null, map);
        }
        return null;
    }
}
