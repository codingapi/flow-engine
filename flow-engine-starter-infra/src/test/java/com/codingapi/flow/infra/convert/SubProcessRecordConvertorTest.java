package com.codingapi.flow.infra.convert;

import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.infra.entity.SubProcessRecordEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubProcessRecordConvertorTest {

    /**
     * 测试目标：验证子流程聚合记录可在领域对象与持久化对象之间完整转换。
     * 前置条件：持久化对象包含两个不同状态的子流程实例。
     * 执行步骤：转换为领域对象，再将其回写到原持久化对象。
     * 期望断言：记录字段和实例状态不丢失，回写时保留原实体以及版本号。
     */
    @Test
    void shouldRoundTripAndKeepManagedEntityVersion() {
        SubProcessRecordEntity entity = new SubProcessRecordEntity();
        entity.setId(10L);
        entity.setGroupId("group-1");
        entity.setParentProcessId("parent-process");
        entity.setParentRecordId(20L);
        entity.setParentWorkRuntimeId(30L);
        entity.setNodeId("sub-node");
        entity.setTotalCount(2);
        entity.setInstances("""
                [{"startRecordId":1,"processId":"child-1","finishRecordId":11,"state":"FINISHED","finishTime":100},
                 {"startRecordId":2,"processId":"child-2","finishRecordId":0,"state":"RUNNING","finishTime":0}]
                """);
        entity.setState(SubProcessRecord.State.WAITING.name());
        entity.setCreateTime(90L);
        entity.setFinishTime(0L);
        entity.setVersion(7L);

        SubProcessRecord record = SubProcessRecordConvertor.convert(entity);
        assertNull(record.getInstances().get(0).getWorkTitle(), "历史 JSON 缺少流程名称时应兼容读取");
        SubProcessRecord.Instance first = record.getInstances().get(0);
        record.getInstances().set(0, new SubProcessRecord.Instance(
                first.getStartRecordId(),
                first.getProcessId(),
                "采购审批子流程",
                first.getFinishRecordId(),
                first.getState(),
                first.getFinishTime()));
        SubProcessRecordEntity converted = SubProcessRecordConvertor.convert(record, entity);

        assertSame(entity, converted);
        assertEquals(7L, converted.getVersion());
        assertEquals(List.of(11L), record.findFinishedRecordIds());
        assertEquals(SubProcessRecord.InstanceState.RUNNING, record.getInstances().get(1).getState());
        assertTrue(converted.getInstances().contains("采购审批子流程"), "新记录应在实例 JSON 中保存流程名称");
    }

    /**
     * 测试目标：验证重置相关字段（已取代标记、继承实例、重建映射）的转换与存量兼容性。
     * 前置条件：持久化对象携带已取代标记与含重置字段的实例；另有一条不含新字段的存量数据。
     * 执行步骤：转换为领域对象后回写持久化对象。
     * 期望断言：新字段完整往返；存量数据（无新字段）按未取代、非继承读取。
     */
    @Test
    void shouldRoundTripResetFieldsAndKeepLegacyCompatibility() {
        SubProcessRecordEntity entity = new SubProcessRecordEntity();
        entity.setId(11L);
        entity.setGroupId("group-reset");
        entity.setParentProcessId("parent-process");
        entity.setParentRecordId(20L);
        entity.setParentWorkRuntimeId(30L);
        entity.setNodeId("sub-node");
        entity.setTotalCount(2);
        entity.setInstances("""
                [{"startRecordId":1,"processId":"child-1","finishRecordId":11,"state":"FINISHED","finishTime":100,"inherited":true},
                 {"startRecordId":3,"processId":"child-3","finishRecordId":0,"state":"RUNNING","finishTime":0,"sourceProcessId":"child-2"}]
                """);
        entity.setState(SubProcessRecord.State.WAITING.name());
        entity.setCreateTime(90L);
        entity.setFinishTime(0L);
        entity.setSuperseded(true);

        SubProcessRecord record = SubProcessRecordConvertor.convert(entity);
        assertAll("重置字段读取",
                () -> assertTrue(record.isSuperseded()),
                () -> assertTrue(record.getInstances().get(0).isInherited()),
                () -> assertEquals("child-2", record.getInstances().get(1).getSourceProcessId()));

        SubProcessRecordEntity converted = SubProcessRecordConvertor.convert(record, entity);
        assertEquals(Boolean.TRUE, converted.getSuperseded());
        assertTrue(converted.getInstances().contains("sourceProcessId"), "实例 JSON 应保存重建映射字段");

        SubProcessRecordEntity legacy = new SubProcessRecordEntity();
        legacy.setId(12L);
        legacy.setGroupId("group-legacy");
        legacy.setParentProcessId("parent-process");
        legacy.setParentRecordId(20L);
        legacy.setParentWorkRuntimeId(30L);
        legacy.setNodeId("sub-node");
        legacy.setTotalCount(1);
        legacy.setInstances("""
                [{"startRecordId":1,"processId":"child-1","finishRecordId":11,"state":"FINISHED","finishTime":100}]
                """);
        legacy.setState(SubProcessRecord.State.PASSED.name());
        legacy.setCreateTime(90L);
        legacy.setFinishTime(100L);

        SubProcessRecord legacyRecord = SubProcessRecordConvertor.convert(legacy);
        assertAll("存量数据兼容",
                () -> assertFalse(legacyRecord.isSuperseded(), "无已取代标记的存量数据按未取代处理"),
                () -> assertFalse(legacyRecord.getInstances().get(0).isInherited()),
                () -> assertNull(legacyRecord.getInstances().get(0).getSourceProcessId()));
    }
}
