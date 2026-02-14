package com.codingapi.flow.api.controller;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.service.FlowService;
import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.response.Response;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import com.codingapi.springboot.framework.user.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cmd/record")
@AllArgsConstructor
public class FlowRecordController {

    private final FlowService flowService;

    @GetMapping("/detail")
    public SingleResponse<FlowContent> detail(IdRequest request) {
        String id = request.getStringId();
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        return SingleResponse.of(flowService.detail(id, current));
    }


    @PostMapping("/create")
    public SingleResponse<Long> create(@RequestBody FlowCreateRequest request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        request.setOperatorId(current.getUserId());
        return SingleResponse.of(flowService.create(request));
    }


    @PostMapping("/action")
    public Response action(@RequestBody FlowActionRequest request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        request.updateOperatorId(current.getUserId());
        flowService.action(request);
        return Response.buildSuccess();
    }

}
