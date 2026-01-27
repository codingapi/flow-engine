package com.codingapi.flow.infra.entity.convert;

import com.alibaba.fastjson.JSON;
import jakarta.persistence.AttributeConverter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class MapConvertor implements AttributeConverter<Map<String,Object>,String> {

    @Override
    public String convertToDatabaseColumn(Map<String, Object> map) {
        if(map==null || map.isEmpty()){
            return null;
        }
        return JSON.toJSONString(map);
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if(!StringUtils.hasText(dbData)){
            return new HashMap<>();
        }
        return JSON.parseObject(dbData);
    }
}
