package com.codingapi.example.convertor;

import com.codingapi.example.entity.NodeViewJavaScriptEntity;
import com.codingapi.flow.javscript.NodeViewJavaScript;

public class NodeViewJavaScriptConvertor {

    public static NodeViewJavaScript convert(NodeViewJavaScriptEntity entity){
        if(entity==null){
            return null;
        }

        return new NodeViewJavaScript(entity.getCode(),entity.getScript(),entity.getCreateTime(),entity.getUpdateTime());
    }


    public static NodeViewJavaScriptEntity convert(NodeViewJavaScript javaScript){
        if(javaScript==null){
            return null;
        }

        NodeViewJavaScriptEntity entity = new NodeViewJavaScriptEntity();
        entity.setCode(javaScript.getCode());
        entity.setScript(javaScript.getScript());
        entity.setCreateTime(javaScript.getCreateTime());
        entity.setUpdateTime(javaScript.getUpdateTime());
        return entity;
    }
}
