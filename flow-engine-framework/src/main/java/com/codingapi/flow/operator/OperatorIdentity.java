package com.codingapi.flow.operator;

/**
 * 身份标识归一化工具。
 *
 * <p>流程引擎不解释身份标识的业务语义，只做透传、存储与比较。为了让「存量数据」与
 * 「未启用多身份的业务」保持完全一致的行为，统一约定：
 * <ul>
 *     <li>{@code null} 与空串等价，均视为「默认身份」；</li>
 *     <li>所有身份读取、写入、比较前都必须经过 {@link #normalize(String)}，禁止直接使用原始值比较；</li>
 *     <li>默认身份的存取不改变既有数据格式（例如待办合并键在默认身份下保持历史格式）。</li>
 * </ul>
 *
 * @see IFlowOperator#getIdentity()
 */
public final class OperatorIdentity {

    /**
     * 默认身份标识
     */
    public static final String DEFAULT = "";

    private OperatorIdentity() {
    }

    /**
     * 归一化身份标识：null 转为空串，其余原样返回。
     *
     * @param identity 原始身份标识
     * @return 归一化后的身份标识，不为 null
     */
    public static String normalize(String identity) {
        return identity == null ? DEFAULT : identity;
    }

    /**
     * 是否为默认身份（null 或空串）。
     *
     * @param identity 身份标识
     * @return true 表示默认身份
     */
    public static boolean isDefault(String identity) {
        return normalize(identity).isEmpty();
    }

    /**
     * 读取操作者的身份标识，操作者为 null 时返回默认身份。
     *
     * @param operator 流程参与用户
     * @return 归一化后的身份标识
     */
    public static String of(IFlowOperator operator) {
        return operator == null ? DEFAULT : normalize(operator.getIdentity());
    }

    /**
     * 判断两个身份标识是否相同（null 安全）。
     *
     * @param left  身份标识
     * @param right 身份标识
     * @return true 表示同一身份
     */
    public static boolean same(String left, String right) {
        return normalize(left).equals(normalize(right));
    }
}
