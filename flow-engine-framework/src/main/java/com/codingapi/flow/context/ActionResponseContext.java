package com.codingapi.flow.context;

import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.pojo.response.NodeOption;
import lombok.Getter;

import java.util.List;

public class ActionResponseContext {

    @Getter
    private final static ActionResponseContext instance = new ActionResponseContext();

    private final ThreadLocal<ActionResponse> threadLocal;


    private ActionResponseContext() {
        this.threadLocal = new InheritableThreadLocal<>();
    }


    public void clear() {
        this.threadLocal.remove();
    }

    public void set(List<NodeOption> options) {
        this.threadLocal.set(new ActionResponse(options));
    }

    public ActionResponse get() {
        return threadLocal.get();
    }


}
