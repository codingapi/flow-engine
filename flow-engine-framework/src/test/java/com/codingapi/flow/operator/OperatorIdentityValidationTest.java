package com.codingapi.flow.operator;

import com.codingapi.flow.exception.FlowValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 身份标识校验测试（issue #288 衍生 / #294）。
 *
 * <p>校验的目标是让「内存里的身份」与「数据库里的身份」语义一致：
 * 长度上限与 DDL 列宽对齐，禁止与待办合并键后缀冲突的字符与控制字符。
 */
class OperatorIdentityValidationTest {

    @Test
    void should_normalize_null_to_default_and_keep_empty() {
        assertEquals(OperatorIdentity.DEFAULT, OperatorIdentity.validate(null));
        assertEquals("", OperatorIdentity.validate(""));
    }

    @Test
    void should_accept_identity_at_max_length() {
        String identity = "a".repeat(OperatorIdentity.MAX_LENGTH);

        assertEquals(identity, OperatorIdentity.validate(identity));
    }

    @Test
    void should_reject_identity_longer_than_max_length() {
        String identity = "a".repeat(OperatorIdentity.MAX_LENGTH + 1);

        FlowValidationException exception = assertThrows(FlowValidationException.class,
                () -> OperatorIdentity.validate(identity));
        assertTrue(exception.getMessage().contains("exceeds the max length"), exception.getMessage());
    }

    @Test
    void should_reject_identity_containing_merge_key_separator() {
        FlowValidationException exception = assertThrows(FlowValidationException.class,
                () -> OperatorIdentity.validate("department1#manager"));

        assertTrue(exception.getMessage().contains("illegal character"), exception.getMessage());
    }

    @Test
    void should_reject_identity_containing_control_character() {
        FlowValidationException exception = assertThrows(FlowValidationException.class,
                () -> OperatorIdentity.validate("department\u00011"));

        assertTrue(exception.getMessage().contains("illegal character"), exception.getMessage());
    }

    @Test
    void should_keep_normalize_lenient_for_existing_data() {
        // 存量数据可能含非法字符，读取路径必须保持宽松，不能因为校验把历史流程读崩
        assertSame("department1#manager", OperatorIdentity.normalize("department1#manager"));
    }
}
