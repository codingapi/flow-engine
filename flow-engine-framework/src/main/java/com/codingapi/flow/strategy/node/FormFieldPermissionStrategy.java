package com.codingapi.flow.strategy.node;

import com.codingapi.flow.builder.NodeMapBuilder;
import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.form.permission.FormFieldPermission;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表单字段权限策略配置
 */
@Setter
@Getter
public class FormFieldPermissionStrategy extends BaseStrategy {

    /**
     * 表单字段权限
     */
    private List<FormFieldPermission> fieldPermissions;


    public FormFieldPermissionStrategy() {
        this.fieldPermissions = new ArrayList<>();
    }

    public FormFieldPermissionStrategy(List<FormFieldPermission> fieldPermissions) {
        this.fieldPermissions = fieldPermissions;
    }

    @Override
    public void copy(INodeStrategy target) {
        this.fieldPermissions = ((FormFieldPermissionStrategy) target).fieldPermissions;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("fieldPermissions", fieldPermissions.stream().map(FormFieldPermission::toMap).toList());
        return map;
    }

    public static FormFieldPermissionStrategy fromMap(Map<String, Object> map) {
        FormFieldPermissionStrategy strategy = IMapConvertor.fromMap(map, FormFieldPermissionStrategy.class);
        if (strategy == null) return null;
        strategy.setFieldPermissions(NodeMapBuilder.loadFormFieldPermissions(map));
        return strategy;
    }

    public static FormFieldPermissionStrategy defaultStrategy() {
        return new FormFieldPermissionStrategy();
    }


}
