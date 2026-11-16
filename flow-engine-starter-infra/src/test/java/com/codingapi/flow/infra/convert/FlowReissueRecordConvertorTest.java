package com.codingapi.flow.infra.convert;

import com.codingapi.flow.domain.FlowReissueRecord;
import com.codingapi.flow.infra.entity.FlowReissueRecordEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FlowReissueRecordConvertorTest {

    /**
     * 测试目标：验证二次发起关系记录可在领域对象与持久化对象之间完整往返。
     * 前置条件：持久化对象携带源流程、新流程、操作人等信息。
     * 执行步骤：转换为领域对象，再回写到一个新建的持久化对象。
     * 期望断言：全部业务字段不丢失；回写已有实体时保留原实体与版本号。
     */
    @Test
    void shouldRoundTripReissueRecord() {
        FlowReissueRecordEntity entity = new FlowReissueRecordEntity();
        entity.setId(10L);
        entity.setSourceRecordId(21L);
        entity.setSourceProcessId("source-process");
        entity.setNewRecordId(31L);
        entity.setNewProcessId("new-process");
        entity.setWorkCode("reissue-workflow");
        entity.setWorkRuntimeId(41L);
        entity.setOperatorId(51L);
        entity.setOperatorName("流程管理员");
        entity.setCreateTime(1000L);
        entity.setAdvice("业务数据有误，重新发起");
        entity.setVersion(7L);

        FlowReissueRecord record = FlowReissueRecordConvertor.convert(entity);

        assertAll("实体转换为领域对象",
                () -> assertEquals(10L, record.getId()),
                () -> assertEquals(21L, record.getSourceRecordId()),
                () -> assertEquals("source-process", record.getSourceProcessId()),
                () -> assertEquals(31L, record.getNewRecordId()),
                () -> assertEquals("new-process", record.getNewProcessId()),
                () -> assertEquals("reissue-workflow", record.getWorkCode()),
                () -> assertEquals(41L, record.getWorkRuntimeId()),
                () -> assertEquals(51L, record.getOperatorId()),
                () -> assertEquals("流程管理员", record.getOperatorName()),
                () -> assertEquals(1000L, record.getCreateTime()),
                () -> assertEquals("业务数据有误，重新发起", record.getAdvice()));

        FlowReissueRecordEntity converted = FlowReissueRecordConvertor.convert(record, entity);

        assertSame(entity, converted, "回写时应复用已有实体");
        assertEquals(7L, converted.getVersion(), "回写不应覆盖乐观锁版本号");
        assertEquals(record.getNewProcessId(), converted.getNewProcessId());
    }
}
