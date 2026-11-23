package com.codingapi.flow.cache;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 流程操作者线程缓存。
 *
 * <p>同一个人员可以以不同「身份」参与流程（issue #288），因此缓存键必须是
 * {@code (userId, identity)} 二元组而不是 userId：以身份为维度隔离后，同一 id 的
 * 两个身份各自缓存各自的 {@link IFlowOperator} 实例，不会互相串味。
 *
 * <p>身份为 {@code null}/{@code ""}（默认身份）时与历史行为等价，等同于只按 userId 缓存。
 */
public class FlowOperatorLocalThreadCache {

    /**
     * userId 与 identity 之间的分隔符；使用不可见控制字符，避免与业务身份标识冲突
     */
    private static final String KEY_SEPARATOR = "\u0001";

    private final ThreadLocal<Map<String, IFlowOperator>> cache;

    private FlowOperatorLocalThreadCache() {
        this.cache = new ThreadLocal<>();
    }

    @Getter
    private final static FlowOperatorLocalThreadCache instance = new FlowOperatorLocalThreadCache();

    /**
     * 拼装缓存键，身份统一归一化，保证 {@code null} 与空串命中同一条缓存
     */
    private static String cacheKey(long userId, String identity) {
        return userId + KEY_SEPARATOR + OperatorIdentity.normalize(identity);
    }

    public void clear() {
        this.cache.remove();
    }

    private Map<String, IFlowOperator> getCache() {
        Map<String, IFlowOperator> operatorCaches = this.cache.get();
        if (operatorCaches == null) {
            operatorCaches = new HashMap<>();
        }
        return operatorCaches;
    }

    /**
     * 将操作者写入缓存，缓存键取自操作者自身的身份标识。
     */
    public void sync(IFlowOperator flowOperator) {
        Map<String, IFlowOperator> operatorCaches = this.getCache();
        operatorCaches.put(cacheKey(flowOperator.getUserId(), OperatorIdentity.of(flowOperator)), flowOperator);
        this.cache.set(operatorCaches);
    }

    /**
     * 读取操作者（默认身份）。
     */
    public IFlowOperator get(long id, Supplier<IFlowOperator> operatorLoader) {
        return this.get(id, OperatorIdentity.DEFAULT, operatorLoader);
    }

    /**
     * 按身份读取操作者，未命中时通过 {@code operatorLoader} 加载并回填缓存。
     *
     * @param id             人员 id
     * @param identity       身份标识，{@code null} 与空串等价
     * @param operatorLoader 缓存未命中时的加载器
     * @return 操作者，加载不到时返回 {@code null}
     */
    public IFlowOperator get(long id, String identity, Supplier<IFlowOperator> operatorLoader) {
        Map<String, IFlowOperator> operatorCaches = this.getCache();
        String key = cacheKey(id, identity);
        IFlowOperator operator = operatorCaches.get(key);
        if (operator != null) {
            return operator;
        }

        operator = operatorLoader.get();
        if (operator != null) {
            operatorCaches.put(key, operator);
            this.cache.set(operatorCaches);
        }
        return operator;
    }

    /**
     * 批量读取操作者（默认身份）。
     *
     * <p>批量加载没有身份维度（下游网关的 {@code findByIds} 只按 id 查询），
     * 因此这里固定使用默认身份作为缓存键，与历史行为一致。
     */
    public List<IFlowOperator> find(List<Long> ids, IFlowOperatorFinder flowOperatorFinder) {
        return this.find(ids, OperatorIdentity.DEFAULT, flowOperatorFinder);
    }

    /**
     * 按身份批量读取操作者（issue #288 衍生 / #293）。
     *
     * <p>用于「一批人员以同一个身份参与流程」的场景（例如同一部门下的多个审批人）：
     * 结果登记在 {@code (id, identity)} 键下，按同一身份读取时可直接命中，
     * 避免同一批人员被逐条回查。
     *
     * @param ids             人员 id 列表
     * @param identity        身份标识，{@code null} 与空串等价
     * @param operatorFinder  缓存未命中时的批量加载器
     * @return 操作者列表（按入参 ids 顺序，加载不到的 id 会被跳过）
     */
    public List<IFlowOperator> find(List<Long> ids, String identity, IFlowOperatorFinder operatorFinder) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        ids = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return null;
        }

        String normalizedIdentity = OperatorIdentity.normalize(identity);

        Map<String, IFlowOperator> operatorCaches = this.getCache();

        List<Long> queryIds = ids.stream()
                .filter((id) -> !operatorCaches.containsKey(cacheKey(id, normalizedIdentity)))
                .toList();

        if (!queryIds.isEmpty()) {
            List<IFlowOperator> newOperatorList = operatorFinder.find(queryIds);
            if (newOperatorList != null && !newOperatorList.isEmpty()) {
                for (IFlowOperator operator : newOperatorList) {
                    // 结果按「请求身份」登记，而不是操作者对象自带身份：
                    // 调用方是按该身份发起查询的，登记键必须与读取键一致，否则读取时必然落空
                    operatorCaches.put(cacheKey(operator.getUserId(), normalizedIdentity), operator);
                }
                this.cache.set(operatorCaches);
            }
        }

        // 按入参 ids 顺序返回，避免缓存命中情况影响结果顺序，保证多人审批顺序稳定
        List<IFlowOperator> operatorList = new ArrayList<>();
        for (long id : ids) {
            IFlowOperator operator = operatorCaches.get(cacheKey(id, normalizedIdentity));
            if (operator != null) {
                operatorList.add(operator);
            }
        }
        return operatorList;
    }


    public interface IFlowOperatorFinder {

        List<IFlowOperator> find(List<Long> ids);

    }

}
