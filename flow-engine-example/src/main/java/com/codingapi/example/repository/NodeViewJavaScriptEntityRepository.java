package com.codingapi.example.repository;

import com.codingapi.example.entity.NodeViewJavaScriptEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

public interface NodeViewJavaScriptEntityRepository extends FastRepository<NodeViewJavaScriptEntity,String> {

    NodeViewJavaScriptEntity getNodeViewJavaScriptEntityByCode(String code);
}
