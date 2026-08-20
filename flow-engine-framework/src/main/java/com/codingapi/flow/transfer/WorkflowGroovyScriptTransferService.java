package com.codingapi.flow.transfer;

import com.codingapi.flow.exception.WorkflowTransferException;
import com.codingapi.flow.script.action.ActionCustomScript;
import com.codingapi.flow.script.action.ActionDisplayScript;
import com.codingapi.flow.script.action.ActionRejectScript;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.script.node.ConditionScript;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.script.node.OperatorMatchScript;
import com.codingapi.flow.script.node.RouterNodeScript;
import com.codingapi.flow.script.node.SubProcessResultScript;
import com.codingapi.flow.script.node.SubProcessScript;
import com.codingapi.flow.script.node.TriggerScript;
import com.codingapi.springboot.framework.reflect.ObjectAnnotationFieldUtils;
import com.codingapi.springboot.framework.reflect.pojo.AnnotationTargetFieldResult;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 与Schema版本无关的Groovy脚本收集、重建和引用替换能力。
 */
public class WorkflowGroovyScriptTransferService {

    /**
     * 收集目标对象引用的全部Groovy脚本正文。
     */
    public Map<String, String> exportScripts(Collection<?> targets) {
        Map<String, String> scripts = new LinkedHashMap<>();
        for (Object target : targets) {
            if (target == null) {
                continue;
            }
            AnnotationTargetFieldResult<String> fields = findFields(target);
            fields.fetch(field -> {
                String key = field.getValue();
                if (scripts.containsKey(key)) {
                    return;
                }
                GroovyScript groovyScript = GroovyScriptCacheContext.getInstance().getGroovyScript(key);
                if (groovyScript == null || groovyScript.getScript() == null) {
                    throw WorkflowTransferException.scriptNotFound(key);
                }
                scripts.put(key, groovyScript.getScript());
            });
        }
        return scripts;
    }

    /**
     * 按脚本引用所属类型重建脚本，并将所有旧key替换为新key。
     *
     * @return 旧key到新key的映射
     */
    public Map<String, String> rebuildScripts(Collection<?> targets, Map<String, String> scriptContents) {
        Map<String, ScriptType> scriptTypes = new LinkedHashMap<>();
        List<AnnotationTargetFieldResult<String>> targetFields = new ArrayList<>();
        for (Object target : targets) {
            if (target == null) {
                continue;
            }
            AnnotationTargetFieldResult<String> fields = findFields(target);
            targetFields.add(fields);
            fields.fetch(field -> {
                String key = field.getValue();
                ScriptType currentType = ScriptType.from(field.getTarget());
                ScriptType previousType = scriptTypes.putIfAbsent(key, currentType);
                if (previousType != null && previousType != currentType) {
                    throw WorkflowTransferException.invalidSchema(
                            String.format("Script key %s is referenced by both %s and %s",
                                    key, previousType, currentType));
                }
            });
        }

        Set<String> unreferencedScriptKeys = new LinkedHashSet<>(scriptContents.keySet());
        unreferencedScriptKeys.removeAll(scriptTypes.keySet());
        if (!unreferencedScriptKeys.isEmpty()) {
            throw WorkflowTransferException.invalidSchema(
                    String.format("groovyScripts contains unreferenced keys: %s", unreferencedScriptKeys));
        }

        Map<String, GroovyScript> rebuiltScripts = new LinkedHashMap<>();
        Map<String, String> keyMapping = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, ScriptType> entry : scriptTypes.entrySet()) {
                String oldKey = entry.getKey();
                String content = resolveScriptContent(oldKey, scriptContents);
                GroovyScript rebuiltScript = entry.getValue().create(content);
                rebuiltScripts.put(oldKey, rebuiltScript);
                keyMapping.put(oldKey, rebuiltScript.getKey());
            }

            for (AnnotationTargetFieldResult<String> fields : targetFields) {
                fields.update(oldKey -> keyMapping.getOrDefault(oldKey, oldKey));
            }
            rebuiltScripts.values().forEach(GroovyScript::save);
            return keyMapping;
        } catch (RuntimeException exception) {
            rebuiltScripts.values().forEach(GroovyScript::remove);
            throw exception;
        }
    }

    private String resolveScriptContent(String key, Map<String, String> scriptContents) {
        String content = scriptContents.get(key);
        if (content != null) {
            return content;
        }

        // 兼容历史文件：旧文件可能将脚本正文直接放在脚本字段中。
        if (looksLikeInlineScript(key)) {
            return key;
        }

        // 兼容同环境导入的旧key-only文件；跨环境缺少正文时必须明确失败。
        GroovyScript existing = GroovyScriptCacheContext.getInstance().getGroovyScript(key);
        if (existing != null && existing.getScript() != null) {
            return existing.getScript();
        }
        throw WorkflowTransferException.scriptNotFound(key);
    }

    private boolean looksLikeInlineScript(String value) {
        return value != null && (value.contains("def run") || value.contains("\n") || value.contains("\r"));
    }

    private AnnotationTargetFieldResult<String> findFields(Object target) {
        return ObjectAnnotationFieldUtils.findFieldAnnotationValue(
                target,
                com.codingapi.springboot.script.annotation.GroovyScript.class,
                String.class);
    }

    private enum ScriptType {
        ROUTER {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createRouterScript(content);
            }
        },
        NODE_TITLE {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createNodeTitleScript(content);
            }
        },
        CONDITION {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createConditionScript(content);
            }
        },
        TRIGGER {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createTriggerScript(content);
            }
        },
        SUB_PROCESS {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createSubProcessScript(content);
            }
        },
        SUB_PROCESS_RESULT {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createSubProcessResultScript(content);
            }
        },
        OPERATOR_LOAD {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createOperatorLoadScript(content);
            }
        },
        OPERATOR_MATCH {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createOperatorMatchScript(content);
            }
        },
        ERROR_TRIGGER {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createErrorTriggerScript(content);
            }
        },
        ACTION_DISPLAY {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createActionDisplayScript(content);
            }
        },
        ACTION_CUSTOM {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createActionCustomScript(content);
            }
        },
        ACTION_REJECT {
            @Override
            GroovyScript create(String content) {
                return FlowGroovyScriptFactory.createActionRejectScript(content);
            }
        };

        abstract GroovyScript create(String content);

        static ScriptType from(Object target) {
            if (target instanceof RouterNodeScript) return ROUTER;
            if (target instanceof NodeTitleScript) return NODE_TITLE;
            if (target instanceof ConditionScript) return CONDITION;
            if (target instanceof TriggerScript) return TRIGGER;
            if (target instanceof SubProcessScript) return SUB_PROCESS;
            if (target instanceof SubProcessResultScript) return SUB_PROCESS_RESULT;
            if (target instanceof OperatorLoadScript) return OPERATOR_LOAD;
            if (target instanceof OperatorMatchScript) return OPERATOR_MATCH;
            if (target instanceof ErrorTriggerScript) return ERROR_TRIGGER;
            if (target instanceof ActionDisplayScript) return ACTION_DISPLAY;
            if (target instanceof ActionCustomScript) return ACTION_CUSTOM;
            if (target instanceof ActionRejectScript) return ACTION_REJECT;
            throw WorkflowTransferException.scriptTypeUnsupported(target.getClass().getName());
        }
    }
}
