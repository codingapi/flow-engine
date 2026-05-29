package com.codingapi.example.convertor;

import com.alibaba.fastjson.JSON;
import com.codingapi.example.entity.GroovyScriptEntity;
import com.codingapi.springboot.script.GroovyScript;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class GroovyScriptConvertor {

    public static GroovyScriptEntity convert(GroovyScript groovyScript) {
        if (groovyScript == null) {
            return null;
        }
        GroovyScriptEntity entity = new GroovyScriptEntity();
        entity.setKey(groovyScript.getKey());
        entity.setScript(groovyScript.getScript());
        entity.setDescription(groovyScript.getDescription());
        entity.setMethod(groovyScript.getMethod());
        entity.setReturnType(groovyScript.getReturnType() != null ? groovyScript.getReturnType().getName() : null);
        entity.setBinds(groovyScript.getBinds() != null ? JSON.toJSONString(groovyScript.getBinds()) : null);
        entity.setRequests(groovyScript.getRequests() != null ? JSON.toJSONString(groovyScript.getRequests()) : null);
        entity.setTypeOne(groovyScript.getTypeOne());
        entity.setTypeTwo(groovyScript.getTypeTwo());
        entity.setRemark(groovyScript.getRemark());
        entity.setTag(groovyScript.getTag());
        entity.setCreateTime(groovyScript.getCreateTime());
        entity.setUpdateTime(groovyScript.getUpdateTime());
        return entity;
    }

    @SneakyThrows
    public static Map<String, Class<?>> toClassMap(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        Map<String, Object> value = JSON.parseObject(content, Map.class);
        if (value == null || value.isEmpty()) {
            return null;
        }
        Map<String, Class<?>> data = new HashMap<>();

        for (String key : value.keySet()) {
            Object clazzValue = value.get(key);
            if (clazzValue instanceof String) {
                data.put(key, Class.forName((String) clazzValue));
            }
            if (clazzValue instanceof Class<?>) {
                data.put(key, (Class) clazzValue);
            }
        }

        return data;
    }

    @SneakyThrows
    public static GroovyScript convert(GroovyScriptEntity entity) {
        if (entity == null) {
            return null;
        }
        return new GroovyScript(entity.getKey(),
                entity.getScript(),
                entity.getDescription(),
                entity.getMethod(),
                entity.getReturnType() != null ? Class.forName(entity.getReturnType()) : null,
                toClassMap(entity.getBinds()),
                toClassMap(entity.getRequests()),
                entity.getTypeOne(),
                entity.getTypeTwo(),
                entity.getTag(),
                entity.getRemark(),
                entity.getCreateTime(),
                entity.getUpdateTime());
    }
}
