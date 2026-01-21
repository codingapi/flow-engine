package com.codingapi.flow.action.factory;

import com.codingapi.flow.action.*;
import com.codingapi.flow.action.actions.*;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class FlowActionFactory {

    @Getter
    private final static FlowActionFactory instance = new FlowActionFactory();

    private final Map<String, Class<? extends IFlowAction>> actionClasses = new HashMap<>();

    private FlowActionFactory() {
        this.initNodes();
    }

    private void initNodes() {
        actionClasses.put(ActionType.REJECT.name(), RejectAction.class);
        actionClasses.put(ActionType.ADD_AUDIT.name(), AddAuditAction.class);
        actionClasses.put(ActionType.PASS.name(), PassAction.class);
        actionClasses.put(ActionType.DEFAULT.name(), DefaultAction.class);
        actionClasses.put(ActionType.TRANSFER.name(), TransferAction.class);
        actionClasses.put(ActionType.RETURN.name(), ReturnAction.class);
        actionClasses.put(ActionType.DELEGATE.name(), DelegateAction.class);
        actionClasses.put(ActionType.CUSTOM.name(), CustomAction.class);
        actionClasses.put(ActionType.SAVE.name(), SaveAction.class);
    }


    @SneakyThrows
    public IFlowAction createAction(Map<String, Object> map) {
        String type = (String) map.get("type");
        Class<? extends IFlowAction> clazz = actionClasses.get(type);
        if (clazz != null) {
            Method fromMap = clazz.getMethod("fromMap", Map.class);
            return (IFlowAction) fromMap.invoke(null, map);
        }
        return null;
    }
}
