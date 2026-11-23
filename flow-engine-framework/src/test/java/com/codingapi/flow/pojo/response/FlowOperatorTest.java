package com.codingapi.flow.pojo.response;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 响应体操作者身份字段测试（issue #288 P1）。
 *
 * <p>响应体为公开契约，必须保留双参构造器以保证下游实现源码兼容。
 */
class FlowOperatorTest {

    /**
     * 测试目标：存量双参构造器仍然可用，身份默认空串。
     * 前置条件：使用 (id, name) 构造。
     * 执行步骤：读取身份。
     * 期望断言：身份为空串，不出现 null。
     */
    @Test
    void should_keep_two_args_constructor_and_default_identity() {
        FlowOperator operator = new FlowOperator(1L, "张三");

        assertEquals(1L, operator.getId());
        assertEquals("张三", operator.getName());
        assertEquals("", operator.getIdentity());
    }

    /**
     * 测试目标：由流程操作者构建响应体时携带身份。
     * 前置条件：操作者身份为 department1。
     * 执行步骤：构造响应体。
     * 期望断言：id、名称、身份均正确。
     */
    @Test
    void should_take_identity_from_flow_operator() {
        IFlowOperator user = User.withIdentity(2, "boss", "department2");

        FlowOperator operator = new FlowOperator(user);

        assertEquals(2L, operator.getId());
        assertEquals("boss", operator.getName());
        assertEquals("department2", operator.getIdentity());
    }

    /**
     * 测试目标：三参构造器支持从记录直接还原带身份的响应体。
     * 前置条件：记录中保存了审批人 id、名称与身份。
     * 执行步骤：构造响应体。
     * 期望断言：身份被正确设置。
     */
    @Test
    void should_support_three_args_constructor() {
        FlowOperator operator = new FlowOperator(3L, "lorne", "department3");

        assertEquals("department3", operator.getIdentity());
    }
}
