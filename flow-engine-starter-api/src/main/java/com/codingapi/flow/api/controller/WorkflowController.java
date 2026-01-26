package com.codingapi.flow.api.controller;

import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/workflow/cmd")
@AllArgsConstructor
public class WorkflowController {

    private final WorkflowRepository workflowRepository;

    @GetMapping("/create")
    public SingleResponse<Workflow> create() {
        Workflow workflow = WorkflowBuilder.builder()
                .build();
        workflowRepository.save(workflow);
        return SingleResponse.of(workflow);
    }

}
