package com.codingapi.flow.mock.service;

import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.mock.repository.FlowTodoRecordRepositoryMockImpl;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.pojo.response.FlowRecordContent;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.WorkflowRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mock 查询链路的身份隔离测试（issue #288 衍生 / #291、#295）。
 *
 * <p>带 {@code mockKey} 的 REST 调用走 Mock 仓储，若三个查询重载退化成按 id 查询，
 * 同一人员的多个身份数据会串在一起，与真实 JPA 链路口径不一致。
 */
class FlowRecordQueryMockIdentityTest {

    private static final long OPERATOR_ID = 5;
    private static final PageRequest PAGE = PageRequest.of(0, 10);

    private FlowRecordQueryMockService queryService;
    private FlowTodoRecordRepositoryMockImpl todoRepository;

    @BeforeEach
    void setUp() {
        MockRepositoryHolder holder = new MockRepositoryHolder(new UserGateway(), new WorkflowRepositoryImpl());
        todoRepository = (FlowTodoRecordRepositoryMockImpl) holder.getFlowTodoRecordRepository();
        queryService = new FlowRecordQueryMockService(holder);
    }

    private static FlowTodoRecord todo(long id, long operatorId, String identity) {
        return new FlowTodoRecord(
                id, "process-" + id, 1L, "测试流程", "test-code", "node-1", "APPROVAL", "审批节点", "标题",
                0L, operatorId, "审批人", 0L, "", 0L, 0L, "", "todo-key-" + id,
                0, false, id, 0L,
                identity, "", "");
    }

    /**
     * 测试目标：按身份查询只返回该身份的待办。
     * 前置条件：同一人员存在默认 / department1 / department2 三条待办，另有他人一条。
     * 执行步骤：分别以 department1、department2 查询。
     * 期望断言：各返回 1 条，且返回内容的身份正确。
     */
    @Test
    void should_filter_todo_by_identity() {
        todoRepository.save(todo(1, OPERATOR_ID, ""));
        todoRepository.save(todo(2, OPERATOR_ID, "department1"));
        todoRepository.save(todo(3, OPERATOR_ID, "department2"));
        todoRepository.save(todo(4, 6, "department1"));

        Page<FlowRecordContent> d1 = queryService.findTodoRecordPage(OPERATOR_ID, "department1", PAGE);
        Page<FlowRecordContent> d2 = queryService.findTodoRecordPage(OPERATOR_ID, "department2", PAGE);

        assertEquals(1, d1.getTotalElements());
        assertEquals(1, d2.getTotalElements());
        assertEquals("department1", d1.getContent().get(0).getCurrentOperatorIdentity());
        assertEquals("department2", d2.getContent().get(0).getCurrentOperatorIdentity());
    }

    /**
     * 测试目标：历史记录身份为 null 时按默认身份命中（与 JPA coalesce 口径一致）。
     * 前置条件：一条默认身份待办、一条身份为 null 的历史待办、一条非空身份待办。
     * 执行步骤：以 null/空串查询默认身份。
     * 期望断言：命中 2 条，非空身份那条不混入。
     */
    @Test
    void should_treat_null_identity_as_default_for_legacy_records() {
        todoRepository.save(todo(1, OPERATOR_ID, ""));
        todoRepository.save(todo(2, OPERATOR_ID, null));
        todoRepository.save(todo(3, OPERATOR_ID, "department1"));

        Page<FlowRecordContent> byNull = queryService.findTodoRecordPage(OPERATOR_ID, null, PAGE);
        Page<FlowRecordContent> byEmpty = queryService.findTodoRecordPage(OPERATOR_ID, OperatorIdentity.DEFAULT, PAGE);

        assertEquals(2, byNull.getTotalElements());
        assertEquals(2, byEmpty.getTotalElements());
    }

    /**
     * 测试目标：存量单参查询保持不过滤身份的行为。
     * 前置条件：同一人员三条不同身份待办、他人一条。
     * 执行步骤：调用历史签名 findTodoRecordPage(userId, request)。
     * 期望断言：返回该人员全部 3 条，不含他人记录。
     */
    @Test
    void should_keep_legacy_query_unfiltered() {
        todoRepository.save(todo(1, OPERATOR_ID, ""));
        todoRepository.save(todo(2, OPERATOR_ID, "department1"));
        todoRepository.save(todo(3, OPERATOR_ID, "department2"));
        todoRepository.save(todo(4, 6, "department1"));

        Page<FlowRecordContent> legacy = queryService.findTodoRecordPage(OPERATOR_ID, PAGE);

        assertEquals(3, legacy.getTotalElements());
        List<String> identities = legacy.getContent().stream()
                .map(FlowRecordContent::getCurrentOperatorIdentity).toList();
        assertEquals(3, identities.size());
    }
}
