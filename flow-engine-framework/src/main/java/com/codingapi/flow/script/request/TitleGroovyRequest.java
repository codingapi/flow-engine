package com.codingapi.flow.script.request;

import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.Setter;

/**
 * 标题表达式Groovy脚本请求对象
 * 提供给NodeTitleScript使用的上下文数据
 * 继承BaseGroovyRequest，从FlowSession自动提取通用数据
 */
@Getter
@Setter
public class TitleGroovyRequest extends BaseGroovyRequest {

    /**
     * 当前节点名称
     */
    private String nodeName;

    /**
     * 当前节点类型
     */
    private String nodeType;

    /**
     * 从FlowSession构建TitleGroovyRequest
     * @param session 流程会话
     */
    public TitleGroovyRequest(FlowSession session) {
        super(session);
        // 提取节点信息
        if (session != null && session.getCurrentNode() != null) {
            this.nodeName = session.getCurrentNode().getName();
            this.nodeType = session.getCurrentNode().getType();
        }
        // 提取流程编号（从record获取）
        if (session != null && session.getCurrentRecord() != null) {
            this.workCode = session.getCurrentRecord().getWorkCode();
        }
    }
}
