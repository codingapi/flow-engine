package com.codingapi.flow.strategy;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.builder.NodeMapBuilder;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class FormFieldPermissionStrategy extends BaseStrategy  {

    /**
     * 表单字段权限
     */
    private List<FormFieldPermission> fieldPermissions;


    public FormFieldPermissionStrategy() {
        super(RandomUtils.generateStringId());
        this.fieldPermissions = new ArrayList<>();
    }

    public FormFieldPermissionStrategy(List<FormFieldPermission> fieldPermissions) {
        super(RandomUtils.generateStringId());
        this.fieldPermissions = fieldPermissions;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("fieldPermissions", fieldPermissions.stream().map(FormFieldPermission::toMap).toList());
        return map;
    }

    public static FormFieldPermissionStrategy fromMap(Map<String, Object> map) {
        FormFieldPermissionStrategy strategy = BaseStrategy.fromMap(map, FormFieldPermissionStrategy.class);
        if (strategy == null) return null;
        strategy.setFieldPermissions(NodeMapBuilder.loadFormFieldPermissions(map));
        return strategy;
    }

    public static ErrorTriggerStrategy defaultStrategy() {
        ErrorTriggerStrategy strategy = new ErrorTriggerStrategy();
        strategy.setErrorTriggerScript(ErrorTriggerScript.SCRIPT_NODE_DEFAULT);
        return strategy;
    }


}
