package com.codingapi.flow.operator;

import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 身份标识归一化规则测试。
 *
 * <p>约定的核心规则：null 与空串等价，统一视为「默认身份」（issue #288）。
 */
class OperatorIdentityTest {

    @Test
    void should_normalize_null_to_empty() {
        // given
        String identity = null;

        // when
        String normalized = OperatorIdentity.normalize(identity);

        // then
        assertEquals("", normalized);
    }

    @Test
    void should_keep_identity_value() {
        // given
        String identity = "department1";

        // when
        String normalized = OperatorIdentity.normalize(identity);

        // then
        assertEquals("department1", normalized);
    }

    @Test
    void should_treat_null_and_empty_as_default() {
        assertTrue(OperatorIdentity.isDefault(null));
        assertTrue(OperatorIdentity.isDefault(""));
        assertFalse(OperatorIdentity.isDefault("department1"));
    }

    @Test
    void should_compare_identities_null_safe() {
        assertTrue(OperatorIdentity.same(null, ""));
        assertTrue(OperatorIdentity.same("department1", "department1"));
        assertFalse(OperatorIdentity.same("department1", "department2"));
        assertFalse(OperatorIdentity.same(null, "department1"));
    }

    @Test
    void should_read_identity_from_operator_and_handle_null_operator() {
        // given
        IFlowOperator operator = new User(1, "张三");

        // when & then
        assertEquals("", OperatorIdentity.of(operator));
        assertEquals("", OperatorIdentity.of(null));
    }

    @Test
    void should_provide_default_identity_on_operator_interface() {
        // given：未实现身份的存量操作者（测试 User 未覆盖 default 方法）
        IFlowOperator operator = new IFlowOperator() {
            @Override
            public long getUserId() {
                return 1;
            }

            @Override
            public String getName() {
                return "张三";
            }

            @Override
            public boolean isFlowManager() {
                return false;
            }

            @Override
            public IFlowOperator forwardOperator(GroovyScriptRequest request) {
                return null;
            }
        };

        // when & then：默认身份为空串，附加属性为空集合，保证存量实现零改动可用
        assertEquals("", operator.getIdentity());
        assertTrue(operator.getIdentityAttributes().isEmpty());
    }

    @Test
    void should_allow_downstream_override_identity() {
        // given
        IFlowOperator operator = new IdentityUser(1, "张三", "department1");

        // when & then
        assertEquals("department1", OperatorIdentity.of(operator));
        assertEquals("部门一", operator.getIdentityAttributes().get("deptName"));
    }

    /**
     * 模拟下游自定义身份的操作者实现。
     */
    private static class IdentityUser extends User {

        private final String identity;

        IdentityUser(long userId, String name, String identity) {
            super(userId, name);
            this.identity = identity;
        }

        @Override
        public String getIdentity() {
            return identity;
        }

        @Override
        public Map<String, String> getIdentityAttributes() {
            return Map.of("deptName", "部门一");
        }
    }
}
