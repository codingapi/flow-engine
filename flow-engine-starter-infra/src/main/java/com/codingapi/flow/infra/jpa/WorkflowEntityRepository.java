package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.flow.infra.pojo.Select;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkflowEntityRepository extends FastRepository<WorkflowEntity,String> {

    WorkflowEntity getWorkflowEntityById(String id);

    @Query("select new com.codingapi.flow.infra.pojo.Select(w.title,w.id) from WorkflowEntity w where w.enable = true")
    List<Select> options();
}
