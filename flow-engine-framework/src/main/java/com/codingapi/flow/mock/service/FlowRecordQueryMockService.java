package com.codingapi.flow.mock.service;

import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.mock.repository.FlowRecordRepositoryMockImpl;
import com.codingapi.flow.mock.repository.FlowTodoRecordRepositoryMockImpl;
import com.codingapi.flow.pojo.response.FlowRecordContent;
import com.codingapi.flow.query.FlowRecordQueryService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class FlowRecordQueryMockService implements FlowRecordQueryService {

    private final static int CURRENT_OFFSET = 0;

    private final MockRepositoryHolder mockRepositoryHolder;

    private Page<FlowRecordContent> toPage(List<FlowRecordContent> recordList, PageRequest request) {
        // 创建副本避免修改原列表
        List<FlowRecordContent> sortedList = new ArrayList<>(recordList);

        // 按recordId降序排序（大到小）
        sortedList.sort((o1, o2) -> {
            // 处理null值（假设recordId不为null）
            return Long.compare(o2.getRecordId(), o1.getRecordId());
        });

        int pageNumber = request.getPageNumber()  + CURRENT_OFFSET;
        int pageSize = request.getPageSize();
        int totalSize = sortedList.size();

        // 计算分页起始位置
        int from = Math.min(pageNumber * pageSize, totalSize);
        int to = Math.min(from + pageSize, totalSize);

        // 处理空列表或越界情况
        List<FlowRecordContent> currentPage;
        if (from >= totalSize) {
            currentPage = Collections.emptyList();
        } else {
            currentPage = sortedList.subList(from, to);
        }

        return new PageImpl<>(currentPage, PageRequest.of(pageNumber,pageSize), totalSize);
    }

    @Override
    public Page<FlowRecordContent> findAll(PageRequest request) {
        FlowRecordRepositoryMockImpl flowRecordRepositoryMock = (FlowRecordRepositoryMockImpl) mockRepositoryHolder.getFlowRecordRepository();
        List<FlowRecordContent> recordList = flowRecordRepositoryMock.findAll().stream().map(FlowRecordContent::convert).toList();
        return this.toPage(recordList, request);
    }

    @Override
    public Page<FlowRecordContent> findTodoRecordPage(long userId, PageRequest request) {
        FlowTodoRecordRepositoryMockImpl flowTodoRecordRepositoryMock = (FlowTodoRecordRepositoryMockImpl) mockRepositoryHolder.getFlowTodoRecordRepository();
        List<FlowRecordContent> recordList = flowTodoRecordRepositoryMock.findByOperatorId(userId).stream().map(FlowRecordContent::convert).toList();
        return this.toPage(recordList, request);
    }

    @Override
    public Page<FlowRecordContent> findNotifyRecordPage(long userId, PageRequest request) {
        FlowRecordRepositoryMockImpl flowRecordRepositoryMock = (FlowRecordRepositoryMockImpl) mockRepositoryHolder.getFlowRecordRepository();
        List<FlowRecordContent> recordList = flowRecordRepositoryMock.findNotifyByOperator(userId).stream().map(FlowRecordContent::convert).toList();
        return toPage(recordList, request);
    }

    @Override
    public Page<FlowRecordContent> findDoneRecordPage(long userId, PageRequest request) {
        FlowRecordRepositoryMockImpl flowRecordRepositoryMock = (FlowRecordRepositoryMockImpl) mockRepositoryHolder.getFlowRecordRepository();
        List<FlowRecordContent> recordList = flowRecordRepositoryMock.findDoneByOperator(userId).stream().map(FlowRecordContent::convert).toList();
        return toPage(recordList, request);
    }

}
