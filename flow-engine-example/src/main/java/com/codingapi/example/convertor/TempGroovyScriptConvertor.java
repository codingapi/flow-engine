package com.codingapi.example.convertor;

import com.alibaba.fastjson.JSON;
import com.codingapi.example.entity.TempGroovyScriptEntity;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.temp.TempGroovyScript;
import lombok.SneakyThrows;

public class TempGroovyScriptConvertor {

    public static TempGroovyScriptEntity convert(TempGroovyScript tempGroovyScript) {
        if (tempGroovyScript == null) {
            return null;
        }
        GroovyScript groovyScript = tempGroovyScript.getGroovyScript();
        TempGroovyScriptEntity entity = new TempGroovyScriptEntity();
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
        entity.setClearTime(tempGroovyScript.getClearTime());
        return entity;
    }


    @SneakyThrows
    public static TempGroovyScript convert(TempGroovyScriptEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TempGroovyScript(new GroovyScript(entity.getKey(),
                entity.getScript(),
                entity.getDescription(),
                entity.getMethod(),
                entity.getReturnType() != null ? Class.forName(entity.getReturnType()) : null,
                GroovyScriptConvertor.toClassMap(entity.getBinds()),
                GroovyScriptConvertor.toClassMap(entity.getRequests()),
                entity.getTypeOne(),
                entity.getTypeTwo(),
                entity.getTag(),
                entity.getRemark(),
                entity.getCreateTime(),
                entity.getUpdateTime()),entity.getClearTime());
    }
}
