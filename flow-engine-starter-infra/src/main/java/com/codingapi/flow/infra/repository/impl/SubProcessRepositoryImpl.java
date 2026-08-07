package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.infra.convert.SubProcessRecordConvertor;
import com.codingapi.flow.infra.entity.SubProcessRecordEntity;
import com.codingapi.flow.infra.jpa.SubProcessRecordEntityRepository;
import com.codingapi.flow.repository.SubProcessRepository;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 子流程执行记录仓库的 JPA 实现。
 */
@AllArgsConstructor
public class SubProcessRepositoryImpl implements SubProcessRepository {

    private final SubProcessRecordEntityRepository repository;

    /**
     * 保存子流程执行记录：
     * 已持久化的记录（id 大于 0）更新到已有实体，否则新建实体；
     * 保存后回写生成的主键。
     */
    @Override
    public void save(SubProcessRecord record) {
        SubProcessRecordEntity entity = record.getId() > 0
                ? repository.findById(record.getId()).orElseGet(SubProcessRecordEntity::new)
                : new SubProcessRecordEntity();
        SubProcessRecordConvertor.convert(record, entity);
        repository.save(entity);
        record.setId(entity.getId());
    }

    /**
     * 按父流程中子流程节点的执行记录id查询，加悲观写锁防止子流程结果并发判定。
     */
    @Override
    public List<SubProcessRecord> findByParentRecordId(long parentRecordId) {
        return repository.findForUpdateByParentRecordId(parentRecordId).stream()
                .map(SubProcessRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<SubProcessRecord> findByParentProcessId(String parentProcessId) {
        return repository.findByParentProcessIdOrderById(parentProcessId).stream()
                .map(SubProcessRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<SubProcessRecord> findByParentProcessIdAndNodeId(String parentProcessId, String nodeId) {
        return repository.findByParentProcessIdAndNodeIdOrderById(parentProcessId, nodeId).stream()
                .map(SubProcessRecordConvertor::convert)
                .toList();
    }

}