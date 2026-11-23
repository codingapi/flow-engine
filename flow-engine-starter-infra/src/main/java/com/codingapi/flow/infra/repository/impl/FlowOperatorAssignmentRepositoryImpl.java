package com.codingapi.flow.infra.repository.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.codingapi.flow.infra.entity.FlowOperatorAssignmentEntity;
import com.codingapi.flow.infra.jpa.FlowOperatorAssignmentEntityRepository;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.operator.OperatorKey;
import com.codingapi.flow.repository.FlowOperatorAssignmentRepository;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class FlowOperatorAssignmentRepositoryImpl implements FlowOperatorAssignmentRepository {

    private final FlowOperatorAssignmentEntityRepository jpa;

    @Override
    public void save(String processId, String nodeId, List<Long> operatorIds) {
        this.saveOperators(processId, nodeId, operatorIds.stream().map(OperatorKey::ofDefault).toList());
    }

    @Override
    public List<Long> findOperatorIds(String processId, String nodeId) {
        return this.findOperators(processId, nodeId).stream().map(OperatorKey::operatorId).toList();
    }

    /**
     * 保存分配关系，携带身份（issue #288 衍生 / #292）。
     *
     * <p>持久化格式保持向后兼容：全部为默认身份时仍写历史格式 {@code [1,2,3]}；
     * 存在非默认身份时写 {@code [{"operatorId":1,"identity":"department1"}]}，
     * 存量读取方（只认 id 数组）在最常见的默认身份场景下不受影响。
     */
    @Override
    public void saveOperators(String processId, String nodeId, List<OperatorKey> operators) {
        Optional<FlowOperatorAssignmentEntity> existing = jpa.findByProcessIdAndNodeId(processId, nodeId);
        FlowOperatorAssignmentEntity entity = existing.orElseGet(FlowOperatorAssignmentEntity::new);
        entity.setProcessId(processId);
        entity.setNodeId(nodeId);
        entity.setOperatorIds(toJson(operators));
        jpa.save(entity);
    }

    /**
     * 读取分配关系，携带身份（issue #288 衍生 / #292）。
     *
     * <p>同时兼容历史格式 {@code [1,2,3]}（按默认身份处理）与带身份格式。
     */
    @Override
    public List<OperatorKey> findOperators(String processId, String nodeId) {
        return jpa.findByProcessIdAndNodeId(processId, nodeId)
                .map(e -> fromJson(e.getOperatorIds()))
                .orElse(Collections.emptyList());
    }

    private static String toJson(List<OperatorKey> operators) {
        boolean hasIdentity = operators.stream().anyMatch(operator -> !OperatorIdentity.isDefault(operator.identity()));
        if (!hasIdentity) {
            // 默认身份保持历史格式，避免存量读取方（按 id 数组解析）失效
            return JSON.toJSONString(operators.stream().map(OperatorKey::operatorId).toList());
        }
        JSONArray array = new JSONArray();
        for (OperatorKey operator : operators) {
            JSONObject item = new JSONObject();
            item.put("operatorId", operator.operatorId());
            item.put("identity", operator.identity());
            array.add(item);
        }
        return array.toJSONString();
    }

    private static List<OperatorKey> fromJson(String text) {
        JSONArray array = JSON.parseArray(text);
        if (array == null || array.isEmpty()) {
            return Collections.emptyList();
        }
        List<OperatorKey> operators = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            Object item = array.get(i);
            if (item instanceof JSONObject jsonObject) {
                operators.add(OperatorKey.of(
                        jsonObject.getLongValue("operatorId"),
                        jsonObject.getString("identity")));
            } else if (item instanceof Number number) {
                operators.add(OperatorKey.ofDefault(number.longValue()));
            }
        }
        return operators;
    }
}
