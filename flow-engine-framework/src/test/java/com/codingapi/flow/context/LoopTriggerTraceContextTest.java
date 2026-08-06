package com.codingapi.flow.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 循环触发点内存跟踪上下文单元测试。
 * <p>
 * 验证被动式循环检测的核心语义：窗口内重复触发判定循环、窗口外视为新触发、
 * 清理后重新计数。
 */
class LoopTriggerTraceContextTest {

    private LoopTriggerTraceContext context;

    @BeforeEach
    void setUp() {
        context = LoopTriggerTraceContext.getInstance();
        context.clear();
    }

    /**
     * 测试目标：同一触发点在窗口内第二次触发时判定为循环。
     * 前置条件：空上下文。
     * 执行步骤：对同一 key 连续 trace 两次。
     * 期望断言：第一次返回 false（首触发），第二次返回 true（窗口内重复）。
     */
    @Test
    void shouldDetectRepeatedTriggerWithinWindow() {
        String key = "process-1:NOTIFY:node-a";
        assertFalse(context.trace(key), "首次触发不应判定为循环");
        assertTrue(context.trace(key), "窗口内重复触发应判定为循环");
    }

    /**
     * 测试目标：不同触发点互不影响。
     * 前置条件：空上下文。
     * 执行步骤：对两个不同 key 各 trace 一次。
     * 期望断言：均为 false。
     */
    @Test
    void shouldNotAffectDifferentKeys() {
        assertFalse(context.trace("process-1:NOTIFY:node-a"));
        assertFalse(context.trace("process-1:NOTIFY:node-b"));
        assertFalse(context.trace("process-2:NOTIFY:node-a"));
    }

    /**
     * 测试目标：窗口外再次触发视为新触发，不判定循环。
     * 前置条件：空上下文。
     * 执行步骤：trace 后等待超过窗口，再次 trace。
     * 期望断言：第二次返回 false。
     */
    @Test
    void shouldTreatExpiredWindowAsNewTrigger() throws InterruptedException {
        String key = "process-1:NOTIFY:node-a";
        long windowMillis = 50;
        assertFalse(context.trace(key, windowMillis));
        Thread.sleep(windowMillis + 30);
        assertFalse(context.trace(key, windowMillis), "窗口外的触发应视为新触发");
    }

    /**
     * 测试目标：clear 后所有标记清空，重新计数。
     * 前置条件：已有触发标记。
     * 执行步骤：trace 后 clear，再 trace 同一 key。
     * 期望断言：clear 后返回 false。
     */
    @Test
    void shouldResetAfterClear() {
        String key = "process-1:NOTIFY:node-a";
        assertFalse(context.trace(key));
        context.clear();
        assertFalse(context.trace(key), "clear 后应重新计数");
    }

    /**
     * 测试目标：clearExpired 只清理窗口外标记，窗口内标记保留。
     * 前置条件：空上下文。
     * 执行步骤：两条标记，一条窗口外、一条窗口内，调用 clearExpired 后分别 trace。
     * 期望断言：窗口外的重新计数，窗口内的仍判定循环。
     */
    @Test
    void shouldClearOnlyExpiredEntries() throws InterruptedException {
        String expiredKey = "process-1:NOTIFY:node-a";
        String freshKey = "process-2:NOTIFY:node-b";
        long windowMillis = 50;
        assertFalse(context.trace(expiredKey, windowMillis));
        Thread.sleep(windowMillis + 30);
        assertFalse(context.trace(freshKey, windowMillis));

        context.clearExpired();

        assertFalse(context.trace(expiredKey, windowMillis), "过期标记清理后应重新计数");
        assertTrue(context.trace(freshKey, windowMillis), "窗口内标记不应被清理，重复触发仍判定循环");
    }
}