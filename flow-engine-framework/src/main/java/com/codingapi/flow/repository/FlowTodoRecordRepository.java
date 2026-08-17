package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowTodoRecord;

import java.util.List;

public interface FlowTodoRecordRepository {

    void saveAll(List<FlowTodoRecord> margeRecords);

    FlowTodoRecord getByTodoKey(String key);

    /**
     * 按多个待办合并 key 批量加载已存在的待办记录，用于替代循环内逐条 {@link #getByTodoKey(String)} 的 N+1 查询。
     *
     * @param keys 待办合并 key 列表
     * @return 已存在的待办记录
     */
    List<FlowTodoRecord> findByKeys(List<String> keys);

    void delete(FlowTodoRecord margeRecord);

    void save(FlowTodoRecord margeRecord);

}
