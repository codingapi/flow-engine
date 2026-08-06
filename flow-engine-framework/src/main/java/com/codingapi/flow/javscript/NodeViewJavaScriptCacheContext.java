package com.codingapi.flow.javscript;

import com.codingapi.flow.repository.NodeViewJavaScriptRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NodeViewJavaScriptCacheContext {

    // 最大缓存时间15分钟
    public static final long MAX_CACHE_TIME = 1000 * 60 * 15;

    /**
     * 共享清理调度线程，替代每个缓存项一个 Timer 原生线程。
     */
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "node-view-javascript-clear");
                thread.setDaemon(true);
                return thread;
            });

    private final Map<String, NodeViewJavaScriptClearJob> cache;

    @Setter
    private NodeViewJavaScriptRepository nodeViewJavaScriptRepository;

    @Getter
    private final static NodeViewJavaScriptCacheContext instance = new NodeViewJavaScriptCacheContext();

    private NodeViewJavaScriptCacheContext() {
        this.cache = new HashMap<>();
    }


    public void cache(String code, String script) {
        if (StringUtils.hasText(code) && StringUtils.hasText(script)) {
            NodeViewJavaScript javaScript = this.get(code);
            if (javaScript == null) {
                javaScript = new NodeViewJavaScript(code, script, System.currentTimeMillis(), System.currentTimeMillis());
            }
            javaScript.update(script);
            NodeViewJavaScriptClearJob previous = this.cache.put(
                    code, new NodeViewJavaScriptClearJob(javaScript, MAX_CACHE_TIME));
            if (previous != null) {
                previous.cancel();
            }
        }
    }


    public void save(NodeViewJavaScript javaScript) {
        if (nodeViewJavaScriptRepository != null) {
            nodeViewJavaScriptRepository.save(javaScript);
            this.remove(javaScript.getCode());
        }
    }

    public NodeViewJavaScript get(String code) {
        NodeViewJavaScriptClearJob job = this.cache.get(code);
        if (job != null) {
            return job.getJavaScript();
        }
        if (nodeViewJavaScriptRepository != null) {
            return nodeViewJavaScriptRepository.get(code);
        }
        return null;
    }


    public void remove(String code) {
        NodeViewJavaScriptClearJob job = this.cache.remove(code);
        if (job != null) {
            job.cancel();
        }
    }


    public void delete(String code) {
        this.remove(code);
        if (nodeViewJavaScriptRepository != null) {
            nodeViewJavaScriptRepository.delete(code);
        }
    }


    public static class NodeViewJavaScriptClearJob {

        @Getter
        private final NodeViewJavaScript javaScript;
        private final ScheduledFuture<?> future;

        public NodeViewJavaScriptClearJob(NodeViewJavaScript javaScript, long clearDelayMillis) {
            this.javaScript = javaScript;
            this.future = SCHEDULER.schedule(
                    () -> NodeViewJavaScriptCacheContext.getInstance().remove(javaScript.getCode()),
                    clearDelayMillis,
                    TimeUnit.MILLISECONDS);
        }

        public void cancel() {
            this.future.cancel(false);
        }
    }
}
