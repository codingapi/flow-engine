package com.codingapi.flow.script.action;

import com.codingapi.flow.script.registry.ScriptRegistry;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 拒绝脚本
 * 拒绝，拒绝时需要根据拒绝的配置流程来设置,退回指定节点、终止流程
 */
@AllArgsConstructor
public class RejectActionScript {

    public static final String TYPE_TERMINATE = "TERMINATE";


    @Getter
    private final String script;

    public RejectResult execute(FlowSession session) {
        String result =  ScriptRuntimeContext.getInstance().run(script, String.class, session);
        return new RejectResult(result);
    }

    /**
     * 退回至发起节点
     */
    public static RejectActionScript defaultScript() {
        return new RejectActionScript(ScriptRegistry.getInstance().getActionRejectScript());
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
