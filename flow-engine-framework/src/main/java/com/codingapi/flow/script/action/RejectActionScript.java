package com.codingapi.flow.script.action;

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

    public static final String SCRIPT_START = "def run(session){return new com.codingapi.flow.script.action.RejectActionScript.RejectResult(session.getStartNode().getId())}";
    public static final String SCRIPT_TERMINATE = "def run(session){return new com.codingapi.flow.script.action.RejectActionScript.RejectResult(\"TERMINATE\")}";

    @Getter
    private final String script;

    public RejectResult execute(FlowSession session) {
        return ScriptRuntimeContext.getInstance().run(script, RejectResult.class, session);
    }

    /**
     * 退回至发起节点
     */
    public static RejectActionScript startScript() {
        return new RejectActionScript(SCRIPT_START);
    }

    /**
     * 终止流程
     */
    public static RejectActionScript terminateScript() {
        return new RejectActionScript(SCRIPT_TERMINATE);
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
            if (result.equals("TERMINATE")) {
                this.type = RejectType.TERMINATE;
            } else {
                this.type = RejectType.RETURN_NODE;
                this.nodeId = result;
            }
        }
    }

}
