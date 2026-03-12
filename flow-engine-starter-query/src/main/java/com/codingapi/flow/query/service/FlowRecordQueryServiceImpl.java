package com.codingapi.flow.query.service;

import com.codingapi.flow.infra.convert.FlowRecordContentConvertor;
import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.infra.jpa.FlowRecordEntityRepository;
import com.codingapi.flow.infra.jpa.FlowTodoRecordEntityRepository;
import com.codingapi.flow.pojo.response.FlowRecordContent;
import com.codingapi.flow.query.FlowRecordQueryService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@AllArgsConstructor
public class FlowRecordQueryServiceImpl implements FlowRecordQueryService {

    private final FlowRecordEntityRepository flowRecordEntityRepository;
    private final FlowTodoRecordEntityRepository flowTodoRecordEntityRepository;


    @Override
    public Page<FlowRecordContent> findAll(PageRequest request) {
        Page<FlowRecordEntity> page = flowRecordEntityRepository.findAllFlowRecords(request.withSort(Sort.by("id").descending()));
        return page.map(FlowRecordContentConvertor::convert);
    }

    @Override
    public Page<FlowRecordContent> findTodoRecordPage(long userId, PageRequest request) {
        Page<FlowTodoRecordEntity> page = flowTodoRecordEntityRepository.findTodoRecordPage(userId,request.withSort(Sort.by("id").descending()));
        return page.map(FlowRecordContentConvertor::convert);

    }

    @Override
    public Page<FlowRecordContent> findNotifyRecordPage(long userId, PageRequest request) {
        Page<FlowRecordEntity> page =flowRecordEntityRepository.findNotifyRecordPage(userId,request.withSort(Sort.by("id").descending()));
        return page.map(FlowRecordContentConvertor::convert);

    }

    @Override
    public Page<FlowRecordContent> findDoneRecordPage(long userId, PageRequest request) {
        Page<FlowRecordEntity> page =flowRecordEntityRepository.findDoneRecordPage(userId,request.withSort(Sort.by("id").descending()));
        return page.map(FlowRecordContentConvertor::convert);
    }
}
