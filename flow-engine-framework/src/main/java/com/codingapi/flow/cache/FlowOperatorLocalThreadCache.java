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

        Map<String, IFlowOperator> operatorCaches = this.getCache();

        List<Long> queryIds = ids.stream()
                .filter((id) -> !operatorCaches.containsKey(cacheKey(id, OperatorIdentity.DEFAULT)))
                .toList();

        if (!queryIds.isEmpty()) {
            List<IFlowOperator> newOperatorList = flowOperatorFinder.find(queryIds);
            if (newOperatorList != null && !newOperatorList.isEmpty()) {
                for (IFlowOperator operator : newOperatorList) {
                    // 批量加载是「按 id 查询」的标识无关路径：结果统一登记在默认身份键下，
                    // 与下面按默认身份读取保持一致。否则下游返回自带身份的操作者会被登记到身份键，
                    // 读取时按默认键取不到，表现为「审批人为空」。
                    operatorCaches.put(cacheKey(operator.getUserId(), OperatorIdentity.DEFAULT), operator);
                }
                this.cache.set(operatorCaches);
            }
        }

        // 按入参 ids 顺序返回，避免缓存命中情况影响结果顺序，保证多人审批顺序稳定
        List<IFlowOperator> operatorList = new ArrayList<>();
        for (long id : ids) {
            IFlowOperator operator = operatorCaches.get(cacheKey(id, OperatorIdentity.DEFAULT));
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
