package com.codingapi.flow.query;

import com.codingapi.flow.pojo.response.FlowRecordContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 流程记录查询服务
 */
public interface FlowRecordQueryService {

    Page<FlowRecordContent> findAll(PageRequest request);

    Page<FlowRecordContent> findTodoRecordPage(long userId, PageRequest request);

    Page<FlowRecordContent> findNotifyRecordPage(long userId, PageRequest request);

    Page<FlowRecordContent> findDoneRecordPage(long userId, PageRequest request);
}
