package com.codingapi.flow.repository;

import com.codingapi.flow.javscript.NodeViewJavaScript;

public interface NodeViewJavaScriptRepository {

    void save(NodeViewJavaScript javaScript);

    void delete(String code);

    NodeViewJavaScript get(String code);

}
