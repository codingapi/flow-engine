package com.codingapi.flow.api.controller;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.service.FlowService;
import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.response.Response;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import com.codingapi.springboot.framework.user.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cmd/record")
@AllArgsConstructor
public class FlowRecordController {

    private final FlowService flowService;

    @GetMapping("/detail")
    public Response detail(IdRequest request) {
        String id = request.getStringId();
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        return SingleResponse.of(flowService.detail(id, current));
    }

}
