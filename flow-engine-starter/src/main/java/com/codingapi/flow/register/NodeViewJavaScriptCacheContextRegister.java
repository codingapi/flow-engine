package com.codingapi.flow.register;

import com.codingapi.flow.javscript.NodeViewJavaScriptCacheContext;
import com.codingapi.flow.repository.NodeViewJavaScriptRepository;

public class NodeViewJavaScriptCacheContextRegister {

    public NodeViewJavaScriptCacheContextRegister(NodeViewJavaScriptRepository nodeViewJavaScriptRepository) {
        NodeViewJavaScriptCacheContext.getInstance().setNodeViewJavaScriptRepository(nodeViewJavaScriptRepository);
    }
}
