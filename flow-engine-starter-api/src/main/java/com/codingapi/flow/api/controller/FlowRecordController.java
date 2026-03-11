package com.codingapi.flow.api.controller;

import com.codingapi.flow.mock.FlowServiceMockFactory;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.*;
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

    private FlowService loadFlowService(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String key = request.getParameter("mockKey");
        if(StringUtils.hasText(key)) {
            return FlowServiceMockFactory.getInstance().getFlowService(key);
        }else {
            return this.flowService;
        }
    }

    @GetMapping("/detail")
    public SingleResponse<FlowContent> detail(IdRequest idRequest) {
        FlowService flowService = this.loadFlowService();
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        return SingleResponse.of(flowService.detail(new FlowDetailRequest(idRequest.getStringId(), current.getUserId())));
    }


    @PostMapping("/processNodes")
    public MultiResponse<ProcessNode> processNodes(@RequestBody FlowProcessNodeRequest request) {
        FlowService flowService = this.loadFlowService();
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        request.setOperatorId(current.getUserId());
        return MultiResponse.of(flowService.processNodes(request));
    }

    @PostMapping("/create")
    public SingleResponse<Long> create(@RequestBody FlowCreateRequest request) {
        FlowService flowService = this.loadFlowService();
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        request.setOperatorId(current.getUserId());
        return SingleResponse.of(flowService.create(request));
    }


    @PostMapping("/urge")
    public Response urge(@RequestBody IdRequest request) {
        FlowService flowService = this.loadFlowService();
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        flowService.urge(new FlowUrgeRequest(request.getLongId(),current.getUserId()));
        return Response.buildSuccess();
    }

    @PostMapping("/revoke")
    public Response revoke(@RequestBody IdRequest request) {
        FlowService flowService = this.loadFlowService();
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        flowService.revoke(new FlowRevokeRequest(request.getLongId(),current.getUserId()));
        return Response.buildSuccess();
    }


    @PostMapping("/action")
    public Response action(@RequestBody FlowActionRequest request) {
        FlowService flowService = this.loadFlowService();
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        request.updateOperatorId(current.getUserId());
        flowService.action(request);
        return Response.buildSuccess();
    }

}
