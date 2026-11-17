package com.codingapi.flow.api.controller;

import com.codingapi.flow.mock.MockInstance;
import com.codingapi.flow.mock.MockInstanceFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.*;
import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.FlowForceSyncResult;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.service.FlowService;
import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.response.MultiResponse;
import com.codingapi.springboot.framework.dto.response.Response;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import com.codingapi.springboot.framework.user.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/api/cmd/record")
@AllArgsConstructor
public class FlowRecordController {

    private final FlowService flowService;

    private FlowService loadFlowService() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String key = request.getParameter("mockKey");
        if (StringUtils.hasText(key)) {
            MockInstance mockInstance = MockInstanceFactory.getInstance().getMockInstance(key);
            if (mockInstance != null) {
                return mockInstance.getFlowService();
            }
        }
        return this.flowService;
    }

    private long loadCurrentOperatorId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String key = request.getParameter("operatorId");
        if (StringUtils.hasText(key)) {
            return Long.parseLong(key);
        } else {
            IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
            return current.getUserId();
        }
    }

    @GetMapping("/detail")
    public SingleResponse<FlowContent> detail(IdRequest idRequest) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        return SingleResponse.of(flowService.detail(new FlowDetailRequest(idRequest.getStringId(), operatorId)));
    }


    @PostMapping("/processNodes")
    public MultiResponse<ProcessNode> processNodes(@RequestBody FlowProcessNodeRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        request.setOperatorId(operatorId);
        return MultiResponse.of(flowService.processNodes(request));
    }

    @PostMapping("/create")
    public SingleResponse<Long> create(@RequestBody FlowCreateRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        request.setOperatorId(operatorId);
        return SingleResponse.of(flowService.create(request));
    }


    @PostMapping("/urge")
    public Response urge(@RequestBody IdRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        flowService.urge(new FlowUrgeRequest(request.getLongId(), operatorId));
        return Response.buildSuccess();
    }

    @PostMapping("/revoke")
    public Response revoke(@RequestBody IdRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        flowService.revoke(new FlowRevokeRequest(request.getLongId(), operatorId));
        return Response.buildSuccess();
    }


    @PostMapping("/delete")
    public Response delete(@RequestBody IdRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        flowService.delete(new FlowDeleteRequest(request.getLongId(), operatorId));
        return Response.buildSuccess();
    }


    @PostMapping("/action")
    public SingleResponse<ActionResponse> action(@RequestBody FlowActionRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        request.updateOperatorId(operatorId);
        ActionResponse response = flowService.action(request);
        return SingleResponse.of(response);
    }

    /**
     * 子流程数据重置（独立接口，不属于常规审批动作）。
     * <p>仅当子流程节点开启重置能力且当前记录位于其下游时可调用，
     * 详情数据中的 {@code resetSubProcess} 标识字段表明当前记录是否具备该能力。</p>
     */
    @PostMapping("/subProcess/reset")
    public Response resetSubProcess(@RequestBody FlowSubProcessResetRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        request.setOperatorId(operatorId);
        flowService.resetSubProcess(request);
        return Response.buildSuccess();
    }

    /**
     * 已完成流程二次发起（独立接口，不属于常规审批动作）。
     * <p>仅流程管理员可对已正常完成的流程记录执行，详情数据中的 {@code reissue} 标识字段
     * 表明当前记录是否具备该能力。新流程停在开始节点成为发起人的待办，源流程记录不做改写。</p>
     */
    @PostMapping("/reissue")
    public SingleResponse<Long> reissue(@RequestBody FlowReissueRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        request.setOperatorId(operatorId);
        return SingleResponse.of(flowService.reissue(request));
    }

    /**
     * 运行中流程实例强制同步最新流程设计（独立接口，不属于常规审批动作）。
     * <p>只对请求中选中的实例生效，未选中的实例继续使用原运行时快照；
     * 同步前会校验结构一致性（不允许新增/删除节点、改变节点类型/层级/顺序，旧按钮动作必须保留）。</p>
     */
    @PostMapping("/forceSync")
    public SingleResponse<FlowForceSyncResult> forceSync(@RequestBody FlowForceSyncRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        request.setOperatorId(operatorId);
        return SingleResponse.of(flowService.forceSync(request));
    }

}
