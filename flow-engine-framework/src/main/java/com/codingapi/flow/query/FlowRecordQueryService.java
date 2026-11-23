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
     * 按 {@code (审批人, 身份)} 查询待办（issue #288）。
     *
     * <p>默认实现忽略身份、退化为 {@link #findTodoRecordPage(long, PageRequest)}，
     * 保证下游自定义实现无需改动即可编译；需要多身份隔离时覆盖此方法。
     *
     * @param userId   审批人 id
     * @param identity 身份标识，{@code null} 与空串等价（默认身份）
     * @param request  分页
     * @return 待办分页
     */
    default Page<FlowRecordContent> findTodoRecordPage(long userId, String identity, PageRequest request) {
        return findTodoRecordPage(userId, request);
    }

    /**
     * 按 {@code (审批人, 身份)} 查询抄送（issue #288），默认忽略身份。
     */
    default Page<FlowRecordContent> findNotifyRecordPage(long userId, String identity, PageRequest request) {
        return findNotifyRecordPage(userId, request);
    }

    /**
     * 按 {@code (审批人, 身份)} 查询已办（issue #288），默认忽略身份。
     */
    default Page<FlowRecordContent> findDoneRecordPage(long userId, String identity, PageRequest request) {
        return findDoneRecordPage(userId, request);
    }

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
