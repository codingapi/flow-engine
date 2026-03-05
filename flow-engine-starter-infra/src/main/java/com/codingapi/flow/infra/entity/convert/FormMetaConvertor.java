package com.codingapi.flow.infra.entity.convert;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.form.FlowForm;
import jakarta.persistence.AttributeConverter;
import org.springframework.util.StringUtils;

public class FormMetaConvertor implements AttributeConverter<FlowForm, String> {

    @Override
    public String convertToDatabaseColumn(FlowForm attribute) {
        if (attribute == null) {
            return null;
        }
        return JSON.toJSONString(attribute.toMap());
    }

    @Override
    public FlowForm convertToEntityAttribute(String dbData) {
        if(!StringUtils.hasText(dbData)){
            return null;
        }
        return FlowForm.fromMap(JSON.parseObject(dbData));
    }
}
