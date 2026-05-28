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
                路由触发脚本\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为String类型\\n
                返回的是为跳转的流程节点id，即nodeId。
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

        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createNodeTitleScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                节点待办标题脚本\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为String类型，即为展示的待办标题数据。\\n
                标题数据支持html的语法格式，以适配不同的标题表现形式数据。例如 return "你好，<span style=\"color:read\">XXX</span>你有一个待办数据"
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


        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createConditionScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                条件匹配脚本\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为Boolean类型，返回true则为满足条件，执行后续的操作，返回false则不执行后续条件。\\n
                当所有的条件都不满足的时候，默认会执行其他情况，即else的操作逻辑。
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

        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createTriggerScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                触发节点脚本\\n
                脚本说明：即用户自定义处理逻辑。\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为Void类型，即不需要返回数据。\\n
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


        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createSubProcessScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                子流程任务脚本\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为FlowCreateRequest类型\\n
                概对象是流程发起的参数对象，通过构建该对象控制子流程触发的流程节点。
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

        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createOperatorLoadScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                匹配人员列表脚本\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为List<Long>类型，即long[]数组格式，每一个long的id数据为一个人员的id，实例格式为[1,2,3]\\n
                返回的人员id将会作为匹配人员信息，作为流程的审批者。
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

        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createOperatorMatchScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                人员匹配脚本\\n
                传入对象为GroovyWorkflowRequest类型的request对象，返回数据为Boolean类型，返回true时表明该人拥有发起流程的权限，否则反之。
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

        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createErrorTriggerScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                异常触发脚本\\n
                脚本说明：所谓的异常脚本，是指流程在执行时没有匹配到审批人员时触发的情况。\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为Object类型，实际支持两种数据结构。\\n
                一种是String格式的字符串，对应的节点Id(nodeId)。\\n
                当返回的是字符串类型的节点id时，则会当出现异常时会跳转到对应的节点上去执行审批。\\n
                另一种是List<Long>，即long[]格式，实例格式为return [1,2,3]\\n
                当返回的为List<Long>格式时，代表的是指定审批人员，即当异常时将会将这个流程转交给指定的这些人员来审批。
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

        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createActionCustomScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                自定义脚本\\n
                脚本说明：即当点击自定义按钮时的操作逻辑，在完成自定义的业务之后，需要再联动一个动作。\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为String类型，为操作类型，数据分类有SAVE、PASS、REJECT、ADD_AUDIT、DELEGATE、RETURN、TRANSFER。\\n
                对应的业务含义分为为：保存、通过、拒绝、加签、委派、退回、转办。\\n
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

        groovyScript.save();
        return groovyScript;
    }

    public static GroovyScript createActionRejectScript(String script) {
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        String description = """
                拒绝操作脚本\\n
                脚本说明：即当用户点击拒绝时会发生的处理逻辑。\\n
                传入对象为GroovyScriptRequest对象，返回数据格式为String类型，数据分为两种情况。\\n
                一种是返回固定的TERMINATE字符串，TERMINATE代表的是终止流程。\\n
                另一种是节点id，即nodeId的字符串，代表的是跳转到的指定的节点。\\n
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

        groovyScript.save();
        return groovyScript;
    }

}
