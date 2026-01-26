package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.UrgeIntervalEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface UrgeIntervalEntityRepository extends FastRepository<UrgeIntervalEntity,Long> {

    Page<UrgeIntervalEntity> findByProcessIdAndRecordId(String processId, long recordId, PageRequest pageRequest);

}
