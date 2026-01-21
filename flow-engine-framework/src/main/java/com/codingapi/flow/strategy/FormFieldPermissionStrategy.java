package com.codingapi.flow.strategy;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.builder.NodeMapBuilder;
import com.codingapi.flow.node.manager.FieldPermissionManager;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class FormFieldPermissionStrategy implements INodeStrategy {

    /**
     * 表单字段权限
     */
    @Setter
    @Getter
    private List<FormFieldPermission> formFieldPermissions;


    public FormFieldPermissionStrategy() {
        this.formFieldPermissions = new ArrayList<>();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", strategyType());
        map.put("fieldPermissions", formFieldPermissions.stream().map(FormFieldPermission::toMap).toList());
        return map;
    }

    public static FormFieldPermissionStrategy fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        FormFieldPermissionStrategy strategy = new FormFieldPermissionStrategy();
        strategy.setFormFieldPermissions(NodeMapBuilder.loadFormFieldPermissions(map));
        return strategy;
    }


    public FieldPermissionManager fieldPermissionManager() {
        return new FieldPermissionManager(formFieldPermissions);
    }


    public static ErrorTriggerStrategy defaultStrategy() {
        ErrorTriggerStrategy strategy = new ErrorTriggerStrategy();
        strategy.setErrorTriggerScript(ErrorTriggerScript.SCRIPT_NODE_DEFAULT);
        return strategy;
    }


}
