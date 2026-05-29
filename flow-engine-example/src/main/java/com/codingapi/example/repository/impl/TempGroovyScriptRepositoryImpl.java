package com.codingapi.example.repository.impl;

import com.codingapi.example.convertor.TempGroovyScriptConvertor;
import com.codingapi.example.entity.TempGroovyScriptEntity;
import com.codingapi.example.repository.TempGroovyScriptEntityRepository;
import com.codingapi.springboot.script.repository.TempGroovyScriptRepository;
import com.codingapi.springboot.script.repository.TempGroovyScriptRepositoryContext;
import com.codingapi.springboot.script.temp.TempGroovyScript;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TempGroovyScriptRepositoryImpl implements TempGroovyScriptRepository, InitializingBean {

    private final TempGroovyScriptEntityRepository groovyScriptEntityRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        TempGroovyScriptRepositoryContext.getInstance().setTempGroovyScriptRepository(this);
    }


    @Override
    public TempGroovyScript get(String key) {
        return TempGroovyScriptConvertor.convert(groovyScriptEntityRepository.getTempGroovyScriptEntityByKey(key));
    }

    @Override
    public void save(TempGroovyScript tempGroovyScript) {
        TempGroovyScriptEntity entity = TempGroovyScriptConvertor.convert(tempGroovyScript);
        if(entity!=null){
            groovyScriptEntityRepository.save(entity);
        }
    }


    @Override
    public void delete(String key) {
        groovyScriptEntityRepository.deleteById(key);
    }

    @Override
    public Page<TempGroovyScript> find(PageRequest request) {
        return groovyScriptEntityRepository.findAll(request).map(TempGroovyScriptConvertor::convert);
    }
}
