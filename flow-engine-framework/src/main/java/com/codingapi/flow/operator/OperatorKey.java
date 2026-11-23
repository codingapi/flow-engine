package com.codingapi.flow.operator;

/**
 * 操作者标识二元组 {@code (人员id, 身份)}（issue #288 衍生 / #292）。
 *
 * <p>同一个人员可以以不同身份参与流程，因此凡是「指向某个参与人」的持久化数据或批量参数，
 * 都不能只保存人员 id。该类型用于人员分配（发起人设定 / 审批人设定）等需要显式携带身份的场景。
 *
 * <p>身份统一经 {@link OperatorIdentity#normalize(String)} 归一化，{@code null} 与空串等价。
 *
 * @param operatorId 人员 id
 * @param identity   身份标识（默认身份为空串）
 */
public record OperatorKey(long operatorId, String identity) {

    /**
     * 紧凑构造：身份统一归一化，保证 {@code null} 与空串等价
     */
    public OperatorKey {
        identity = OperatorIdentity.normalize(identity);
    }

    /**
     * 构建操作者标识
     *
     * @param operatorId 人员 id
     * @param identity   身份标识，{@code null} 与空串等价
     * @return 操作者标识
     */
    public static OperatorKey of(long operatorId, String identity) {
        return new OperatorKey(operatorId, identity);
    }

    /**
     * 构建默认身份的操作者标识
     *
     * @param operatorId 人员 id
     * @return 操作者标识
     */
    public static OperatorKey ofDefault(long operatorId) {
        return new OperatorKey(operatorId, OperatorIdentity.DEFAULT);
    }

    /**
     * 组装可直接用于比较 / 缓存键的字符串
     *
     * @return {@code 人员id + '#' + 身份}
     */
    public String key() {
        return operatorId + OperatorIdentity.KEY_SEPARATOR + identity;
    }
}
