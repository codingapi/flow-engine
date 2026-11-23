package com.codingapi.flow.infra.convert;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 流程记录身份字段的持久化映射测试（issue #288 P1）。
 *
 * <p>覆盖两点：历史数据（identity 列为 null）读取为默认身份；身份在实体与领域对象之间不丢失。
 */
class FlowRecordIdentityConvertorTest {

    /**
     * 构造字段完整的流程记录实体，避免转换时的拆箱 NPE 干扰身份断言。
     */
    private FlowRecordEntity newRecordEntity() {
        FlowRecordEntity entity = new FlowRecordEntity();
        entity.setId(1L);
        entity.setWorkRuntimeId(1L);
        entity.setWorkTitle("请假流程");
        entity.setWorkCode("leave");
        entity.setNodeId("start");
        entity.setNodeType("START");
        entity.setNodeName("开始");
        entity.setTitle("请假流程");
        entity.setFromId(0L);
        entity.setParentId(0L);
        entity.setReadTime(0L);
        entity.setCurrentOperatorId(1L);
        entity.setSubmitOperatorId(1L);
        entity.setForwardOperatorId(0L);
        entity.setNodeOrder(0);
        entity.setHidden(false);
        entity.setRevoked(false);
        entity.setNotify(false);
        entity.setRecordState(0);
        entity.setFlowState(0);
        entity.setUpdateTime(0L);
        entity.setCreateTime(0L);
        entity.setFinishTime(0L);
        entity.setReadable(false);
        entity.setCreateOperatorId(1L);
        entity.setTimeoutTime(0L);
        entity.setMergeable(false);
        entity.setInterferedOperatorId(0L);
        entity.setDelegateId(0L);
        entity.setParallelBranchTotal(0);
        return entity;
    }

    /**
     * 构造字段完整的待办记录实体。
     */
    private FlowTodoRecordEntity newTodoEntity() {
        FlowTodoRecordEntity entity = new FlowTodoRecordEntity();
        entity.setId(1L);
        entity.setWorkflowRuntimeId(1L);
        entity.setReadTime(0L);
        entity.setCurrentOperatorId(1L);
        entity.setSubmitOperatorId(1L);
        entity.setCreateTime(0L);
        entity.setCreateOperatorId(1L);
        entity.setMargeCount(0);
        entity.setMergeable(false);
        entity.setRecordId(1L);
        entity.setTimeoutTime(0L);
        return entity;
    }

    /**
     * 测试目标：历史数据没有 identity 列值时，读取后统一为默认身份（空串）。
     * 前置条件：实体 identity 字段全部为 null（升级前的存量数据）。
     * 执行步骤：实体 → 领域对象。
     * 期望断言：五组操作者身份均为空串，不出现 null。
     */
    @Test
    void should_read_null_identity_as_default_for_legacy_record() {
        FlowRecordEntity entity = newRecordEntity();

        FlowRecord record = FlowRecordConvertor.convert(entity);

        assertAll(
                () -> assertEquals("", record.getCreateOperatorIdentity()),
                () -> assertEquals("", record.getSubmitOperatorIdentity()),
                () -> assertEquals("", record.getCurrentOperatorIdentity()),
                () -> assertEquals("", record.getForwardOperatorIdentity()),
                () -> assertEquals("", record.getInterferedOperatorIdentity())
        );
    }

    /**
     * 测试目标：身份标识在实体与领域对象之间双向转换不丢失。
     * 前置条件：实体带有五组不同的身份标识。
     * 执行步骤：实体 → 领域对象 → 实体。
     * 期望断言：两组身份值完全一致。
     */
    @Test
    void should_round_trip_identity() {
        FlowRecordEntity entity = newRecordEntity();
        entity.setCreateOperatorIdentity("department1");
        entity.setSubmitOperatorIdentity("department1");
        entity.setCurrentOperatorIdentity("department2");
        entity.setForwardOperatorIdentity("department3");
        entity.setInterferedOperatorIdentity("department4");

        FlowRecord record = FlowRecordConvertor.convert(entity);
        FlowRecordEntity roundTrip = FlowRecordConvertor.convert(record);

        assertAll(
                () -> assertEquals("department1", record.getCreateOperatorIdentity()),
                () -> assertEquals("department2", roundTrip.getCurrentOperatorIdentity()),
                () -> assertEquals("department3", roundTrip.getForwardOperatorIdentity()),
                () -> assertEquals("department4", roundTrip.getInterferedOperatorIdentity())
        );
    }

    /**
     * 测试目标：待办记录的历史数据同样读取为默认身份，且身份可往返。
     * 前置条件：待办实体 identity 为 null 与有值两种情况。
     * 执行步骤：实体 → 领域对象 → 实体。
     * 期望断言：null 归一化为空串，有值身份保持。
     */
    @Test
    void should_map_todo_record_identity() {
        FlowTodoRecordEntity legacyEntity = newTodoEntity();
        FlowTodoRecord legacyTodo = FlowTodoRecordConvertor.convert(legacyEntity);
        assertEquals("", legacyTodo.getCurrentOperatorIdentity());
        assertEquals("", legacyTodo.getSubmitOperatorIdentity());
        assertEquals("", legacyTodo.getCreateOperatorIdentity());

        FlowTodoRecordEntity entity = newTodoEntity();
        entity.setId(2L);
        entity.setCurrentOperatorIdentity("department2");
        entity.setSubmitOperatorIdentity("department1");
        entity.setCreateOperatorIdentity("department1");

        FlowTodoRecord todo = FlowTodoRecordConvertor.convert(entity);
        FlowTodoRecordEntity roundTrip = FlowTodoRecordConvertor.convert(todo);

        assertAll(
                () -> assertEquals("department2", todo.getCurrentOperatorIdentity()),
                () -> assertEquals("department1", roundTrip.getSubmitOperatorIdentity()),
                () -> assertEquals("department1", roundTrip.getCreateOperatorIdentity())
        );
    }
}
