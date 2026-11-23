package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.domain.FlowReissueRecord;
import com.codingapi.flow.event.FlowEventPublisher;
import com.codingapi.flow.event.FlowRecordReissueEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowPermissionException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorKey;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowReissueRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowReissueRepository;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.OperatorSelectType;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 已完成流程的二次发起服务（独立接口，不属于常规审批动作）。
 *
 * <p>二次发起以「已正常完成（flowState = FINISH）的流程记录」为目标，仅流程管理员可执行。
 * 处理方式为复制源流程记录的表单数据，重新走一遍标准的流程创建过程：新流程拥有独立流程id，
 * 创建后停在开始节点成为原发起人的待办，由发起人调整数据后重新提交执行。
 * 源流程记录、待办与历史记录不做任何改写，重发关系通过 {@link FlowReissueRecord} 单独留痕。</p>
 */
public class FlowReissueService {

    private final FlowReissueRequest request;
    private final IRepositoryHolder repositoryHolder;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;
    private final FlowReissueRepository reissueRepository;

    public FlowReissueService(FlowReissueRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.repositoryHolder = repositoryHolder;
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.reissueRepository = repositoryHolder.getFlowReissueRepository();
    }

    /**
     * 判断指定流程记录对当前操作人是否具备二次发起能力。
     *
     * <p>仅「已正常完成」的流程记录且操作人为流程管理员时开放；终止（未通过结束）、
     * 运行中、异常与已删除的流程均不允许二次发起。</p>
     *
     * @param flowRecord    流程记录
     * @param currentOperator 当前操作人
     * @return 是否可二次发起
     */
    public static boolean canReissue(FlowRecord flowRecord, IFlowOperator currentOperator) {
        if (flowRecord == null || currentOperator == null) {
            return false;
        }
        return currentOperator.isFlowManager()
                && flowRecord.getFlowState() == FlowRecord.SATE_FLOW_FINISH;
    }

    /**
     * 执行二次发起。
     *
     * @return 新流程的开始节点记录id
     */
    public long reissue() {
        request.verify();

        FlowRecord sourceRecord = flowRecordService.getFlowRecord(request.getSourceRecordId());
        if (sourceRecord == null) {
            throw FlowNotFoundException.record(request.getSourceRecordId());
        }

        IFlowOperator operator = repositoryHolder.getOperatorById(request.getOperatorId(), request.getOperatorIdentity());
        if (operator == null) {
            throw FlowNotFoundException.operator(request.getOperatorId());
        }
        if (!operator.isFlowManager()) {
            throw FlowPermissionException.accessDenied("reissue flow record");
        }
        if (sourceRecord.getFlowState() != FlowRecord.SATE_FLOW_FINISH) {
            throw FlowStateException.recordNotSupportReissue();
        }

        WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(sourceRecord.getWorkRuntimeId());
        if (workflowRuntime == null) {
            throw FlowNotFoundException.workflow(sourceRecord.getWorkRuntimeId() + " not found");
        }
        // 运行时快照不携带启用状态，流程是否被禁用交由创建环节按最新流程定义校验
        Workflow workflow = workflowRuntime.toWorkflow();

        // 新流程沿用源流程的发起人，二次发起后的待办回到发起人，由发起人调整数据后重新提交
        long initiatorId = sourceRecord.getCreateOperatorId();
        if (repositoryHolder.getOperatorById(initiatorId, sourceRecord.getCreateOperatorIdentity()) == null) {
            throw FlowNotFoundException.operator(initiatorId);
        }

        FlowCreateRequest createRequest = this.buildCreateRequest(workflow, sourceRecord);
        long newRecordId = repositoryHolder.createFlowService().create(createRequest);
        FlowRecord newRecord = flowRecordService.getFlowRecord(newRecordId);
        if (newRecord == null) {
            throw FlowNotFoundException.record(newRecordId);
        }

        FlowReissueRecord reissueRecord = new FlowReissueRecord(
                0,
                sourceRecord.getId(),
                sourceRecord.getProcessId(),
                newRecord.getId(),
                newRecord.getProcessId(),
                workflow.getCode(),
                sourceRecord.getWorkRuntimeId(),
                operator.getUserId(),
                operator.getName(),
                System.currentTimeMillis(),
                request.getAdvice());
        reissueRepository.save(reissueRecord);

        boolean mock = repositoryHolder instanceof MockRepositoryHolder;
        FlowEventPublisher.push(new FlowRecordReissueEvent(sourceRecord, newRecord, operator, mock));
        return newRecordId;
    }

    /**
     * 构建二次发起的流程创建请求。
     *
     * <p>创建动作用开始节点的保存动作（不存在时退化为开始节点的首个动作），
     * 保证新流程创建后停在开始节点成为待办，不会自动提交到下一节点。</p>
     */
    private FlowCreateRequest buildCreateRequest(Workflow workflow, FlowRecord sourceRecord) {
        StartNode startNode = (StartNode) workflow.getStartNode();
        IFlowAction action = startNode.actionManager().getActionByType(ActionType.SAVE.name());
        if (action == null) {
            action = startNode.actionManager().getFirstAction();
        }

        FlowCreateRequest createRequest = new FlowCreateRequest();
        createRequest.setWorkCode(workflow.getCode());
        createRequest.setActionId(action.id());
        createRequest.setOperatorId(sourceRecord.getCreateOperatorId());
        // 沿用源流程发起人的身份，保证二次发起后的参与人与源流程一致（issue #288）
        createRequest.setOperatorIdentity(sourceRecord.getCreateOperatorIdentity());
        createRequest.setFormData(this.loadFormData(sourceRecord));
        // 沿用源流程的自定义标题（如子流程实例区分标题），保持重发前后的业务标识一致
        if (StringUtils.hasText(sourceRecord.getWorkTitle())) {
            createRequest.setWorkTitle(sourceRecord.getWorkTitle());
        }
        createRequest.setOperatorSelectOperators(this.loadOperatorSelectOperators(workflow, sourceRecord.getProcessId()));
        return createRequest;
    }

    /**
     * 复制源流程记录的表单数据；请求中提供了表单数据时以其为准。
     */
    private Map<String, Object> loadFormData(FlowRecord sourceRecord) {
        Map<String, Object> requestFormData = request.getFormData();
        if (requestFormData != null && !requestFormData.isEmpty()) {
            return new HashMap<>(requestFormData);
        }
        Map<String, Object> sourceFormData = sourceRecord.getFormData();
        return sourceFormData == null ? new HashMap<>() : new HashMap<>(sourceFormData);
    }

    /**
     * 复制源流程「发起人设定 / 审批人设定」节点的操作人分配（含身份），保证重走时人员设定与首次发起一致。
     *
     * <p>身份必须一起复制：同一人员的两个身份是两项参与人，只复制 id 会让重走后的审批人退化到默认身份
     * （issue #288 衍生 / #292）。
     */
    private Map<String, List<OperatorKey>> loadOperatorSelectOperators(Workflow workflow, String sourceProcessId) {
        Map<String, List<OperatorKey>> operatorSelectOperators = new HashMap<>();
        for (IFlowNode node : workflow.getNodes()) {
            OperatorLoadStrategy strategy = node.strategyManager().getStrategy(OperatorLoadStrategy.class);
            if (strategy == null || strategy.getSelectType() == OperatorSelectType.SCRIPT) {
                continue;
            }
            List<OperatorKey> operators = repositoryHolder.findAssignedOperators(sourceProcessId, node.getId());
            if (operators != null && !operators.isEmpty()) {
                operatorSelectOperators.put(node.getId(), operators);
            }
        }
        return operatorSelectOperators;
    }
}
