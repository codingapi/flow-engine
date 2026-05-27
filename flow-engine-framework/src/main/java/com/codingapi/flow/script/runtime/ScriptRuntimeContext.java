package com.codingapi.flow.script.runtime;

import com.codingapi.springboot.script.GroovyScriptRunner;
import com.codingapi.springboot.script.GroovyScriptRunningContext;
import lombok.Getter;

/**
 * 脚本运行时上下文
 *
 */
public class ScriptRuntimeContext {

    /**
     * 默认最大锁缓存数量
     */
    private static final int DEFAULT_MAX_LOCK_CACHE_SIZE = 10000;

    private GroovyScriptRunner scriptRunner;


    @Getter
    private final static ScriptRuntimeContext instance = new ScriptRuntimeContext();

    private ScriptRuntimeContext() {
        this.scriptRunner = new GroovyScriptRunner(DEFAULT_MAX_LOCK_CACHE_SIZE);
    }

    public void clearCache() {
        scriptRunner.clearCache();
    }


    /**
     * 执行脚本
     * <p>
     * 线程安全：使用脚本哈希值进行细粒度同步
     * 资源管理：执行完成后确保资源被释放
     *
     * @param request 请求对象
     * @return 脚本执行结果
     * @throws com.codingapi.flow.exception.FlowExecutionException 脚本执行失败时抛出
     */
    public <T> T execute(ScriptRuntimeRequest request) {
        return (T)GroovyScriptRunningContext.getInstance().invoke(request.getRunningScript());
    }


    /**
     * 设置最大锁缓存数量
     * <p>
     * 当锁缓存数量超过此值时，将自动触发清理
     *
     * @param maxSize 最大锁缓存数量
     */
    public void setMaxLockCacheSize(int maxSize) {
        this.scriptRunner = new GroovyScriptRunner(maxSize);
    }

    /**
     * 获取当前锁缓存大小
     *
     * @return 当前锁缓存大小
     */
    public int getLockCacheSize() {
        return this.scriptRunner.getMaxCacheSize();
    }

}
