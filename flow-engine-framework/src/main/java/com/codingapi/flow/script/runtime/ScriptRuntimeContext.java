package com.codingapi.flow.script.runtime;

import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.exception.FlowValidationException;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.Getter;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 脚本运行时上下文
 * <p>
 * 线程安全设计：
 * 1. GroovyShell 不是线程安全的，不能在多线程间共享
 * 2. GroovyClassLoader 内部使用全局类缓存，相同脚本同时执行会导致类名冲突
 * 3. 解决方案：
 * - 每次执行创建独立的 GroovyClassLoader 和 GroovyShell 实例
 * - 使用脚本哈希值进行细粒度同步，避免相同脚本并发执行
 * <p>
 * 性能考虑：
 * - 同粒度同步只影响相同脚本的并发执行，不同脚本可以并发执行
 * - 创建 ClassLoader 的开销相对较小，现代JVM优化后性能影响可接受
 * <p>
 * 自动清理机制：
 * - 当锁缓存数量超过阈值时自动清理
 * - 可选的定时清理任务，定期清理不活跃的锁
 */
public class ScriptRuntimeContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptRuntimeContext.class);

    /**
     * 脚本锁映射，使用脚本内容的哈希值作为键
     * 相同的脚本会使用同一个锁，避免类名冲突
     */
    private static final ConcurrentHashMap<Integer, Object> SCRIPT_LOCKS = new ConcurrentHashMap<>();

    /**
     * 脚本执行计数器，用于统计执行次数
     */
    private static final AtomicInteger EXECUTION_COUNTER = new AtomicInteger(0);

    /**
     * 默认最大锁缓存数量
     */
    private static final int DEFAULT_MAX_LOCK_CACHE_SIZE = 1000;

    /**
     * 最大锁缓存数量，超过此值将触发自动清理
     * -- GETTER --
     * 获取当前配置的最大锁缓存数量
     *
     */
    @Getter
    private static volatile int maxLockCacheSize = DEFAULT_MAX_LOCK_CACHE_SIZE;

    /**
     * 默认清理间隔（秒）
     */
    private static final int DEFAULT_CLEANUP_INTERVAL_SECONDS = 300;

    /**
     * 定时清理任务的调度器
     */
    private static ScheduledExecutorService cleanupScheduler;

    /**
     * 是否启用自动清理（默认启用）
     * -- GETTER --
     * 检查自动清理是否已启用
     *
     */
    @Getter
    private static volatile boolean autoCleanupEnabled = true;

    /**
     * 自动清理间隔（秒），可通过系统属性覆盖：flow.script.cleanup.interval
     */
    private static final int CLEANUP_INTERVAL_SECONDS = Integer.parseInt(
            System.getProperty("flow.script.cleanup.interval", String.valueOf(DEFAULT_CLEANUP_INTERVAL_SECONDS))
    );

    static {
        // 注册 JVM 关闭钩子，确保资源被正确释放
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
                cleanupScheduler.shutdown();
                try {
                    if (!cleanupScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                        cleanupScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    cleanupScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }, "script-runtime-shutdown"));
    }

    @Getter
    private final static ScriptRuntimeContext instance = new ScriptRuntimeContext();

    private ScriptRuntimeContext() {
        // 自动启动定时清理任务
        if (autoCleanupEnabled) {
            startAutoCleanup();
        }
    }

    /**
     * 启动自动清理任务
     */
    private static synchronized void startAutoCleanup() {
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            return;
        }

        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "script-runtime-auto-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        cleanupScheduler.scheduleAtFixedRate(
                () -> {
                    int size = SCRIPT_LOCKS.size();
                    int executionCount = EXECUTION_COUNTER.get();
                    if (size > 0) {
                        LOGGER.debug("Auto-cleanup: clearing {} script locks (execution count: {})", size, executionCount);
                        clearLockCache();
                    }
                },
                CLEANUP_INTERVAL_SECONDS,
                CLEANUP_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        LOGGER.info("Auto-cleanup started with interval: {} seconds", CLEANUP_INTERVAL_SECONDS);
    }

    /**
     * 运行脚本
     *
     * @param script     脚本内容
     * @param returnType 返回类型
     * @param args       脚本参数
     * @param <T>        返回类型泛型
     * @return 脚本执行结果
     */
    public <T> T run(String script, Class<T> returnType, Object... args) {
        return execute("run", script, returnType, args);
    }

    /**
     * 执行脚本
     * <p>
     * 线程安全：使用脚本哈希值进行细粒度同步
     * 资源管理：执行完成后确保资源被释放
     *
     * @param method     要调用的方法名
     * @param script     脚本内容
     * @param returnType 返回类型
     * @param args       脚本参数
     * @param <T>        返回类型泛型
     * @return 脚本执行结果
     * @throws com.codingapi.flow.exception.FlowExecutionException 脚本执行失败时抛出
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(String method, String script, Class<T> returnType, Object... args) {
        // 增加执行计数
        int executionCount = EXECUTION_COUNTER.incrementAndGet();

        // 使用脚本内容的哈希值作为锁键，确保相同脚本不会并发执行
        int scriptHash = script.hashCode();
        Object lock = SCRIPT_LOCKS.computeIfAbsent(scriptHash, k -> new Object());

        // 当锁缓存数量超过阈值时，触发自动清理
        if (SCRIPT_LOCKS.size() > maxLockCacheSize) {
            LOGGER.debug("Auto-cleanup triggered: lock cache size {} exceeds threshold {}, execution count: {}",
                    SCRIPT_LOCKS.size(), maxLockCacheSize, executionCount);
            clearLockCache();
        }

        synchronized (lock) {
            GroovyClassLoader classLoader = null;
            GroovyShell shell = null;
            try {
                // 创建编译配置
                CompilerConfiguration config = new CompilerConfiguration();
                config.setTargetDirectory((File) null);

                // 创建独立的 ClassLoader 和 GroovyShell
                classLoader = new GroovyClassLoader(getClass().getClassLoader(), config);
                shell = new GroovyShell(classLoader);

                Script runtime = shell.parse(script);
                runtime.setProperty("$bind", FlowScriptContext.getInstance());
                return (T) runtime.invokeMethod(method, args);
            } catch (Exception e) {
                LOGGER.error("Script execution error, method: {}, script: {}", method, script, e);
                throw new FlowExecutionException(
                        FlowExecutionException.scriptExecutionError(method, e).getErrorCode(),
                        "Script execution failed: " + e.getMessage(),
                        e
                );
            } finally {
                // 确保 GroovyShell 和 ClassLoader 实例被释放，帮助 GC
                shell = null;
                classLoader = null;
            }
        }
    }

    /**
     * 清理脚本锁缓存
     * <p>
     * 在以下情况下建议调用此方法：
     * 1. 应用关闭时
     * 2. 执行了大量不同的脚本后，避免内存占用过大
     */
    public static void clearLockCache() {
        int size = SCRIPT_LOCKS.size();
        int executionCount = EXECUTION_COUNTER.get();
        SCRIPT_LOCKS.clear();
        EXECUTION_COUNTER.set(0);
        LOGGER.debug("Cleared {} script locks, reset execution counter from {}", size, executionCount);
    }

    /**
     * 设置最大锁缓存数量
     * <p>
     * 当锁缓存数量超过此值时，将自动触发清理
     *
     * @param maxSize 最大锁缓存数量
     */
    public static void setMaxLockCacheSize(int maxSize) {
        if (maxSize <= 0) {
            throw new FlowValidationException("maxSize", "must be positive");
        }
        maxLockCacheSize = maxSize;
        LOGGER.info("Max lock cache size set to {}", maxSize);
    }

    /**
     * 获取当前锁缓存大小
     *
     * @return 当前锁缓存大小
     */
    public static int getLockCacheSize() {
        return SCRIPT_LOCKS.size();
    }

    /**
     * 获取脚本执行总次数
     *
     * @return 脚本执行总次数
     */
    public static int getExecutionCount() {
        return EXECUTION_COUNTER.get();
    }

    /**
     * 启用自动清理
     * <p>
     * 启用后，将定期清理脚本锁缓存，避免内存占用过大
     */
    public static synchronized void enableAutoCleanup() {
        if (!autoCleanupEnabled) {
            autoCleanupEnabled = true;
            startAutoCleanup();
            LOGGER.info("Auto-cleanup enabled");
        }
    }

    /**
     * 禁用自动清理
     * <p>
     * 禁用后，停止定时清理任务但仍保留阈值触发清理
     */
    public static synchronized void disableAutoCleanup() {
        if (autoCleanupEnabled) {
            autoCleanupEnabled = false;
            if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
                cleanupScheduler.shutdown();
                try {
                    if (!cleanupScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                        cleanupScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    cleanupScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            LOGGER.info("Auto-cleanup disabled");
        }
    }

    /**
     * 获取自动清理间隔（秒）
     *
     * @return 清理间隔（秒）
     */
    public static int getCleanupIntervalSeconds() {
        return CLEANUP_INTERVAL_SECONDS;
    }
}
