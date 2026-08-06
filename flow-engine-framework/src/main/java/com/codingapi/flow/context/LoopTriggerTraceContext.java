package com.codingapi.flow.context;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 循环触发点内存跟踪上下文（被动式循环检测）。
 * <p>
 * 触发点执行时（子流程创建、抄送自动流转等）将标识记录到内存 Map，在时间窗口内同一标识
 * （同一流程实例 + 同一节点 + 同一触发类型）再次执行即判定为循环。循环产生通常极快，
 * 不会跨越太长的周期，因此标记在时间窗口后由共享调度线程自动清理，内存有界。
 * <p>
 * 相比主动查询（沿 parentId 回溯 / 全量流程记录统计），该机制不主动访问数据库，
 * 仅在触发点记录与对照，运行期开销可忽略。
 */
public class LoopTriggerTraceContext {

    /**
     * 默认检测窗口：30 秒。窗口内重复触发即判定循环，窗口外视为新触发。
     */
    public static final long DEFAULT_WINDOW_MILLIS = 1000 * 30;

    /**
     * 共享清理调度线程，替代 per-cache Timer。
     */
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "loop-trigger-trace-clear");
                thread.setDaemon(true);
                return thread;
            });

    @Getter
    private static final LoopTriggerTraceContext instance = new LoopTriggerTraceContext();

    private final Map<String, Long> traces = new ConcurrentHashMap<>();

    private LoopTriggerTraceContext() {
        SCHEDULER.scheduleAtFixedRate(this::clearExpired, DEFAULT_WINDOW_MILLIS, DEFAULT_WINDOW_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录触发点并检查是否在默认窗口内重复触发。
     *
     * @param key 触发点标识（流程实例 + 节点 + 触发类型）
     * @return true 表示窗口内重复触发（循环）
     */
    public boolean trace(String key) {
        return this.trace(key, DEFAULT_WINDOW_MILLIS);
    }

    /**
     * 记录触发点并检查是否在指定窗口内重复触发。
     *
     * @param key          触发点标识
     * @param windowMillis 检测窗口（毫秒）
     * @return true 表示窗口内重复触发（循环）
     */
    public boolean trace(String key, long windowMillis) {
        long now = System.currentTimeMillis();
        Long previous = this.traces.put(key, now);
        return previous != null && now - previous <= windowMillis;
    }

    /**
     * 清理窗口外的过期标记。
     */
    public void clearExpired() {
        long now = System.currentTimeMillis();
        this.traces.entrySet().removeIf(entry -> now - entry.getValue() > DEFAULT_WINDOW_MILLIS);
    }

    /**
     * 清空全部标记（测试隔离用）。
     */
    public void clear() {
        this.traces.clear();
    }
}