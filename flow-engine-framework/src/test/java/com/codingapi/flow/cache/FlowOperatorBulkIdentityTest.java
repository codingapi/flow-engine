package com.codingapi.flow.cache;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 按身份批量取人的缓存与网关契约测试（issue #288 衍生 / #293、#295）。
 *
 * <p>批量加载必须按 {@code (id, identity)} 隔离缓存，否则同一批人以不同身份参与时
 * 会命中同一个对象，身份串味；同时验证未覆盖新方法的存量网关能安全退化为按 id 批量查询。
 */
class FlowOperatorBulkIdentityTest {

    private FlowOperatorLocalThreadCache cache;

    @BeforeEach
    void setUp() {
        cache = FlowOperatorLocalThreadCache.getInstance();
        cache.clear();
    }

    /**
     * 测试目标：两个身份分别回源、分别命中缓存，互不覆盖。
     * 前置条件：缓存为空，人员 1 有两个身份副本。
     * 执行步骤：以两个身份各批量加载一次，再重复加载第一个身份。
     * 期望断言：返回各自的操作者；重复加载命中缓存不回源，回源次数为 2。
     */
    @Test
    void should_isolate_bulk_load_by_identity() {
        User department1 = User.withIdentity(1, "user1", "department1");
        User department2 = User.withIdentity(1, "user1", "department2");
        AtomicInteger loads = new AtomicInteger();

        List<IFlowOperator> first = cache.find(List.of(1L), "department1", ids -> {
            loads.incrementAndGet();
            return List.of(department1);
        });
        List<IFlowOperator> second = cache.find(List.of(1L), "department2", ids -> {
            loads.incrementAndGet();
            return List.of(department2);
        });
        List<IFlowOperator> again = cache.find(List.of(1L), "department1", ids -> {
            loads.incrementAndGet();
            return List.of();
        });

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertSame(department1, first.get(0));
        assertSame(department2, second.get(0));
        assertSame(department1, again.get(0));
        assertEquals(2, loads.get(), "同一身份重复加载应命中缓存");
    }

    /**
     * 测试目标：默认身份的批量加载与历史单参 {@code find} 等价。
     * 前置条件：缓存为空。
     * 执行步骤：用三参重载、identity 传空串加载。
     * 期望断言：结果与默认身份键下的缓存一致。
     */
    @Test
    void should_share_cache_with_legacy_bulk_load_for_default_identity() {
        User user = new User(3, "user3");

        List<IFlowOperator> loaded = cache.find(List.of(3L), OperatorIdentity.DEFAULT,
                ids -> List.of(user));

        assertEquals(1, loaded.size());
        assertSame(user, cache.get(3, () -> null));
    }

    /**
     * 测试目标：下游未覆盖身份重载时安全退化为按 id 批量查询（向后兼容硬约束）。
     * 前置条件：网关只实现两个抽象方法。
     * 执行步骤：调用带身份的默认方法。
     * 期望断言：返回按 id 查询的结果，编译与行为均不破坏。
     */
    @Test
    void should_degrade_to_plain_find_by_ids_when_gateway_not_overridden() {
        FlowOperatorGateway legacyGateway = new FlowOperatorGateway() {
            @Override
            public IFlowOperator get(long id) {
                return new User(id, "user" + id);
            }

            @Override
            public List<IFlowOperator> findByIds(List<Long> ids) {
                return ids.stream().map(id -> (IFlowOperator) new User(id, "user" + id)).toList();
            }
        };

        List<IFlowOperator> operators = legacyGateway.findByIds(List.of(1L, 2L), "department1");

        assertEquals(2, operators.size());
        assertEquals("", OperatorIdentity.of(operators.get(0)));
    }
}
