package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.WorkflowVersionEntity;
import com.codingapi.flow.infra.pojo.WorkflowOption;
import com.codingapi.flow.infra.pojo.WorkflowVersionOption;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkflowVersionEntityRepository  extends FastRepository<WorkflowVersionEntity,Long> {

    WorkflowVersionEntity getWorkflowVersionEntityById(long id);

    void deleteByWorkId(String workId);

    List<WorkflowVersionEntity> findByWorkId(String workId);

    @Query("select new com.codingapi.flow.infra.pojo.WorkflowVersionOption(w.id,w.versionName,w.current) from WorkflowVersionEntity w where w.workId = ?1")
    List<WorkflowVersionOption> versions(String workId);

}
