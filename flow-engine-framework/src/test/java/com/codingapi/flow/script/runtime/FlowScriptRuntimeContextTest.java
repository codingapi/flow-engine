package com.codingapi.flow.script.runtime;

import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程运行时脚本代理测试。
 * <p>
 * 验证：运行态优先使用运行时快照内容，且不影响全局脚本缓存；非运行态走原路径。
 */
class FlowScriptRuntimeContextTest {

    private static final String KEY = "test-script-key";

    @AfterEach
    void tearDown() {
        FlowRuntimeScriptLocalCache.getInstance().clear();
        GroovyScriptCacheContext.getInstance().remove(KEY);
    }

    private void registerGlobalScript(String content) {
        GroovyScript script = GroovyScript.builder(KEY)
                .script(content)
                .method("run")
                .returnType(String.class)
                .build();
        GroovyScriptCacheContext.getInstance().cache(script);
    }

    private static final String NEW_CONTENT = "def run(request){ return 'NEW' }";
    private static final String OLD_CONTENT = "def run(request){ return 'OLD' }";

    @Test
    void should_return_snapshot_content_when_runtime_cache_present() {
        // given 设计期将脚本内容改为 NEW
        registerGlobalScript(NEW_CONTENT);
        // 运行时快照固化的是旧内容 OLD
        FlowRuntimeScriptLocalCache.getInstance().set(Map.of(KEY, OLD_CONTENT));

        // when 运行态获取脚本
        GroovyScript script = FlowScriptRuntimeContext.getInstance().getGroovyScript(KEY);

        // then 使用快照内容
        assertEquals(OLD_CONTENT, script.getScript());
        // 且全局缓存对象未被修改
        assertEquals(NEW_CONTENT, GroovyScriptCacheContext.getInstance().getScript(KEY));
    }

    @Test
    void should_execute_snapshot_content_not_design_content() {
        // given
        registerGlobalScript(NEW_CONTENT);
        FlowRuntimeScriptLocalCache.getInstance().set(Map.of(KEY, OLD_CONTENT));

        // when 运行态执行脚本
        String runtimeResult = FlowScriptRuntimeContext.getInstance().getGroovyScript(KEY).invoke("request");
        // 设计期(无快照)执行同一脚本key
        FlowRuntimeScriptLocalCache.getInstance().clear();
        String designResult = FlowScriptRuntimeContext.getInstance().getGroovyScript(KEY).invoke("request");

        // then 运行时执行快照内容, 设计期执行最新内容, 互不串扰
        assertEquals("OLD", runtimeResult);
        assertEquals("NEW", designResult);
    }

    @Test
    void should_execute_snapshot_after_design_script_deleted() {
        // given 运行时已固化脚本正文，随后设计态脚本被物理删除
        registerGlobalScript(NEW_CONTENT);
        FlowRuntimeScriptLocalCache.getInstance().set(Map.of(KEY, OLD_CONTENT));
        GroovyScriptCacheContext.getInstance().remove(KEY);

        // when
        GroovyScript runtimeScript = FlowScriptRuntimeContext.getInstance().getGroovyScript(KEY);

        // then 不再依赖设计态脚本对象，仍可使用运行时正文执行
        assertEquals(OLD_CONTENT, runtimeScript.getScript());
        assertEquals("OLD", runtimeScript.invoke("request"));
    }

    @Test
    void should_return_global_script_when_no_runtime_cache() {
        // given 无运行时快照
        registerGlobalScript(NEW_CONTENT);
        FlowRuntimeScriptLocalCache.getInstance().clear();

        // when
        GroovyScript script = FlowScriptRuntimeContext.getInstance().getGroovyScript(KEY);

        // then 代理不干预, 直接返回全局缓存中的同一对象
        assertSame(GroovyScriptCacheContext.getInstance().getGroovyScript(KEY), script);
        assertEquals(NEW_CONTENT, script.getScript());
    }

    @Test
    void should_throw_clear_exception_when_script_missing_everywhere() {
        // given 脚本在任何缓存与仓储中都不存在（模拟脚本未持久化或数据丢失）
        String missingKey = "missing-script-" + System.nanoTime();
        FlowRuntimeScriptLocalCache.getInstance().clear();

        // when / then 不再返回 null 导致调用方空指针，而是抛出明确的流程状态异常
        FlowStateException exception = assertThrows(FlowStateException.class,
                () -> FlowScriptRuntimeContext.getInstance().getGroovyScript(missingKey));
        assertTrue(exception.getMessage().contains(missingKey),
                "异常信息应包含缺失的脚本key，便于定位");
    }

    @Test
    void should_throw_clear_exception_when_runtime_snapshot_missing_key() {
        // given 存在运行时快照，但快照中不包含目标脚本key，且全局缓存也没有
        String missingKey = "missing-runtime-script-" + System.nanoTime();
        FlowRuntimeScriptLocalCache.getInstance().set(Map.of(KEY, OLD_CONTENT));

        // when / then
        assertThrows(FlowStateException.class,
                () -> FlowScriptRuntimeContext.getInstance().getGroovyScript(missingKey));
    }

    @Test
    void should_return_null_from_local_cache_when_not_set() {
        // given
        FlowRuntimeScriptLocalCache.getInstance().clear();

        // when / then
        assertNull(FlowRuntimeScriptLocalCache.getInstance().get(KEY));

        // set(null) 等价于清理
        FlowRuntimeScriptLocalCache.getInstance().set(Map.of(KEY, "x"));
        FlowRuntimeScriptLocalCache.getInstance().set(null);
        assertNull(FlowRuntimeScriptLocalCache.getInstance().get(KEY));
    }
}
