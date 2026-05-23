package com.codingapi.flow.script.runtime;

import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.springboot.framework.script.GroovyScriptRunningContext;
import com.codingapi.springboot.framework.script.request.RuntimeBindObject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本运行时上下文
 *
 */
public class ScriptRuntimeContext {

    /**
     * 默认最大锁缓存数量
     */
    private static final int DEFAULT_MAX_LOCK_CACHE_SIZE = 10000;


    @Getter
    private final static ScriptRuntimeContext instance = new ScriptRuntimeContext();

    private ScriptRuntimeContext() {
        if(GroovyScriptRunningContext.getInstance().getMaxCacheSize()!= DEFAULT_MAX_LOCK_CACHE_SIZE) {
            GroovyScriptRunningContext.getInstance().setMaxCacheSize(DEFAULT_MAX_LOCK_CACHE_SIZE);
        }
    }

    public static void clearCache() {
        GroovyScriptRunningContext.getInstance().clearCache();
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
     * 执行脚本，脚本传入的$bind对象，对应{@link FlowScriptContext}对象，用于脚本运行时获取相关的服务数据能力。
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
    public <T> T execute(String method, String script, Class<T> returnType, Object... args) {
        List<RuntimeBindObject> bindObjects = new ArrayList<>();
        bindObjects.add(new RuntimeBindObject("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())));
        return GroovyScriptRunningContext.getInstance().run(method,script,returnType,bindObjects,args);
    }



    /**
     * 设置最大锁缓存数量
     * <p>
     * 当锁缓存数量超过此值时，将自动触发清理
     *
     * @param maxSize 最大锁缓存数量
     */
    public static void setMaxLockCacheSize(int maxSize) {
        GroovyScriptRunningContext.getInstance().setMaxCacheSize(maxSize);
    }

    /**
     * 获取当前锁缓存大小
     *
     * @return 当前锁缓存大小
     */
    public static int getLockCacheSize() {
        return GroovyScriptRunningContext.getInstance().getMaxCacheSize();
    }

}
