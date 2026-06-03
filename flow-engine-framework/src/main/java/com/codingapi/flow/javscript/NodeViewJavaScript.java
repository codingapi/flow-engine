package com.codingapi.flow.javscript;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 视图代码
 */
@Data
@AllArgsConstructor
public class NodeViewJavaScript {

    /**
     * 代码唯一标识
     */
    private String code;
    /**
     * 代码内容
     */
    private String script;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 更新时间
     */
    private long updateTime;

    /**
     * 保存数据
     */
    public void save() {
        NodeViewJavaScriptCacheContext.getInstance().save(this);
    }

    public NodeViewJavaScript copy(String code) {
        return new NodeViewJavaScript(code, script,createTime,updateTime);
    }

    public void update(String script) {
        this.script = script;
        this.updateTime = System.currentTimeMillis();
    }
}
