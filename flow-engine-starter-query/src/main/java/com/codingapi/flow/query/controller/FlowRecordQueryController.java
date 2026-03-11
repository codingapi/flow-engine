package com.codingapi.flow.query.controller;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.infra.jpa.FlowRecordEntityRepository;
import com.codingapi.flow.infra.jpa.FlowTodoRecordEntityRepository;
import com.codingapi.flow.infra.pojo.FlowRecordContent;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.springboot.framework.dto.request.Relation;
import com.codingapi.springboot.framework.dto.request.SearchRequest;
import com.codingapi.springboot.framework.dto.response.MultiResponse;
import com.codingapi.springboot.framework.user.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query/record")
@AllArgsConstructor
public class FlowRecordQueryController {

    private final FlowRecordEntityRepository flowRecordEntityRepository;
    private final FlowTodoRecordEntityRepository flowTodoRecordEntityRepository;

    /**
     * 全部流程记录接口
     */
    @GetMapping("/list")
    public MultiResponse<FlowRecordContent> list(SearchRequest request) {
        request.addSort(Sort.by("id").descending());
        request.addFilter("revoked", Relation.EQUAL,false);
        Page<FlowRecordEntity> page = flowRecordEntityRepository.searchRequest(request);
        return MultiResponse.of(page.map(FlowRecordContent::convert));
    }


    /**
     * 我的待办记录
     */
    @GetMapping("/todo")
    public MultiResponse<FlowRecordContent> todo(SearchRequest request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        PageRequest pageRequest = request.toPageRequest(FlowRecordEntity.class);
        Page<FlowTodoRecordEntity> page = flowTodoRecordEntityRepository.findTodoRecordPage(current.getUserId(),pageRequest.withSort(Sort.by("id").descending()));
        return MultiResponse.of(page.map(FlowRecordContent::convert));
    }

    /**
     * 我的抄送记录
     */
    @GetMapping("/notify")
    public MultiResponse<FlowRecordContent> notify(SearchRequest request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        PageRequest pageRequest = request.toPageRequest(FlowRecordEntity.class);
        Page<FlowRecordEntity> page =flowRecordEntityRepository.findNotifyRecordPage(current.getUserId(),pageRequest.withSort(Sort.by("id").descending()));
        return MultiResponse.of(page.map(FlowRecordContent::convert));
    }


    /**
     * 我的已办记录
     */
    @GetMapping("/done")
    public MultiResponse<FlowRecordContent> done(SearchRequest request) {
        IFlowOperator current = (IFlowOperator) UserContext.getInstance().current();
        PageRequest pageRequest = request.toPageRequest(FlowRecordEntity.class);
        Page<FlowRecordEntity> page =flowRecordEntityRepository.findDoneRecordPage(current.getUserId(),pageRequest.withSort(Sort.by("id").descending()));
        return MultiResponse.of(page.map(FlowRecordContent::convert));
    }
}
