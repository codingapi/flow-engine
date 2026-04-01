package com.codingapi.flow.api.controller;

import com.codingapi.flow.mock.MockInstance;
import com.codingapi.flow.mock.MockInstanceFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.*;
import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.pojo.response.FlowContent;
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


    @PostMapping("/action")
    public SingleResponse<ActionResponse> action(@RequestBody FlowActionRequest request) {
        FlowService flowService = this.loadFlowService();
        long operatorId = loadCurrentOperatorId();
        request.updateOperatorId(operatorId);
        ActionResponse response = flowService.action(request);
        return SingleResponse.of(response);
    }

}
