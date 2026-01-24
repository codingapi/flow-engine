package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.script.node.SubProcessScript;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 子流程任务策略
 */
@Getter
@NoArgsConstructor
public class SubProcessStrategy extends BaseStrategy {

    private boolean submit;
    private SubProcessScript subProcessScript;

    public void setTriggerScript(String script) {
        this.subProcessScript = new SubProcessScript(script);
    }

    @Override
    public void copy(INodeStrategy target) {
        this.subProcessScript = ((SubProcessStrategy) target).subProcessScript;
    }

    public static SubProcessStrategy defaultStrategy() {
        SubProcessStrategy processStrategy = new SubProcessStrategy();
        processStrategy.setTriggerScript(SubProcessScript.SCRIPT_DEFAULT);
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
        FlowCreateRequest flowCreateRequest = subProcessScript.execute(session);
        FlowService flowService = RepositoryHolderContext.getInstance().createFlowService();
        long createRecordId = flowService.create(flowCreateRequest);
        if (submit) {
            flowService.action(flowCreateRequest.toActionRequest(createRecordId));
        }
    }
}
