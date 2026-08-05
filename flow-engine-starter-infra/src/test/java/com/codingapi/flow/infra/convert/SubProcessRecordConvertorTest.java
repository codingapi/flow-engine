package com.codingapi.flow.infra.convert;

import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.infra.entity.SubProcessRecordEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
