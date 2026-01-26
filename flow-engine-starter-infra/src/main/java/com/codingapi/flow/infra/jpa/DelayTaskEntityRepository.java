package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.DelayTaskEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

public interface DelayTaskEntityRepository extends FastRepository<DelayTaskEntity,String> {
}
