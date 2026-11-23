package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlowRecordEntityRepository extends FastRepository<FlowRecordEntity, Long> {

    FlowRecordEntity getFlowRecordEntityById(long id);

    @Query("from FlowRecordEntity r where r.revoked = false")
    Page<FlowRecordEntity> findAllFlowRecords(PageRequest pageRequest);

    @Query("from FlowRecordEntity r where r.id in ?1")
    List<FlowRecordEntity> findByIds(List<Long> ids);

    @Query("from FlowRecordEntity r where r.processId = ?1 and r.hidden=false and r.revoked = false")
    List<FlowRecordEntity> findProcessIdRecords(String processId);

    @Query("from FlowRecordEntity r where r.fromId = ?1 and r.nodeId =?2 and r.revoked = false")
    List<FlowRecordEntity> findCurrentNodeRecords(long fromId, String nodeId);

    @Query("from FlowRecordEntity r where r.processId = ?1 and (r.recordState = 0 and r.flowState = 0 and r.hidden=false and r.revoked = false)")
    List<FlowRecordEntity> findTodoRecords(String processId);

    @Query("from FlowRecordEntity r where r.processId = ?1 and r.fromId >=?2 and r.hidden=false and r.revoked = false")
    List<FlowRecordEntity> findAfterRecords(String processId, long fromId);

    @Query("from FlowRecordEntity r where r.processId = ?1 and r.id < ?2 and r.hidden=false and r.revoked = false")
    List<FlowRecordEntity> findBeforeRecords(String processId, long id);

    @Query("from FlowRecordEntity r where r.currentOperatorId = ?1 and (r.recordState = 0 and r.flowState = 0 and r.hidden=false and r.revoked = false)")
    Page<FlowRecordEntity> findTodoRecordPage(long operatorId, PageRequest pageRequest);

    @Query("from FlowRecordEntity r where r.currentOperatorId = ?1 and (r.recordState = 1 and r.hidden=false and r.revoked = false) ")
    Page<FlowRecordEntity> findDoneRecordPage(long operatorId, PageRequest pageRequest);

    @Query("from FlowRecordEntity r where r.currentOperatorId = ?1 and r.notify = true and r.hidden=false and r.revoked = false ")
    Page<FlowRecordEntity> findNotifyRecordPage(long operatorId, PageRequest pageRequest);

    /**
     * 按 {@code (审批人, 身份)} 分页查询待办记录（issue #288）。
     *
     * <p>{@code coalesce(..., '')} 保证历史数据的 NULL 身份被当作默认身份命中，
     * 否则升级后存量待办会从列表中消失。
     */
    @Query("from FlowRecordEntity r where r.currentOperatorId = :operatorId " +
            "and coalesce(r.currentOperatorIdentity, '') = :identity " +
            "and (r.recordState = 0 and r.flowState = 0 and r.hidden=false and r.revoked = false)")
    Page<FlowRecordEntity> findTodoRecordPageByIdentity(@Param("operatorId") long operatorId,
                                                        @Param("identity") String identity,
                                                        PageRequest pageRequest);

    /**
     * 按 {@code (审批人, 身份)} 分页查询已办记录（issue #288）。
     */
    @Query("from FlowRecordEntity r where r.currentOperatorId = :operatorId " +
            "and coalesce(r.currentOperatorIdentity, '') = :identity " +
            "and (r.recordState = 1 and r.hidden=false and r.revoked = false)")
    Page<FlowRecordEntity> findDoneRecordPageByIdentity(@Param("operatorId") long operatorId,
                                                        @Param("identity") String identity,
                                                        PageRequest pageRequest);

    /**
     * 按 {@code (审批人, 身份)} 分页查询抄送记录（issue #288）。
     */
    @Query("from FlowRecordEntity r where r.currentOperatorId = :operatorId " +
            "and coalesce(r.currentOperatorIdentity, '') = :identity " +
            "and r.notify = true and r.hidden=false and r.revoked = false")
    Page<FlowRecordEntity> findNotifyRecordPageByIdentity(@Param("operatorId") long operatorId,
                                                          @Param("identity") String identity,
                                                          PageRequest pageRequest);

    /**
     * 按流程编码查询运行中的流程实例（以开始节点记录代表一个实例），用于强制同步时选择实例范围。
     *
     * @param workCode    流程编码
     * @param pageRequest 分页
     * @return 流程记录分页
     */
    @Query("from FlowRecordEntity r where r.workCode = ?1 and r.flowState = 0 and r.fromId = 0 " +
            "and r.nodeType = 'START' and r.hidden = false and r.revoked = false")
    Page<FlowRecordEntity> findRunningInstancePage(String workCode, PageRequest pageRequest);

    /**
     * 按流程编码与标题模糊查询运行中的流程实例。
     *
     * @param workCode    流程编码
     * @param title       标题模糊匹配条件
     * @param pageRequest 分页
     * @return 流程记录分页
     */
    @Query("from FlowRecordEntity r where r.workCode = ?1 and r.title like ?2 and r.flowState = 0 and r.fromId = 0 " +
            "and r.nodeType = 'START' and r.hidden = false and r.revoked = false")
    Page<FlowRecordEntity> findRunningInstancePageByTitle(String workCode, String title, PageRequest pageRequest);

}
