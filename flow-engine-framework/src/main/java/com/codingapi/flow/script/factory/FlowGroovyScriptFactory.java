package com.codingapi.flow.script.factory;

import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.request.GroovyWorkflowRequest;
import com.codingapi.springboot.script.GroovyScript;

import java.util.List;
import java.util.Map;

public class FlowGroovyScriptFactory {

    public static final String DEFAULT_SCRIPT_TYPE_ONE = "flow";

    public static GroovyScript createRouterScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>路由触发脚本</strong>
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>String</code> 类型。
                返回的是为跳转的流程节点 id，即 <code>nodeId</code>。
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("router-script")
                .description(description)
                .method("run")
                .returnType(String.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createNodeTitleScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>节点待办标题脚本</strong>
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>String</code> 类型，即为展示的待办标题数据。
                标题数据支持 HTML 语法格式，以适配不同的标题表现形式。例如：<br><code>return '你好，&lt;span style="color:red"&gt;XXX&lt;/span&gt;你有一个待办数据'</code>
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("node-title")
                .description(description)
                .method("run")
                .returnType(String.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();


        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createConditionScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>条件匹配脚本</strong>
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>Boolean</code> 类型。返回 <code>true</code> 则为满足条件，执行后续操作；返回 <code>false</code> 则不执行后续条件。
                当所有条件都不满足时，默认会执行其他情况，即 <code>else</code> 的操作逻辑。
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("condition")
                .description(description)
                .method("run")
                .returnType(Boolean.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createTriggerScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>触发节点脚本</strong>
                脚本说明：即用户自定义处理逻辑。
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>Void</code> 类型，即不需要返回数据。
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("trigger")
                .description(description)
                .method("run")
                .returnType(Void.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();


        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createSubProcessScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>子流程任务脚本</strong>
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>FlowCreateRequest</code> 类型。
                该对象是流程发起的参数对象，通过构建该对象控制子流程触发的流程节点。
                """;


        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("sub-process")
                .description(description)
                .method("run")
                .returnType(FlowCreateRequest.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createOperatorLoadScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>匹配人员列表脚本</strong>
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>List&lt;Long&gt;</code> 类型，即 <code>long[]</code> 数组格式，每一个 <code>long</code> 的 id 数据为一个人员的 id，示例格式为 <code>[1,2,3]</code>。
                返回的人员 id 将会作为匹配人员信息，作为流程的审批者。
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("operator-load")
                .description(description)
                .method("run")
                .returnType(List.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createOperatorMatchScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>人员匹配脚本</strong>
                传入对象为 <code>GroovyWorkflowRequest</code> 类型的 request 对象，返回数据为 <code>Boolean</code> 类型。返回 <code>true</code> 时表明该人拥有发起流程的权限，否则反之。
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("operator-match")
                .description(description)
                .method("run")
                .returnType(Boolean.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyWorkflowRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createErrorTriggerScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>异常触发脚本</strong>
                脚本说明：所谓的异常脚本，是指流程在执行时没有匹配到审批人员时触发的情况。
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>Object</code> 类型，实际支持两种数据结构：
                <ul>
                <li><strong>String 格式</strong>：对应的节点 id（<code>nodeId</code>）。当返回字符串类型的节点 id 时，出现异常时会跳转到对应的节点上执行审批。</li>
                <li><strong>List&lt;Long&gt; 格式</strong>：即 <code>long[]</code> 格式，示例：<code>return [1,2,3]</code>。当返回 <code>List&lt;Long&gt;</code> 格式时，代表指定审批人员，即当异常时将会将这个流程转交给指定的这些人员来审批。</li>
                </ul>
                """;


        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("error-trigger")
                .description(description)
                .method("run")
                .returnType(Object.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }


    public static GroovyScript createActionDisplayScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>动作按钮展示脚本</strong>
                脚本说明：实现是否展示改动作按钮的控制。
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>boolean</code> 类型，当为true时则展示按钮：
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("action-display")
                .description(description)
                .method("run")
                .returnType(Boolean.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createActionCustomScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>自定义脚本</strong>
                脚本说明：即当点击自定义按钮时的操作逻辑，在完成自定义的业务之后，需要再联动一个动作。
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>String</code> 类型，为操作类型。数据分类及对应的业务含义如下：
                <ul>
                <li><code>SAVE</code> — 保存</li>
                <li><code>PASS</code> — 通过</li>
                <li><code>REJECT</code> — 拒绝</li>
                <li><code>ADD_AUDIT</code> — 加签</li>
                <li><code>DELEGATE</code> — 委派</li>
                <li><code>RETURN</code> — 退回</li>
                <li><code>TRANSFER</code> — 转办</li>
                </ul>
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("action-custom")
                .description(description)
                .method("run")
                .returnType(String.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }

    public static GroovyScript createActionRejectScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                <strong>拒绝操作脚本</strong>
                脚本说明：即当用户点击拒绝时会发生的处理逻辑。
                传入对象为 <code>GroovyScriptRequest</code> 对象，返回数据格式为 <code>String</code> 类型，数据分为两种情况：
                <ul>
                <li>返回固定的 <code>TERMINATE</code> 字符串，代表终止流程。</li>
                <li>返回节点 id，即 <code>nodeId</code> 的字符串，代表跳转到指定的节点。</li>
                </ul>
                """;

        GroovyScript groovyScript = GroovyScript.builder(key)
                .script(script)
                .typeOne(DEFAULT_SCRIPT_TYPE_ONE)
                .typeTwo("action-reject")
                .description(description)
                .method("run")
                .returnType(String.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests(Map.of("request", GroovyScriptRequest.class))
                .build();

        groovyScript.temp();
        return groovyScript;
    }

}
