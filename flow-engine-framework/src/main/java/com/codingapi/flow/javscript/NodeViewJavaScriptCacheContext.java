package com.codingapi.flow.javscript;

import com.codingapi.flow.repository.NodeViewJavaScriptRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class NodeViewJavaScriptCacheContext {

    // 最大缓存时间15分钟
    public static final long MAX_CACHE_TIME = 1000 * 60 * 15;

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
            this.cache.put(code, new NodeViewJavaScriptClearJob(javaScript, System.currentTimeMillis() + MAX_CACHE_TIME));
        }
    }


    public void save(NodeViewJavaScript javaScript) {
        if (nodeViewJavaScriptRepository != null) {
            nodeViewJavaScriptRepository.save(javaScript);
            this.cache.remove(javaScript.getCode());
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
        this.cache.remove(code);
    }


    public void delete(String code) {
        this.cache.remove(code);
        if (nodeViewJavaScriptRepository != null) {
            nodeViewJavaScriptRepository.delete(code);
        }
    }


    public static class NodeViewJavaScriptClearJob {

        @Getter
        private final NodeViewJavaScript javaScript;
        private final long clearTime;
        private final Timer timer;

        public NodeViewJavaScriptClearJob(NodeViewJavaScript javaScript, long clearTime) {
            this.javaScript = javaScript;
            this.clearTime = clearTime;
            this.timer = new Timer();

            this.initTimer();
        }


        private void initTimer() {
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    NodeViewJavaScriptCacheContext.getInstance().remove(javaScript.getCode());
                }
            }, this.clearTime);
        }
    }
}
