package com.codingapi.flow.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程运行时脚本线程级缓存。
 * <p>
 * 在一次流程操作内缓存当前运行时的脚本快照(脚本key -&gt; 脚本内容)，
 * 供脚本代理({@code FlowScriptRuntimeContext})优先读取，从而隔离流程设计阶段
 * 对脚本内容的修改对已经在运行的流程造成的影响。
 * <p>
 * 使用约定与 {@link FlowOperatorLocalThreadCache} 一致：
 * 在 {@code FlowService} 各入口及延迟任务执行前调用 {@link #clear()} 清理，
 * 各 {@code FlowXxxService} 加载运行时快照后调用 {@link #set(Map)} 装载。
 */
public class FlowRuntimeScriptLocalCache {

    private final ThreadLocal<Map<String, String>> cache;

    private FlowRuntimeScriptLocalCache() {
        this.cache = new ThreadLocal<>();
    }

    private final static FlowRuntimeScriptLocalCache instance = new FlowRuntimeScriptLocalCache();

    public static FlowRuntimeScriptLocalCache getInstance() {
        return instance;
    }

    /**
     * 清理当前线程的脚本快照缓存
     */
    public void clear() {
        this.cache.remove();
    }

    /**
     * 装载当前运行时的脚本快照
     *
     * @param scripts 脚本key到脚本内容的映射，为 null 时清理缓存
     */
    public void set(Map<String, String> scripts) {
        if (scripts == null) {
            this.cache.remove();
            return;
        }
        this.cache.set(new HashMap<>(scripts));
    }

    /**
     * 获取运行时快照中的脚本内容
     *
     * @param key 脚本key
     * @return 快照中的脚本内容，无运行时快照或无该key时返回 null
     */
    public String get(String key) {
        Map<String, String> scripts = this.cache.get();
        if (scripts == null) {
            return null;
        }
        return scripts.get(key);
    }
}
