package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.script.node.SubProcessScript;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 子流程任务策略
 */
@Getter
@NoArgsConstructor
public class SubProcessStrategy extends BaseStrategy {

    /**
     *  是否创建后自动提交
     */
    private boolean submit;
    /**
     * 子流程触发脚本
     */
    private SubProcessScript subProcessScript;

    public SubProcessStrategy(String subProcessScript,boolean submit) {
        this.submit = submit;
        this.subProcessScript = new SubProcessScript(subProcessScript);
    }

    @Override
    public void copy(INodeStrategy target) {
        this.subProcessScript = ((SubProcessStrategy) target).subProcessScript;
    }

    public static SubProcessStrategy defaultStrategy() {
        SubProcessStrategy processStrategy = new SubProcessStrategy();
        processStrategy.subProcessScript = SubProcessScript.defaultScript();
        processStrategy.submit = true;
        return processStrategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", subProcessScript.getScript());
        map.put("submit", submit);
        return map;
    }

    public static SubProcessStrategy fromMap(Map<String, Object> map) {
        SubProcessStrategy processStrategy = IMapConvertor.fromMap(map, SubProcessStrategy.class);
        if (processStrategy == null) return null;
        processStrategy.subProcessScript = new SubProcessScript((String) map.get("script"));
        processStrategy.submit = (boolean) map.get("submit");
        return processStrategy;
    }

    public void execute(FlowSession session) {
        IRepositoryHolder repositoryHolder = session.getRepositoryHolder();
        FlowCreateRequest flowCreateRequest = subProcessScript.execute(session);
        FlowService flowService = repositoryHolder.createFlowService();
        long createRecordId = flowService.create(flowCreateRequest);
        if (submit) {
            flowService.action(flowCreateRequest.toActionRequest(createRecordId));
        }
    }
}
