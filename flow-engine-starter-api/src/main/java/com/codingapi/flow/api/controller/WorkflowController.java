package com.codingapi.flow.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.api.pojo.NodeCreateRequest;
import com.codingapi.flow.api.service.NodeBuildService;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.response.Response;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import com.codingapi.springboot.framework.user.UserContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/cmd/workflow")
@AllArgsConstructor
@Transactional
public class WorkflowController {

    private final WorkflowRepository workflowRepository;
    private final NodeBuildService nodeBuildService;

    @PostMapping("/remove")
    public Response remove(@RequestBody IdRequest request) {
        Workflow workflow = workflowRepository.get(request.getStringId());
        workflowRepository.delete(workflow);
        return Response.buildSuccess();
    }


    @PostMapping("/changeState")
    public Response changeState(@RequestBody IdRequest request) {
        Workflow workflow = workflowRepository.get(request.getStringId());
        if(workflow.isDisable()){
            workflow.enable();
        }else {
            workflow.disable();
        }
        workflowRepository.save(workflow);
        return Response.buildSuccess();
    }


    @PostMapping("/create")
    public SingleResponse<JSONObject> create() {
        Workflow workflow = WorkflowBuilder.builder()
                .build(false);
        workflow.addDefaultNodesAndEdges();
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson(true));
        return SingleResponse.of(jsonObject);
    }

    @PostMapping("/create-node")
    public SingleResponse<Map<String, Object>> createNode(@RequestBody NodeCreateRequest request) {
        return SingleResponse.of(nodeBuildService.buildNode(request.getType()));
    }

    @PostMapping("/save")
    public Response save(@RequestBody JSONObject request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        if (current != null) {
            request.put("createdOperator", String.valueOf(current.getUserId()));
        }
        Workflow workflow = Workflow.formJson(request.toJSONString());
        workflow.updateTime();
        workflowRepository.save(workflow);
        return Response.buildSuccess();
    }

    @GetMapping("/load")
    public SingleResponse<JSONObject> load(String id) {
        Workflow workflow = workflowRepository.get(id);
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson(true));
        return SingleResponse.of(jsonObject);
    }

}
