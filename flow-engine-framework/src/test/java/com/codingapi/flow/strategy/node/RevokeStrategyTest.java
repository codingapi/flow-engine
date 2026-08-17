package com.codingapi.flow.strategy.node;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 撤回策略单元测试
 * <p>
 * 覆盖撤回类型判定(isRemoveNext)、fromMap 反序列化与 copy 复制
 */
class RevokeStrategyTest {

    @Test
    void isRemoveNext_whenTypeIsRevokeNext_shouldReturnTrue() {
        RevokeStrategy strategy = new RevokeStrategy(true, RevokeStrategy.Type.REVOKE_NEXT);
        assertTrue(strategy.isRemoveNext());
    }

    @Test
    void isRemoveNext_whenTypeIsRevokeCurrent_shouldReturnFalse() {
        RevokeStrategy strategy = new RevokeStrategy(true, RevokeStrategy.Type.REVOKE_CURRENT);
        assertFalse(strategy.isRemoveNext());
    }

    @Test
    void fromMap_emptyMap_shouldReturnNull() {
        assertNull(RevokeStrategy.fromMap(Map.of()));
    }

    @Test
    void fromMap_shouldRestoreEnableAndType() {
        Map<String, Object> map = new HashMap<>();
        map.put("enable", true);
        map.put("type", "REVOKE_NEXT");
        RevokeStrategy strategy = RevokeStrategy.fromMap(map);
        assertTrue(strategy.isEnable());
        assertEquals(RevokeStrategy.Type.REVOKE_NEXT, strategy.getType());
    }

    @Test
    void copy_shouldCopyEnableAndType() {
        RevokeStrategy source = new RevokeStrategy(true, RevokeStrategy.Type.REVOKE_NEXT);
        RevokeStrategy target = new RevokeStrategy(false, RevokeStrategy.Type.REVOKE_CURRENT);
        target.copy(source);
        assertTrue(target.isEnable());
        assertEquals(RevokeStrategy.Type.REVOKE_NEXT, target.getType());
    }
}