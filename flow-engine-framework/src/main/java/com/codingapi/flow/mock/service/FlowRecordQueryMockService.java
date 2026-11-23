package com.codingapi.flow.mock.service;

import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.mock.repository.FlowRecordRepositoryMockImpl;
import com.codingapi.flow.mock.repository.FlowTodoRecordRepositoryMockImpl;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.response.FlowRecordContent;
import com.codingapi.flow.query.FlowRecordQueryService;
import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;

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

    /**
     * 按 {@code (审批人, 身份)} 查询待办（issue #288 衍生 / #291）。
     *
     * <p>Mock 链路与真实 JPA 链路必须口径一致，否则带 {@code mockKey} 的前端联调会看到串味数据。
     */
    @Override
    public Page<FlowRecordContent> findTodoRecordPage(long userId, String identity, PageRequest request) {
        FlowTodoRecordRepositoryMockImpl flowTodoRecordRepositoryMock = (FlowTodoRecordRepositoryMockImpl) mockRepositoryHolder.getFlowTodoRecordRepository();
        List<FlowRecordContent> recordList = flowTodoRecordRepositoryMock.findByOperatorId(userId, identity).stream().map(FlowRecordContent::convert).toList();
        return this.toPage(recordList, request);
    }

    @Override
    public Page<FlowRecordContent> findNotifyRecordPage(long userId, PageRequest request) {
        FlowRecordRepositoryMockImpl flowRecordRepositoryMock = (FlowRecordRepositoryMockImpl) mockRepositoryHolder.getFlowRecordRepository();
        List<FlowRecordContent> recordList = flowRecordRepositoryMock.findNotifyByOperator(userId).stream().map(FlowRecordContent::convert).toList();
        return toPage(recordList, request);
    }

    /**
     * 按 {@code (审批人, 身份)} 查询抄送（issue #288 衍生 / #291）。
     */
    @Override
    public Page<FlowRecordContent> findNotifyRecordPage(long userId, String identity, PageRequest request) {
        FlowRecordRepositoryMockImpl flowRecordRepositoryMock = (FlowRecordRepositoryMockImpl) mockRepositoryHolder.getFlowRecordRepository();
        List<FlowRecordContent> recordList = flowRecordRepositoryMock.findNotifyByOperator(userId, identity).stream().map(FlowRecordContent::convert).toList();
        return toPage(recordList, request);
    }

    @Override
    public Page<FlowRecordContent> findDoneRecordPage(long userId, PageRequest request) {
        FlowRecordRepositoryMockImpl flowRecordRepositoryMock = (FlowRecordRepositoryMockImpl) mockRepositoryHolder.getFlowRecordRepository();
        List<FlowRecordContent> recordList = flowRecordRepositoryMock.findDoneByOperator(userId).stream().map(FlowRecordContent::convert).toList();
        return toPage(recordList, request);
    }

    /**
     * 按 {@code (审批人, 身份)} 查询已办（issue #288 衍生 / #291）。
     */
    @Override
    public Page<FlowRecordContent> findDoneRecordPage(long userId, String identity, PageRequest request) {
        FlowRecordRepositoryMockImpl flowRecordRepositoryMock = (FlowRecordRepositoryMockImpl) mockRepositoryHolder.getFlowRecordRepository();
        List<FlowRecordContent> recordList = flowRecordRepositoryMock.findDoneByOperator(userId, identity).stream().map(FlowRecordContent::convert).toList();
        return toPage(recordList, request);
    }

    @Override
    public Page<FlowRecordContent> findRunningInstancePage(String workCode, String title, PageRequest request) {
        FlowRecordRepositoryMockImpl flowRecordRepositoryMock = (FlowRecordRepositoryMockImpl) mockRepositoryHolder.getFlowRecordRepository();
        List<FlowRecordContent> recordList = flowRecordRepositoryMock.findAll().stream()
                .filter(record -> workCode.equals(record.getWorkCode()))
                .filter(record -> record.getFlowState() == FlowRecord.SATE_FLOW_RUNNING)
                .filter(record -> record.getFromId() == 0 && StartNode.NODE_TYPE.equals(record.getNodeType()))
                .filter(record -> !record.isHidden())
                .filter(record -> !StringUtils.hasText(title)
                        || (record.getTitle() != null && record.getTitle().contains(title)))
                .map(FlowRecordContent::convert)
                .toList();
        return toPage(recordList, request);
    }

}
