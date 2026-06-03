package com.codingapi.flow.javscript;

import com.codingapi.flow.javscript.annotation.NodeViewScript;
import com.codingapi.springboot.framework.reflect.ObjectAnnotationFieldUtils;

public class NodeViewScriptAnnotationScannerUtils {

    public static NodeViewScriptFieldResult findNodeViewScriptFields(Object target) {
        return new NodeViewScriptFieldResult(ObjectAnnotationFieldUtils.findFieldAnnotationValue(target, NodeViewScript.class, String.class));
    }

}
