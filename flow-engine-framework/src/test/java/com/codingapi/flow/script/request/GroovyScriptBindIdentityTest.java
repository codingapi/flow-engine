package com.codingapi.flow.script.request;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.cache.FlowOperatorLocalThreadCache;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * 脚本绑定对象 {@code $bind.getOperatorByIdentity} 测试（issue #288 衍生 / #295）。
 *
 * <p>P3 提交新增的脚本能力此前零用例：验证按身份取到的是带身份的操作者，
 * 同一人员的两个身份互不串味，且默认身份与历史 {@code getOperatorById} 完全等价。
 */
class GroovyScriptBindIdentityTest {

    private MyFlowServiceFactory factory;
    private GroovyScriptBind bind;

    @BeforeEach
    void setUp() {
        factory = new MyFlowServiceFactory();
        factory.userGateway.save(new User(2, "user2"));
        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
        FlowOperatorLocalThreadCache.getInstance().clear();
        bind = new GroovyScriptBind(FlowScriptContext.getInstance());
    }

    /**
     * 测试目标：脚本按身份取人返回带身份的操作者。
     * 前置条件：人员 2 已注册。
     * 执行步骤：调用 $bind.getOperatorByIdentity(2, 'department1')。
     * 期望断言：返回对象的人员 id 为 2、身份为 department1。
     */
    @Test
    void should_load_operator_with_requested_identity() {
        IFlowOperator operator = bind.getOperatorByIdentity(2, "department1");

        assertNotNull(operator);
        assertEquals(2, operator.getUserId());
        assertEquals("department1", OperatorIdentity.of(operator));
    }

    /**
     * 测试目标：默认身份与历史单参方法等价（向后兼容）。
     * 前置条件：人员 2 已注册。
     * 执行步骤：分别用 getOperatorById 与 getOperatorByIdentity(2, "") 取人。
     * 期望断言：两者身份均为默认身份空串。
     */
    @Test
    void should_keep_default_identity_equivalent_to_legacy_method() {
        IFlowOperator legacy = bind.getOperatorById(2);
        IFlowOperator byIdentity = bind.getOperatorByIdentity(2, OperatorIdentity.DEFAULT);

        assertNotNull(legacy);
        assertNotNull(byIdentity);
        assertEquals(OperatorIdentity.of(legacy), OperatorIdentity.of(byIdentity));
        assertEquals("", OperatorIdentity.of(byIdentity));
    }

    /**
     * 测试目标：同一人员的两个身份是不同的参与人。
     * 前置条件：人员 2 已注册。
     * 执行步骤：连续以 department1 / department2 取人。
     * 期望断言：两个对象不同，身份各自独立（缓存按 (id, identity) 隔离）。
     */
    @Test
    void should_isolate_two_identities_of_same_operator() {
        IFlowOperator department1 = bind.getOperatorByIdentity(2, "department1");
        IFlowOperator department2 = bind.getOperatorByIdentity(2, "department2");

        assertNotSame(department1, department2);
        assertEquals("department1", OperatorIdentity.of(department1));
        assertEquals("department2", OperatorIdentity.of(department2));
        // 再次取 department1 仍应是同一个带身份的对象，不能被 department2 覆盖
        assertEquals("department1", OperatorIdentity.of(bind.getOperatorByIdentity(2, "department1")));
    }
}
