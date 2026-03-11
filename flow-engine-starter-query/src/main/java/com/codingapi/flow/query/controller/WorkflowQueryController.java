package com.codingapi.flow.query.controller;

import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.flow.infra.jpa.WorkflowEntityRepository;
import com.codingapi.flow.infra.jpa.WorkflowVersionEntityRepository;
import com.codingapi.flow.infra.pojo.WorkflowOption;
import com.codingapi.flow.infra.pojo.WorkflowVersionOption;
import com.codingapi.springboot.framework.dto.request.IdRequest;
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

    private final WorkflowVersionEntityRepository workflowVersionEntityRepository;

    @GetMapping("/list")
    public MultiResponse<WorkflowEntity> list(SearchRequest searchRequest) {
        return MultiResponse.of(workflowEntityRepository.searchRequest(searchRequest));
    }

    @GetMapping("/options")
    public MultiResponse<WorkflowOption> options() {
        return MultiResponse.of(workflowEntityRepository.options());
    }

    @GetMapping("/versions")
    public MultiResponse<WorkflowVersionOption> versions(IdRequest request) {
        return MultiResponse.of(workflowVersionEntityRepository.versions(request.getStringId()));
    }

}
