package com.codingapi.flow.repository;

import com.codingapi.flow.domain.UrgeInterval;

/**
 * 催办频率控制仓库
 */
public interface UrgeIntervalRepository {

    UrgeInterval getLatest(String processId, long recordId);

    void save(UrgeInterval urgeInterval);
}
