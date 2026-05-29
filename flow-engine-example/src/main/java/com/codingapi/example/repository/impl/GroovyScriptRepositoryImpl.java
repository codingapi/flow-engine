package com.codingapi.example.repository.impl;

import com.codingapi.example.convertor.GroovyScriptConvertor;
import com.codingapi.example.entity.GroovyScriptEntity;
import com.codingapi.example.repository.GroovyScriptEntityRepository;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.repository.GroovyScriptRepository;
import com.codingapi.springboot.script.repository.GroovyScriptRepositoryContext;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class GroovyScriptRepositoryImpl implements GroovyScriptRepository, InitializingBean {

    private final GroovyScriptEntityRepository groovyScriptEntityRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        GroovyScriptRepositoryContext.getInstance().setGroovyScriptRepository(this);
    }

    @Override
    public void save(GroovyScript groovyScript) {
        GroovyScriptEntity entity = GroovyScriptConvertor.convert(groovyScript);
        if(entity!=null){
            groovyScriptEntityRepository.save(entity);
        }
    }

    @Override
    public void delete(String key) {
        groovyScriptEntityRepository.deleteById(key);
    }

    @Override
    public GroovyScript get(String key) {
        GroovyScriptEntity entity = groovyScriptEntityRepository.getGroovyScriptEntityByKey(key);
        return GroovyScriptConvertor.convert(entity);
    }
}
