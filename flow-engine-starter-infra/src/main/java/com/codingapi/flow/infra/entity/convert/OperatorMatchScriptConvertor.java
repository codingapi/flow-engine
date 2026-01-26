package com.codingapi.flow.infra.entity.convert;

import com.codingapi.flow.script.node.OperatorMatchScript;
import jakarta.persistence.AttributeConverter;
import org.springframework.util.StringUtils;

public class OperatorMatchScriptConvertor implements AttributeConverter<OperatorMatchScript,String> {

    @Override
    public String convertToDatabaseColumn(OperatorMatchScript script) {
        if (script == null) {
            return null;
        }
        return script.getScript();
    }

    @Override
    public OperatorMatchScript convertToEntityAttribute(String dbData) {
        if(!StringUtils.hasText(dbData)){
            return null;
        }
        return new OperatorMatchScript(dbData);
    }
}
