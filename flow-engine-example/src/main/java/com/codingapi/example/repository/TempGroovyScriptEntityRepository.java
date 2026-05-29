package com.codingapi.example.repository;

import com.codingapi.example.entity.TempGroovyScriptEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

public interface TempGroovyScriptEntityRepository extends FastRepository<TempGroovyScriptEntity,String> {


    TempGroovyScriptEntity getTempGroovyScriptEntityByKey(String key);

}
