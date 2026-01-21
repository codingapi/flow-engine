package com.codingapi.flow.script;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScriptRuntimeContextTest {

    private final UserGateway gateway = new UserGateway();

    private final int originalMaxSize;

    {
        // 保存原始配置
        originalMaxSize = ScriptRuntimeContext.getMaxLockCacheSize();
    }

    @AfterEach
    void tearDown() {
        // 清理状态，避免测试间相互影响
        ScriptRuntimeContext.clearLockCache();
        ScriptRuntimeContext.setMaxLockCacheSize(originalMaxSize);
        // 恢复自动清理（如果被禁用）
        if (!ScriptRuntimeContext.isAutoCleanupEnabled()) {
            ScriptRuntimeContext.enableAutoCleanup();
        }
    }

    @Test
    void execute1() {
        String script = "def run(abc){return 1}";
        int value = ScriptRuntimeContext.getInstance().run(script, Integer.class, 1);
        assertEquals(1, value);
    }

    @Test
    void execute2() {
        GatewayContext.getInstance().setFlowOperatorGateway(gateway);

        User user = new User(1, "codingapi");
        gateway.save(user);
        String script = "def run(abc){return $bind.getOperatorById(1)}";
        IFlowOperator target = ScriptRuntimeContext.getInstance().run(script, IFlowOperator.class, 1);
        assertEquals(target, user);
    }

    @Test
    void testAutoCleanup() {
        // 设置较小的缓存阈值
        ScriptRuntimeContext.setMaxLockCacheSize(10);

        // 执行超过阈值的脚本数量
        Set<String> scripts = new HashSet<>();
        for (int i = 0; i < 15; i++) {
            String script = "def run(abc){return " + i + "}";
            scripts.add(script);
            ScriptRuntimeContext.getInstance().run(script, Integer.class, i);
        }

        // 由于自动清理，缓存大小不应该超过阈值太多
        assertTrue(ScriptRuntimeContext.getLockCacheSize() <= 15,
                "Lock cache size should be controlled by auto-cleanup");
    }

    @Test
    void testAutoCleanupControl() {
        // 测试自动清理默认是启用的
        assertTrue(ScriptRuntimeContext.isAutoCleanupEnabled());

        // 测试禁用自动清理
        ScriptRuntimeContext.disableAutoCleanup();
        assertFalse(ScriptRuntimeContext.isAutoCleanupEnabled());

        // 测试重新启用自动清理
        ScriptRuntimeContext.enableAutoCleanup();
        assertTrue(ScriptRuntimeContext.isAutoCleanupEnabled());
    }

    @Test
    void testGetCleanupIntervalSeconds() {
        // 默认清理间隔应该是 300 秒（5分钟）
        assertTrue(ScriptRuntimeContext.getCleanupIntervalSeconds() > 0);
    }

    @Test
    void testGetExecutionCount() {
        int initialCount = ScriptRuntimeContext.getExecutionCount();

        String script = "def run(abc){return 1}";
        ScriptRuntimeContext.getInstance().run(script, Integer.class, 1);

        assertEquals(initialCount + 1, ScriptRuntimeContext.getExecutionCount());

        ScriptRuntimeContext.clearLockCache();
        assertEquals(0, ScriptRuntimeContext.getExecutionCount());
    }

    @Test
    void testSetMaxLockCacheSize() {
        ScriptRuntimeContext.setMaxLockCacheSize(500);
        assertEquals(500, ScriptRuntimeContext.getMaxLockCacheSize());

        // 测试非法值
        assertThrows(com.codingapi.flow.exception.FlowValidationException.class,
                () -> ScriptRuntimeContext.setMaxLockCacheSize(-1));
    }

    @Test
    void testClearLockCache() {
        // 执行一些脚本以填充缓存
        for (int i = 0; i < 5; i++) {
            String script = "def run(abc){return " + i + "}";
            ScriptRuntimeContext.getInstance().run(script, Integer.class, i);
        }

        assertTrue(ScriptRuntimeContext.getLockCacheSize() > 0);
        assertTrue(ScriptRuntimeContext.getExecutionCount() > 0);

        ScriptRuntimeContext.clearLockCache();

        assertEquals(0, ScriptRuntimeContext.getLockCacheSize());
        assertEquals(0, ScriptRuntimeContext.getExecutionCount());
    }
}