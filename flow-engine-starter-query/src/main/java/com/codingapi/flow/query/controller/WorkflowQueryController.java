package com.codingapi.flow.query.controller;

import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.flow.infra.jpa.WorkflowEntityRepository;
import com.codingapi.springboot.framework.dto.request.SearchRequest;
import com.codingapi.springboot.framework.dto.response.MultiResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/query/workflow")
@AllArgsConstructor
public class WorkflowQueryController {

    private final WorkflowEntityRepository workflowEntityRepository;

    @GetMapping("/list")
    public MultiResponse<WorkflowEntity> list(SearchRequest searchRequest) {
        return MultiResponse.of(workflowEntityRepository.searchRequest(searchRequest));
    }

}
