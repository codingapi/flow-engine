package com.codingapi.flow.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.response.Response;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import com.codingapi.springboot.framework.user.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/cmd/workflow")
@AllArgsConstructor
public class WorkflowController {

    private final WorkflowRepository workflowRepository;

    @PostMapping("/remove")
    public Response remove(@RequestBody IdRequest request) {
        Workflow workflow = workflowRepository.get(request.getStringId());
        workflowRepository.delete(workflow);
        return Response.buildSuccess();
    }

    @PostMapping("/create")
    public SingleResponse<JSONObject> create() {
        Workflow workflow = WorkflowBuilder.builder()
                .build(false);
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson(true));
        return SingleResponse.of(jsonObject);
    }

    @PostMapping("/save")
    public Response save(@RequestBody JSONObject request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        if(current!=null){
            request.put("createdOperator",String.valueOf(current.getUserId()));
        }
        Workflow workflow =  Workflow.formJson(request.toJSONString());
        System.out.println(workflow);
        return Response.buildSuccess();
    }

    @GetMapping("/load")
    public SingleResponse<JSONObject> load(String id) {
        Workflow workflow = workflowRepository.get(id);
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson(true));
        return SingleResponse.of(jsonObject);
    }

}
