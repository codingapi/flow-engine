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

    /**
     * 查询指定流程编码下运行中的流程实例（以开始节点记录代表一个实例）
     *
     * <p>用于「运行中实例强制同步最新流程设计」时选择同步范围（issue #281）。</p>
     *
     * @param workCode 流程编码
     * @param title    流程标题（模糊匹配，可为空）
     * @param request  分页
     * @return 运行中的流程实例分页
     */
    Page<FlowRecordContent> findRunningInstancePage(String workCode, String title, PageRequest request);
}
