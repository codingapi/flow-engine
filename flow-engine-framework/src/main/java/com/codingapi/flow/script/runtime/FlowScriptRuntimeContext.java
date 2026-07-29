package com.codingapi.flow.script.runtime;

import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;

/**
 * 流程运行时脚本代理。
 * <p>
 * 脚本统一获取入口，用于隔离流程设计阶段与运行时：
 * <ul>
 *     <li>运行态(线程缓存中存在当前运行时的脚本快照)：优先使用快照内容构造一个
 *     独立的脚本对象返回，不会修改全局缓存中的脚本对象，从而避免设计期修改污染在途流程。</li>
 *     <li>非运行态(无运行时快照)：直接返回全局脚本缓存中的脚本对象，代理逻辑不干预，
 *     行为与原有实现完全一致。</li>
 * </ul>
 * 说明：脚本编译产物缓存以脚本内容的 SHA256 为键，因此使用同一脚本key执行快照内容
 * 时会按内容独立编译缓存，与设计期修改后的新内容互不串扰。
 */
public class FlowScriptRuntimeContext {

    private final static FlowScriptRuntimeContext instance = new FlowScriptRuntimeContext();

    public static FlowScriptRuntimeContext getInstance() {
        return instance;
    }

    /**
     * 获取脚本对象。运行时优先使用快照内容，否则走全局脚本缓存。
     *
     * @param key 脚本key
     * @return 脚本对象
     */
    public GroovyScript getGroovyScript(String key) {
        GroovyScript cached = GroovyScriptCacheContext.getInstance().getGroovyScript(key);
        String snapshot = FlowRuntimeScriptLocalCache.getInstance().get(key);
        if (snapshot == null) {
            // 非运行态：不干预，返回全局缓存中的脚本对象
            return cached;
        }
        // 运行态：使用运行时快照内容构造独立脚本对象，不修改全局缓存对象
        GroovyScript.Builder builder = GroovyScript.builder(key)
                .script(snapshot)
                .method(cached != null && cached.getMethod() != null ? cached.getMethod() : "run");
        if (cached != null) {
            builder.returnType(cached.getReturnType())
                    .typeOne(cached.getTypeOne())
                    .typeTwo(cached.getTypeTwo())
                    .binds(cached.getBinds())
                    .requests(cached.getRequests());
        }
        return builder.build();
    }
}
