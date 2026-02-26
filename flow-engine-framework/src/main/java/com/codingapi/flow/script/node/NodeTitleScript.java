package com.codingapi.flow.script.node;

import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.TitleGroovyRequest;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点待办标题脚本
 */
@AllArgsConstructor
public class NodeTitleScript {

    public static final String SCRIPT_DEFAULT = "def run(request){return '你有一条待办'}";

    @Getter
    private final String script;

    public String execute(FlowSession session) {
        TitleGroovyRequest request = session.createTitleRequest();
        return execute(request);
    }

    /**
     * 执行标题脚本（直接使用TitleGroovyRequest）
     * @param request 标题请求对象
     * @return 标题字符串
     */
    public String execute(TitleGroovyRequest request) {
        return ScriptRuntimeContext.getInstance().run(script, String.class, request);
    }

    /**
     * 默认脚本
     */
    public static NodeTitleScript defaultScript() {
        return new NodeTitleScript(SCRIPT_DEFAULT);
    }
}
