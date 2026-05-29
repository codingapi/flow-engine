package com.codingapi.example.repository;

import com.codingapi.example.entity.GroovyScriptEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

public interface GroovyScriptEntityRepository extends FastRepository<GroovyScriptEntity,String> {

    GroovyScriptEntity getGroovyScriptEntityByKey(String id);

}
