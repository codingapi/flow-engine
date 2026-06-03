package com.codingapi.example.repository.impl;

import com.codingapi.example.convertor.NodeViewJavaScriptConvertor;
import com.codingapi.example.repository.NodeViewJavaScriptEntityRepository;
import com.codingapi.flow.javscript.NodeViewJavaScript;
import com.codingapi.flow.repository.NodeViewJavaScriptRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class NodeViewJavaScriptRepositoryImpl implements NodeViewJavaScriptRepository {

    private final NodeViewJavaScriptEntityRepository nodeViewJavaScriptEntityRepository;

    @Override
    public void save(NodeViewJavaScript javaScript) {
        nodeViewJavaScriptEntityRepository.save(NodeViewJavaScriptConvertor.convert(javaScript));
    }

    @Override
    public void delete(String code) {
        nodeViewJavaScriptEntityRepository.deleteById(code);
    }

    @Override
    public NodeViewJavaScript get(String code) {
        return NodeViewJavaScriptConvertor.convert(nodeViewJavaScriptEntityRepository.getNodeViewJavaScriptEntityByCode(code));
    }
}
