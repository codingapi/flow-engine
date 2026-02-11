package com.codingapi.flow.query.controller;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.infra.jpa.FlowRecordEntityRepository;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.springboot.framework.dto.request.SearchRequest;
import com.codingapi.springboot.framework.dto.response.MultiResponse;
import com.codingapi.springboot.framework.user.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query/record")
@AllArgsConstructor
public class FlowRecordQueryController {

    private final FlowRecordEntityRepository flowRecordEntityRepository;

    /**
     * 全部流程记录接口
     */
    @GetMapping("/list")
    public MultiResponse<FlowRecordEntity> list(SearchRequest request) {
        return MultiResponse.of(flowRecordEntityRepository.searchRequest(request));
    }


    /**
     * 我的待办记录
     */
    @GetMapping("/todo")
    public MultiResponse<FlowRecordEntity> todo(SearchRequest request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        PageRequest pageRequest = request.toPageRequest(FlowRecordEntity.class);
        return MultiResponse.of(flowRecordEntityRepository.findTodoPage(current.getUserId(),pageRequest));
    }


    /**
     * 我的已办记录
     */
    @GetMapping("/done")
    public MultiResponse<FlowRecordEntity> done(SearchRequest request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        PageRequest pageRequest = request.toPageRequest(FlowRecordEntity.class);
        return MultiResponse.of(flowRecordEntityRepository.findDonePage(current.getUserId(),pageRequest));
    }
}
