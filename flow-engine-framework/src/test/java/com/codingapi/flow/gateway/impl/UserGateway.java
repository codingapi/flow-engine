package com.codingapi.flow.gateway.impl;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserGateway implements FlowOperatorGateway {

    private final Map<Long, User> cache = new HashMap<>();


    @Override
    public IFlowOperator get(long id) {
        return cache.get(id);
    }

    /**
     * 按身份返回用户副本，模拟下游「同一人员以不同身份参与流程」的组织模型（issue #288）。
     * <p>未声明身份时返回缓存中的用户本身，保持 P1 用例「用户自带默认身份」的行为。
     */
    @Override
    public IFlowOperator get(long id, String identity) {
        User user = cache.get(id);
        if (user == null || OperatorIdentity.isDefault(identity)) {
            return user;
        }
        return user.asIdentity(identity);
    }

    @Override
    public List<IFlowOperator> findByIds(List<Long> ids) {
        return cache.values().stream().filter(user -> ids.contains(user.getUserId()))
                .map(user -> (IFlowOperator) user)
                .toList();
    }


    public void save(User user) {
        cache.put(user.getUserId(), user);
    }
}
