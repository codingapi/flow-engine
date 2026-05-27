package com.codingapi.flow.script.action;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 拒绝脚本
 * 拒绝，拒绝时需要根据拒绝的配置流程来设置,退回指定节点、终止流程
 */
@AllArgsConstructor
public class RejectActionScript {

    public static final String TYPE_TERMINATE = "TERMINATE";

    public static final String description = """
            拒绝操作脚本\\n
            脚本说明：即当用户点击拒绝时会发生的处理逻辑。\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为String类型，数据分为两种情况。\\n
            一种是返回固定的TERMINATE字符串，TERMINATE代表的是终止流程。\\n
            另一种是节点id，即nodeId的字符串，代表的是跳转到的指定的节点。\\n
            """;

    @Getter
    private final String script;

    public RejectResult execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, description, String.class, GroovyBindObjectBuilder.builder()
                .add("request", request)
                .build());
        String result = ScriptRuntimeContext.getInstance().execute(runtimeRequest);
        return new RejectResult(result);
    }

    /**
     * 退回至发起节点
     */
    public static RejectActionScript defaultScript() {
        return new RejectActionScript(ScriptRegistryContext.getInstance().getActionRejectScript());
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
