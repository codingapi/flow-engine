package com.codingapi.flow.cache;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 操作者线程缓存的身份维度测试（issue #288 P2）。
 *
 * <p>缓存键必须是 {@code (userId, identity)}：以 userId 为唯一键时，同一人员的两个身份
 * 会命中同一个缓存对象，身份「串味」——department2 的审批人可能拿到 department1 的身份。
 */
class FlowOperatorLocalThreadCacheTest {

    private FlowOperatorLocalThreadCache cache;

    @BeforeEach
    void setUp() {
        cache = FlowOperatorLocalThreadCache.getInstance();
        cache.clear();
    }

    /**
     * 同一 userId 的两个身份必须分别缓存，互不覆盖。
     */
    @Test
    void should_isolate_cache_by_identity() {
        User department1 = User.withIdentity(1, "user1", "department1");
        User department2 = User.withIdentity(1, "user1", "department2");

        cache.sync(department1);
        cache.sync(department2);

        assertSame(department1, cache.get(1, "department1", () -> null));
        assertSame(department2, cache.get(1, "department2", () -> null));
        assertNotSame(cache.get(1, "department1", () -> null), cache.get(1, "department2", () -> null));
    }

    /**
     * null 与空串等价：命中同一条默认身份缓存。
     */
    @Test
    void should_treat_null_and_empty_identity_as_same_key() {
        User defaultUser = new User(1, "user1");
        cache.sync(defaultUser);

        assertSame(defaultUser, cache.get(1, null, () -> null));
        assertSame(defaultUser, cache.get(1, "", () -> null));
        assertSame(defaultUser, cache.get(1, () -> null));
    }

    /**
     * 身份未命中时走加载器，且加载结果按请求的身份登记。
     */
    @Test
    void should_load_and_cache_by_requested_identity() {
        User loaded = User.withIdentity(2, "user2", "department9");

        IFlowOperator operator = cache.get(2, "department9", () -> loaded);

        assertSame(loaded, operator);
        assertSame(loaded, cache.get(2, "department9", () -> null));
        // 另一个身份不会命中该缓存
        assertNull(cache.get(2, "department1", () -> null));
    }

    /**
     * 批量加载是标识无关路径：结果登记在默认身份键下，避免「审批人为空」。
     */
    @Test
    void should_cache_bulk_load_result_under_default_identity() {
        User identityUser = User.withIdentity(3, "user3", "department1");

        var list = cache.find(java.util.List.of(3L), ids -> java.util.List.of(identityUser));

        assertEquals(1, list.size());
        assertSame(identityUser, cache.get(3, () -> null));
    }
}
