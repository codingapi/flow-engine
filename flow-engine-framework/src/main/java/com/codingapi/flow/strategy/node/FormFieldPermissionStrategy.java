package com.codingapi.flow.strategy.node;

import com.codingapi.flow.builder.NodeMapBuilder;
import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
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
        if (fieldPermissions != null) {
            for (FormFieldPermission permission : fieldPermissions) {
                String key = permission.getFormCode() + "." + permission.getFieldCode();
                if (!fieldTypes.containsKey(key)) {
                    throw FlowValidationException.fieldNotFound(key);
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


    private FormFieldPermission getFormFieldPermission(String formCode,String fieldCode){
        if(this.fieldPermissions!=null) {
            for (FormFieldPermission fieldPermission:this.fieldPermissions){
                if(fieldPermission.isField(formCode,fieldCode)){
                    return fieldPermission;
                }
            }
        }
        return null;
    }

    public void filterPermissions(FlowForm meta) {
        Map<String,DataType> fieldMap =  meta.loadAllFieldDataTypeMaps();

        if (this.fieldPermissions == null || this.getFieldPermissions().isEmpty()) {
            // init fieldPermissions
            this.fieldPermissions = new ArrayList<>();
            for (String formField:fieldMap.keySet()){
                String formCode = formField.split("\\.")[0];
                String fieldCode = formField.split("\\.")[1];

                FormFieldPermission fieldPermission = new FormFieldPermission();
                fieldPermission.setFormCode(formCode);
                fieldPermission.setFieldCode(fieldCode);
                fieldPermission.setType(PermissionType.WRITE);

                this.fieldPermissions.add(fieldPermission);
            }

        } else {
            // filter fieldPermissions
            List<FormFieldPermission> fieldPermissions = new ArrayList<>();
            for (String formField:fieldMap.keySet()){
                String formCode = formField.split("\\.")[0];
                String fieldCode = formField.split("\\.")[1];

                FormFieldPermission fieldPermission = this.getFormFieldPermission(formCode,fieldCode);
                if(fieldPermission==null) {
                    fieldPermission = new FormFieldPermission();
                    fieldPermission.setFormCode(formCode);
                    fieldPermission.setFieldCode(fieldCode);
                    fieldPermission.setType(PermissionType.WRITE);
                }
                fieldPermissions.add(fieldPermission);
            }

            this.fieldPermissions = fieldPermissions;
        }

    }
}
