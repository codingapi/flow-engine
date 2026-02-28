package com.codingapi.flow.script.request;

import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.Setter;

/**
 * 条件表达式Groovy脚本请求对象
 * 提供给ConditionScript使用的上下文数据
 * 继承BaseGroovyRequest，从FlowSession自动提取通用数据
 */
@Getter
@Setter
public class ConditionGroovyRequest extends BaseGroovyRequest {

    /**
     * 当前节点名称
     */
    private String nodeName;

    /**
     * 从FlowSession构建ConditionGroovyRequest
     * @param session 流程会话（不能为null）
     */
    public ConditionGroovyRequest(FlowSession session) {
        super(session);
        // 提取节点信息
        if (session.getCurrentNode() != null) {
            this.nodeName = session.getCurrentNode().getName();
        }
    }
}
