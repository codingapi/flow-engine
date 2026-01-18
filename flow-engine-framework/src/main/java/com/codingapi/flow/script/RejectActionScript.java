package com.codingapi.flow.script;

import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 拒绝脚本
 * 拒绝，拒绝时需要根据拒绝的配置流程来设置,退回上级节点、退回指定节点、终止流程
 */
@AllArgsConstructor
public class RejectActionScript {

    public static final String SCRIPT_DEFAULT = "def run(session){return new com.codingapi.flow.script.RejectActionScript.RejectResult('RETURN_PREV')}";

    @Getter
    private final String script;

    public RejectResult execute(FlowSession session) {
        return ScriptRuntimeContext.getInstance().run(script, RejectResult.class, session);
    }

    /**
     * 退回至上一流程
     */
    public static RejectActionScript defaultScript() {
        return new RejectActionScript(SCRIPT_DEFAULT);
    }


    public enum RejectType {
        // 退回上级节点
        RETURN_PREV,
        // 退回指定节点
        RETURN_NODE,
        // 终止流程
        TERMINATE
    }

    @Getter
    public static class RejectResult {
        private final RejectType type;
        private String nodeId;


        public boolean isReturnPrev() {
            return type == RejectType.RETURN_PREV;
        }

        public boolean isReturnNode() {
            return type == RejectType.RETURN_NODE;
        }

        public boolean isTerminate() {
            return type == RejectType.TERMINATE;
        }

        public RejectResult(String result) {
            if (result.equals("RETURN_PREV")) {
                this.type = RejectType.RETURN_PREV;
            } else if (result.equals("TERMINATE")) {
                this.type = RejectType.TERMINATE;
            } else {
                this.type = RejectType.RETURN_NODE;
                this.nodeId = result;
            }
        }
    }


}
