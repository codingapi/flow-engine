package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.infra.convert.UrgeIntervalConvertor;
import com.codingapi.flow.domain.UrgeInterval;
import com.codingapi.flow.infra.entity.UrgeIntervalEntity;
import com.codingapi.flow.infra.jpa.UrgeIntervalEntityRepository;
import com.codingapi.flow.repository.UrgeIntervalRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@AllArgsConstructor
public class UrgeIntervalRepositoryImpl implements UrgeIntervalRepository {

    private final UrgeIntervalEntityRepository urgeIntervalEntityRepository;

    @Override
    public UrgeInterval getLatest(String processId, long recordId) {
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by("createTime").descending());
        Page<UrgeIntervalEntity> page = urgeIntervalEntityRepository.findByProcessIdAndRecordId(processId, recordId, pageRequest);
        if(page.isEmpty()){
            return null;
        }
        return UrgeIntervalConvertor.convert(page.getContent().get(0));
    }

    @Override
    public void save(UrgeInterval urgeInterval) {
        UrgeIntervalEntity entity = UrgeIntervalConvertor.convert(urgeInterval);
        urgeIntervalEntityRepository.save(entity);
        urgeInterval.setId(entity.getId());
    }
}
