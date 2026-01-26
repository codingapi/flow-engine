package com.codingapi.flow.infra.entity.convert;

import com.alibaba.fastjson.JSON;
import com.codingapi.flow.form.FormMeta;
import jakarta.persistence.AttributeConverter;
import org.springframework.util.StringUtils;

public class FormMetaConvertor implements AttributeConverter<FormMeta, String> {

    @Override
    public String convertToDatabaseColumn(FormMeta attribute) {
        if (attribute == null) {
            return null;
        }
        return JSON.toJSONString(attribute.toMap());
    }

    @Override
    public FormMeta convertToEntityAttribute(String dbData) {
        if(!StringUtils.hasText(dbData)){
            return null;
        }
        return FormMeta.fromMap(JSON.parseObject(dbData));
    }
}
