package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.SubProcessRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubProcessRecordEntityRepository extends FastRepository<SubProcessRecordEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("from SubProcessRecordEntity r where r.parentRecordId = ?1 order by r.id")
    List<SubProcessRecordEntity> findForUpdateByParentRecordId(long parentRecordId);

    List<SubProcessRecordEntity> findByParentProcessIdOrderById(String parentProcessId);

    List<SubProcessRecordEntity> findByParentProcessIdAndNodeIdOrderById(
            String parentProcessId, String nodeId);

}
