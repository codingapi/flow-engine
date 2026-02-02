package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.script.node.RouterNodeScript;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 路由策略
 */
@Getter
@NoArgsConstructor
public class RouterStrategy extends BaseStrategy {

    private RouterNodeScript routerNodeScript;

    public RouterStrategy(String script) {
        this.routerNodeScript = new RouterNodeScript(script);
    }

    public void setRouterNodeScript(String script) {
        this.routerNodeScript = new RouterNodeScript(script);
    }

    public String execute(FlowSession request){
        return routerNodeScript.execute(request);
    }

    @Override
    public void copy(INodeStrategy target) {
        this.routerNodeScript = ((RouterStrategy) target).routerNodeScript;
    }

    public static RouterStrategy defaultStrategy() {
        RouterStrategy routerStrategy = new RouterStrategy();
        routerStrategy.setRouterNodeScript(RouterNodeScript.SCRIPT_NODE_DEFAULT);
        return routerStrategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", routerNodeScript.getScript());
        return map;
    }

    public static RouterStrategy fromMap(Map<String, Object> map) {
        RouterStrategy delayStrategy = IMapConvertor.fromMap(map, RouterStrategy.class);
        if (delayStrategy == null) return null;
        delayStrategy.setRouterNodeScript((String) map.get("script"));
        return delayStrategy;
    }
}
