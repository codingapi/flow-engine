package com.codingapi.flow.service;

import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.javscript.NodeViewJavaScript;
import com.codingapi.flow.javscript.NodeViewJavaScriptCacheContext;
import com.codingapi.flow.javscript.NodeViewScriptAnnotationScannerUtils;
import com.codingapi.flow.javscript.NodeViewScriptFieldResult;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import com.codingapi.springboot.script.repository.GroovyScriptRepositoryContext;
import com.codingapi.springboot.script.scanner.GroovyScriptAnnotationScannerUtils;
import com.codingapi.springboot.script.scanner.GroovyScriptFieldResult;
import com.codingapi.springboot.script.temp.TempGroovyScriptContext;

import java.util.List;

public class WorkflowGroovyScriptUtils {


    private static void saveGroovyScripts(Object target) {
        if (target != null) {
            List<String> keys = GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(target).getKeys();
            for (String key : keys) {
                GroovyScript groovyScript = TempGroovyScriptContext.getInstance().getGroovyScript(key);
                if (groovyScript != null) {
                    groovyScript.save();
                }
            }
        }
    }

    private static void saveJavascriptScripts(Object target) {
        if (target != null) {
            List<String> keys = NodeViewScriptAnnotationScannerUtils.findNodeViewScriptFields(target).getKeys();
            for (String key : keys) {
                NodeViewJavaScript javaScript = NodeViewJavaScriptCacheContext.getInstance().get(key);
                if (javaScript != null) {
                    javaScript.save();
                }
            }
        }
    }

    public static void deleteGroovyScripts(Object target) {
        if (target != null) {
            List<String> keys = GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(target).getKeys();
            for (String key : keys) {
                GroovyScriptRepositoryContext.getInstance().delete(key);
            }
        }
    }


    public static void deleteJavaScripts(Object target) {
        if (target != null) {
            List<String> keys = NodeViewScriptAnnotationScannerUtils.findNodeViewScriptFields(target).getKeys();
            for (String key : keys) {
                NodeViewJavaScriptCacheContext.getInstance().delete(key);
            }
        }
    }

    public static void resetGroovyScripts(Object target) {
        if (target != null) {
            GroovyScriptFieldResult result = GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(target);
            result.update((key) -> {
                GroovyScript groovyScript = GroovyScriptCacheContext.getInstance().getGroovyScript(key);
                if (groovyScript != null) {
                    GroovyScript latestScript = groovyScript.copy(FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey());
                    latestScript.save();
                    return latestScript.getKey();
                }
                return key;
            });
        }
    }

    public static void resetJavaScripts(Object target) {
        if (target != null) {
            NodeViewScriptFieldResult result = NodeViewScriptAnnotationScannerUtils.findNodeViewScriptFields(target);
            result.update((key) -> {
                NodeViewJavaScript javaScript = NodeViewJavaScriptCacheContext.getInstance().get(key);
                if (javaScript != null) {
                    NodeViewJavaScript latestScript = javaScript.copy(FlowIDGeneratorGatewayContext.getInstance().generateViewCode());
                    latestScript.save();
                    return latestScript.getCode();
                }
                return key;
            });
        }
    }

    /**
     * 同步保存脚本对象
     *
     * @param target 目标对象
     */
    public static void saveScripts(Object target) {
        saveGroovyScripts(target);
        saveJavascriptScripts(target);
    }

    /**
     * 同步删除脚本数据
     *
     * @param target 目标对象
     */
    public static void deleteScripts(Object target) {
        deleteGroovyScripts(target);
        deleteJavaScripts(target);
    }

    /**
     * 替换脚本数据对象
     *
     * @param target 目标对象
     */
    public static void resetScripts(Object target) {
        resetGroovyScripts(target);
        resetJavaScripts(target);
    }
}
