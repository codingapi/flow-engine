package com.codingapi.flow.strategy.workflow;

import com.codingapi.flow.domain.UrgeInterval;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 催办策略单元测试
 * <p>
 * 覆盖催办频率判断(hasUrge 毫秒换算)、fromMap 反序列化与 copy 复制
 */
class UrgeStrategyTest {

    @Test
    void hasUrge_intervalElapsed_shouldReturnTrue() {
        // 间隔已超过默认 60 秒(createTime 在 61 秒前) -> 可再次催办
        UrgeStrategy strategy = UrgeStrategy.defaultStrategy();
        UrgeInterval past = new UrgeInterval(1, "process", 100L, System.currentTimeMillis() - 61000);
        assertTrue(strategy.hasUrge(past));
    }

    @Test
    void hasUrge_withinInterval_shouldReturnFalse() {
        // 刚催办过(createTime 为当前时刻) -> 仍在间隔内，不可催办
        UrgeStrategy strategy = UrgeStrategy.defaultStrategy();
        UrgeInterval recent = new UrgeInterval(1, "process", 100L, System.currentTimeMillis());
        assertFalse(strategy.hasUrge(recent));
    }

    @Test
    void fromMap_emptyMap_shouldReturnNull() {
        assertNull(UrgeStrategy.fromMap(Map.of()));
    }

    @Test
    void fromMap_shouldRestoreEnableAndInterval() {
        Map<String, Object> map = new HashMap<>();
        map.put("enable", true);
        map.put("interval", "30");
        UrgeStrategy strategy = UrgeStrategy.fromMap(map);
        assertTrue(strategy.isEnable());
        assertEquals(30, strategy.getInterval());
    }

    @Test
    void copy_shouldCopyEnable() {
        UrgeStrategy source = UrgeStrategy.defaultStrategy();
        UrgeStrategy target = new UrgeStrategy();
        target.copy(source);
        assertTrue(target.isEnable());
    }
}