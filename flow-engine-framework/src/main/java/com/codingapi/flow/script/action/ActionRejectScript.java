package com.codingapi.flow.script.action;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 拒绝脚本
 * 拒绝，拒绝时需要根据拒绝的配置流程来设置,退回指定节点、终止流程
 */
@AllArgsConstructor
public class ActionRejectScript {

    public static final String TYPE_TERMINATE = "TERMINATE";

    @Getter
    @GroovyScript
    private final String script;

    public RejectResult execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        String result = GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
        return new RejectResult(result);
    }

    /**
     * 退回至发起节点
     */
    public static ActionRejectScript defaultScript() {
        return new ActionRejectScript(ScriptRegistryContext.getInstance().getActionRejectScript());
    }


    public enum RejectType {
        // 退回指定节点
        RETURN_NODE,
        // 终止流程
        TERMINATE
    }

    @Getter
    public static class RejectResult {
        private final RejectType type;
        private String nodeId;

        public boolean isReturnNode() {
            return type == RejectType.RETURN_NODE;
        }

        public boolean isTerminate() {
            return type == RejectType.TERMINATE;
        }

        public RejectResult(String result) {
            if (result.equals(TYPE_TERMINATE)) {
                this.type = RejectType.TERMINATE;
            } else {
                this.type = RejectType.RETURN_NODE;
                this.nodeId = result;
            }
        }
    }

}
