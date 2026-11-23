package com.codingapi.flow.operator;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 身份透传效果检查器（issue #288 衍生 / #294）。
 *
 * <p>框架通过 {@code default} 方法为身份能力提供向后兼容的退化实现，当下游没有覆盖带身份的取人方法时，
 * 身份会被静默丢弃：请求里带的是「部门A」，落库却变成默认身份，前端按身份查待办会得到空列表，
 * 且没有任何异常或日志。这类问题在集成阶段极难定位，因此在这里补一条运行期告警。
 *
 * <p>告警按 {@code (人员id, 身份)} 去重，并对条目总量设上限，避免高频调用场景刷爆日志或占用内存。
 */
@Slf4j
public final class OperatorIdentityGuard {

    /**
     * 告警去重记录的条目上限
     */
    private static final int MAX_WARN_KEYS = 256;

    private static final String KEY_SEPARATOR = "\u0001";

    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    private OperatorIdentityGuard() {
    }

    /**
     * 检查身份透传是否生效：请求的为非默认身份、但回查到操作者的身份为空时输出告警。
     *
     * @param operatorId        人员 id
     * @param requestedIdentity 请求声明的身份
     * @param resolved          回查到的操作者，可能为 {@code null}
     * @return true 表示本次实际输出了告警（同一 {@code (人员, 身份)} 只会返回一次 true）
     */
    public static boolean check(long operatorId, String requestedIdentity, IFlowOperator resolved) {
        if (!needWarn(requestedIdentity, resolved)) {
            return false;
        }
        if (WARNED_KEYS.size() >= MAX_WARN_KEYS) {
            return false;
        }
        String key = operatorId + KEY_SEPARATOR + OperatorIdentity.normalize(requestedIdentity);
        if (!WARNED_KEYS.add(key)) {
            return false;
        }
        log.warn("流程操作者身份未生效：人员[{}] 请求身份[{}]，但回查到的操作者身份为空。"
                        + "请确认下游已覆盖 FlowOperatorGateway#get(id, identity) 与 IRepositoryHolder#getOperatorById(id, identity)，"
                        + "否则同一人员的多身份将全部退化为默认身份（issue #288）。",
                operatorId, OperatorIdentity.normalize(requestedIdentity));
        return true;
    }

    /**
     * 是否需要告警：请求身份非默认，且回查结果的身份为默认。
     */
    static boolean needWarn(String requestedIdentity, IFlowOperator resolved) {
        if (OperatorIdentity.isDefault(requestedIdentity)) {
            return false;
        }
        if (resolved == null) {
            return false;
        }
        return OperatorIdentity.isDefault(OperatorIdentity.of(resolved));
    }

    /**
     * 清空告警去重记录，仅用于测试隔离。
     */
    public static void resetWarnedCache() {
        WARNED_KEYS.clear();
    }
}
