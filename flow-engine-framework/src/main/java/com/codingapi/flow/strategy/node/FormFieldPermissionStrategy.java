package com.codingapi.flow.strategy.node;

import com.codingapi.flow.builder.NodeMapBuilder;
import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.session.FlowSession;
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
     * 若为空时，代表字段全部可写
     */
    private List<FormFieldPermission> fieldPermissions;


    public FormFieldPermissionStrategy() {
        this.fieldPermissions = new ArrayList<>();
    }

    public FormFieldPermissionStrategy(List<FormFieldPermission> fieldPermissions) {
        this.fieldPermissions = fieldPermissions;
    }

    /**
     * 验证字段权限是否存在
     *
     * @param form 表单元数据
     */
    @Override
    public void verifyNode(FlowForm form) {
        Map<String, DataType> fieldTypes = form.loadAllFieldDataTypeMaps();
        if(fieldPermissions!=null) {
            for (FormFieldPermission permission : fieldPermissions) {
                String key = permission.getFormCode() + "." + permission.getFieldCode();
                if (!fieldTypes.containsKey(key)) {
                    throw FlowValidationException.fieldNotFound(key);
                }
            }
        }
    }

    @Override
    public void verifySession(FlowSession session) {
        FlowForm flowForm = session.getFormData().getFlowForm();
        Map<String, Object> currentData = session.getCurrentRecord().getFormData();
        Map<String, Object> latestData = session.getFormData().toMapData();
        if(fieldPermissions!=null) {
            for (FormFieldPermission permission : fieldPermissions) {
                // 子表
                if (flowForm.isSubForm(permission.getFormCode())) {
                    if (permission.getType() == PermissionType.READ) {
                        List<Map<String, Object>> currentSubFormData = (List<Map<String, Object>>) currentData.get(permission.getFormCode());
                        List<Map<String, Object>> latestSubFormData = (List<Map<String, Object>>) latestData.get(permission.getFormCode());
                        if (currentSubFormData == null || latestSubFormData == null) {
                            throw FlowValidationException.nodeRequired("form");
                        }

                        if (currentSubFormData.size() != latestSubFormData.size()) {
                            throw FlowValidationException.nodeRequired("form");
                        }

                        for (int i = 0; i < currentSubFormData.size(); i++) {
                            Map<String, Object> currentSubFormItem = currentSubFormData.get(i);
                            Map<String, Object> latestSubFormItem = latestSubFormData.get(i);
                            Object currentValue = currentSubFormItem.get(permission.getFieldCode());
                            Object latestValue = latestSubFormItem.get(permission.getFieldCode());
                            if (!currentValue.equals(latestValue)) {
                                throw FlowValidationException.fieldReadOnly(permission.getFieldCode());
                            }
                        }
                    }
                } else {
                    // 在只读权限下不允许修改数据
                    if (permission.getType() == PermissionType.READ) {
                        Object currentValue = currentData.get(permission.getFieldCode());
                        Object latestValue = latestData.get(permission.getFieldCode());
                        if (!currentValue.equals(latestValue)) {
                            throw FlowValidationException.fieldReadOnly(permission.getFieldCode());
                        }
                    }
                }
            }
        }
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
