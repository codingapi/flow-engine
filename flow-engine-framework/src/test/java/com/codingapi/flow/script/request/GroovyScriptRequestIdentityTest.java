package com.codingapi.flow.script.request;

import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 脚本请求对象的身份标识透传测试（issue #288 P1）。
 *
 * <p>验证下游可通过 Groovy 脚本读取发起人 / 提交人 / 当前审批人的身份标识，
 * 用于实现节点分支差异与人员差异。
 */
class GroovyScriptRequestIdentityTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    private FlowSession session(IFlowOperator current, IFlowOperator created, IFlowOperator submit) {
        return new FlowSession(
                factory.repositoryHolder,
                current,
                created,
                submit,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null
        );
    }

    /**
     * 测试目标：请求对象直接读取三类操作者的身份标识。
     * 前置条件：当前审批人、发起人、提交人分别带不同身份。
     * 执行步骤：由会话构建 GroovyScriptRequest 并读取身份。
     * 期望断言：三个身份方法分别返回各自身份。
     */
    @Test
    void should_expose_identity_getters() {
        IFlowOperator current = User.withIdentity(2, "boss", "department2");
        IFlowOperator created = User.withIdentity(1, "user1", "department1");

        GroovyScriptRequest request = new GroovyScriptRequest(session(current, created, created));

        assertEquals("department2", request.getCurrentOperatorIdentity());
        assertEquals("department1", request.getCreatedOperatorIdentity());
        assertEquals("department1", request.getSubmitOperatorIdentity());
    }

    /**
     * 测试目标：存量用户未实现身份时，脚本读取到默认身份（空串）而非 null。
     * 前置条件：三类操作者均未覆盖 getIdentity()。
     * 执行步骤：构建请求并读取身份。
     * 期望断言：三个身份方法均返回空串。
     */
    @Test
    void should_return_default_identity_when_operator_has_no_identity() {
        IFlowOperator user = new User(1, "user1");

        GroovyScriptRequest request = new GroovyScriptRequest(session(user, user, user));

        assertEquals("", request.getCurrentOperatorIdentity());
        assertEquals("", request.getCreatedOperatorIdentity());
        assertEquals("", request.getSubmitOperatorIdentity());
    }

    /**
     * 测试目标：Groovy 脚本内可通过 request 读取当前审批人身份（节点分支差异的基础能力）。
     * 前置条件：当前审批人身份为 department2。
     * 执行步骤：执行节点标题脚本返回身份标识。
     * 期望断言：脚本返回 department2。
     */
    @Test
    void should_read_identity_in_groovy_script() {
        IFlowOperator current = User.withIdentity(2, "boss", "department2");
        IFlowOperator created = User.withIdentity(1, "user1", "department1");

        NodeTitleScript script = new NodeTitleScript(
                FlowGroovyScriptFactory.createNodeTitleScript("def run(request){return request.getCurrentOperatorIdentity()}")
                        .getKey());

        String identity = script.execute(session(current, created, created));

        assertEquals("department2", identity);
    }
}
