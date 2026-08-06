package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.SubProcessScript;
import com.codingapi.flow.script.node.SubProcessResultScript;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * 子流程结果确认脚本。
     */
    private SubProcessResultScript resultScript;

    /**
     * 是否允许子流程参与人在节点记录中查看主流程历史。
     */
    private boolean showParentProcessRecords;

    public SubProcessStrategy(String subProcessScript, boolean submit) {
        this(subProcessScript, submit, SubProcessResultScript.defaultScript().getScript(), false);
    }

    public SubProcessStrategy(String subProcessScript, boolean submit, String resultScript) {
        this(subProcessScript, submit, resultScript, false);
    }

    public SubProcessStrategy(String subProcessScript,
                              boolean submit,
                              String resultScript,
                              boolean showParentProcessRecords) {
        this.submit = submit;
        this.subProcessScript = new SubProcessScript(subProcessScript);
        this.resultScript = new SubProcessResultScript(resultScript);
        this.showParentProcessRecords = showParentProcessRecords;
    }

    @Override
    public void copy(INodeStrategy target) {
        SubProcessStrategy strategy = (SubProcessStrategy) target;
        this.submit = strategy.submit;
        this.subProcessScript = strategy.subProcessScript;
        this.resultScript = strategy.resultScript;
        this.showParentProcessRecords = strategy.showParentProcessRecords;
    }

    public static SubProcessStrategy defaultStrategy() {
        SubProcessStrategy processStrategy = new SubProcessStrategy();
        processStrategy.subProcessScript = SubProcessScript.defaultScript();
        processStrategy.resultScript = SubProcessResultScript.defaultScript();
        processStrategy.submit = true;
        processStrategy.showParentProcessRecords = false;
        return processStrategy;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", subProcessScript.getScript());
        map.put("submit", submit);
        map.put("resultScript", resultScript.getScript());
        map.put("showParentProcessRecords", showParentProcessRecords);
        return map;
    }

    public static SubProcessStrategy fromMap(Map<String, Object> map) {
        SubProcessStrategy processStrategy = IMapConvertor.fromMap(map, SubProcessStrategy.class);
        if (processStrategy == null) return null;
        processStrategy.subProcessScript = new SubProcessScript((String) map.get("script"));
        processStrategy.submit = map.get("submit") == null || Boolean.parseBoolean(map.get("submit").toString());
        Object resultScript = map.get("resultScript");
        processStrategy.resultScript = resultScript == null
                ? SubProcessResultScript.defaultScript()
                : new SubProcessResultScript(resultScript.toString());
        Object showParentProcessRecords = map.get("showParentProcessRecords");
        processStrategy.showParentProcessRecords = showParentProcessRecords != null
                && Boolean.parseBoolean(showParentProcessRecords.toString());
        return processStrategy;
    }

    public void execute(FlowSession session) {
        IRepositoryHolder repositoryHolder = session.getRepositoryHolder();
        List<FlowCreateRequest> flowCreateRequests = subProcessScript.execute(session);
        verifySubProcessDepthAndLoop(session);
        FlowService flowService = repositoryHolder.createFlowService();
        List<FlowRecord> childRecords = new ArrayList<>();
        for (FlowCreateRequest flowCreateRequest : flowCreateRequests) {
            long createRecordId = flowService.create(flowCreateRequest);
            childRecords.add(repositoryHolder.getRecordById(createRecordId));
        }
        List<SubProcessRecord.Instance> instances = childRecords.stream()
                .map(SubProcessRecord.Instance::new)
                .toList();
        SubProcessRecord subProcessRecord = new SubProcessRecord(
                FlowIDGeneratorGatewayContext.getInstance().generateProcessId(),
                session.getCurrentRecord(),
                session.getCurrentNodeId(),
                instances);
        repositoryHolder.getSubProcessRepository().save(subProcessRecord);
        if (submit) {
            for (int i = 0; i < flowCreateRequests.size(); i++) {
                flowService.action(flowCreateRequests.get(i).toActionRequest(childRecords.get(i).getId()));
            }
        }
    }

    public boolean confirm(FlowSession session) {
        return resultScript.execute(session);
    }

    /**
     * 校验子流程创建是否构成循环或超过流程级最大嵌套深度。
     * <p>
     * 沿当前记录的 parentId 父链回溯：
     * <ul>
     *     <li>循环检测：若祖先记录上已存在同一子流程节点（同节点id、同运行时id）的执行记录，
     *     说明同一工作流实例链上该子流程节点被重复触发（创建自身或祖先流程），拒绝创建；</li>
     *     <li>深度校验：统计父链嵌套层数，创建后超过 {@link Workflow#getMaxNestDepth()} 时拒绝创建。</li>
     * </ul>
     *
     * @param session 触发子流程的会话
     */
    private void verifySubProcessDepthAndLoop(FlowSession session) {
        Workflow workflow = session.getWorkflow();
        int maxDepth = workflow.getMaxNestDepth();
        IRepositoryHolder repositoryHolder = session.getRepositoryHolder();
        FlowRecord currentRecord = session.getCurrentRecord();
        String subProcessNodeId = session.getCurrentNodeId();
        long workRuntimeId = session.getWorkflowRuntimeId();

        Set<Long> visited = new HashSet<>();
        FlowRecord ancestor = currentRecord;
        int depth = 1;
        while (ancestor != null) {
            if (!visited.add(ancestor.getId())) {
                // 数据异常防环兜底，避免父链成环导致的死循环
                break;
            }
            boolean sameSubProcessExecuted = repositoryHolder.getSubProcessRepository()
                    .findByParentRecordId(ancestor.getId()).stream()
                    .anyMatch(record -> record.getNodeId().equals(subProcessNodeId)
                            && record.getParentWorkRuntimeId() == workRuntimeId);
            if (sameSubProcessExecuted) {
                throw FlowExecutionException.subProcessLoop();
            }
            if (ancestor.getParentId() <= 0) {
                break;
            }
            ancestor = repositoryHolder.getRecordById(ancestor.getParentId());
            depth++;
        }
        if (depth + 1 > maxDepth) {
            throw FlowExecutionException.subProcessMaxDepth(maxDepth);
        }
    }
}
