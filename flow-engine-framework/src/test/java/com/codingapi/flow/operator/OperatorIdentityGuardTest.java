package com.codingapi.flow.operator;

import com.codingapi.flow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 身份透传告警测试（issue #288 衍生 / #294）。
 *
 * <p>下游不覆盖带身份的取人方法时，身份会被静默丢弃，前端按身份查待办会得到空列表。
 * 这里验证告警条件与去重：只在「请求身份非默认、回查结果身份为默认」时告警，且同一参与人只告警一次。
 */
class OperatorIdentityGuardTest {

    @BeforeEach
    void setUp() {
        OperatorIdentityGuard.resetWarnedCache();
    }

    @Test
    void should_warn_when_requested_identity_is_lost() {
        User resolved = new User(1, "user1");

        assertTrue(OperatorIdentityGuard.check(1, "department1", resolved));
    }

    @Test
    void should_warn_only_once_for_same_operator_and_identity() {
        User resolved = new User(1, "user1");

        assertTrue(OperatorIdentityGuard.check(1, "department1", resolved));
        assertFalse(OperatorIdentityGuard.check(1, "department1", resolved), "同一参与人不应重复告警");
        assertTrue(OperatorIdentityGuard.check(1, "department2", resolved), "不同身份应各自告警一次");
    }

    @Test
    void should_not_warn_for_default_identity() {
        User resolved = new User(1, "user1");

        assertFalse(OperatorIdentityGuard.check(1, OperatorIdentity.DEFAULT, resolved));
        assertFalse(OperatorIdentityGuard.check(1, null, resolved));
    }

    @Test
    void should_not_warn_when_gateway_honors_identity() {
        User resolved = User.withIdentity(1, "user1", "department1");

        assertFalse(OperatorIdentityGuard.check(1, "department1", resolved));
    }

    @Test
    void should_not_warn_when_operator_not_found() {
        // 查不到人属于另一种错误（由上层按需处理），不应在这里刷身份告警
        assertFalse(OperatorIdentityGuard.check(1, "department1", null));
    }
}
