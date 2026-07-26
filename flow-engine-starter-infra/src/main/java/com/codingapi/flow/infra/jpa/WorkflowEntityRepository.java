package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.flow.infra.pojo.WorkflowOption;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowEntityRepository extends FastRepository<WorkflowEntity,String> {

    WorkflowEntity getWorkflowEntityById(String id);

    WorkflowEntity getWorkflowEntityByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkflowEntity w where w.id = :id")
    WorkflowEntity lockById(@Param("id") String id);

    @Query("select new com.codingapi.flow.infra.pojo.WorkflowOption(w.title,w.code) from WorkflowEntity w where w.enable = true")
    List<WorkflowOption> options();
}
