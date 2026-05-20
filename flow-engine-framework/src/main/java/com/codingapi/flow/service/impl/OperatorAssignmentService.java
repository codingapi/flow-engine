package com.codingapi.flow.service.impl;

import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 操作人分配服务
 * <p>
 * 负责发起人设定 / 审批人设定时的操作人分配落库，并在节点配置了可选人员范围时校验所选人员是否越界。
 */
public class OperatorAssignmentService {

    private OperatorAssignmentService() {
    }

    /**
     * 校验并保存操作人分配。
     * <p>
     * 当目标节点配置了可选人员范围（范围非空）时，校验所选人员是否全部落在范围内，越界则抛出异常；
     * 范围为空（未配置脚本或脚本执行结果为空）表示不限范围，跳过校验。
     *
     * @param baseSession       基准会话，用于派生目标节点会话以执行范围脚本
     * @param processId         流程实例ID
     * @param operatorSelectMap 节点ID -> 选定的操作人ID列表
     */
    public static void validateAndSave(FlowSession baseSession, String processId,
                                       Map<String, List<Long>> operatorSelectMap) {
        if (operatorSelectMap == null || operatorSelectMap.isEmpty()) {
            return;
        }
        Workflow workflow = baseSession.getWorkflow();
        IRepositoryHolder repositoryHolder = baseSession.getRepositoryHolder();
        operatorSelectMap.forEach((nodeId, operatorIds) -> {
            IFlowNode node = workflow.getFlowNode(nodeId);
            List<IFlowOperator> range = node.strategyManager()
                    .loadOperatorRange(baseSession.updateSession(node));
            if (range != null && !range.isEmpty()) {
                Set<Long> allowedIds = range.stream()
                        .map(IFlowOperator::getUserId)
                        .collect(Collectors.toSet());
                for (Long operatorId : operatorIds) {
                    if (!allowedIds.contains(operatorId)) {
                        throw FlowValidationException.operatorOutOfRange(nodeId);
                    }
                }
            }
            repositoryHolder.saveOperatorAssignment(processId, nodeId, operatorIds);
        });
    }
}
