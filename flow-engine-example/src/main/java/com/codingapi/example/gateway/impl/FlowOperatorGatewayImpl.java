package com.codingapi.example.gateway.impl;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class FlowOperatorGatewayImpl implements FlowOperatorGateway {

    private final UserRepository userRepository;

    @Override
    public IFlowOperator get(long id) {
        return userRepository.getUserById(id);
    }

    /**
     * 按身份获取操作者（issue #288）。
     *
     * <p>示例组织模型里身份就是「以哪个部门/岗位的身份参与流程」，不属于用户主数据，
     * 因此这里返回携带身份标识的**用户副本**，而不改写持久化上下文中的实体实例。
     * 真实项目应把身份替换为自己的组织模型（例如按部门 id 查询岗位任职关系）。
     */
    @Override
    public IFlowOperator get(long id, String identity) {
        if (OperatorIdentity.isDefault(identity)) {
            return get(id);
        }
        User user = userRepository.getUserById(id);
        return user == null ? null : user.withIdentity(OperatorIdentity.normalize(identity));
    }

    @Override
    public List<IFlowOperator> findByIds(List<Long> ids) {
        return userRepository.findUserByIdIn(ids)
                .stream()
                .map(user -> (IFlowOperator) user)
                .toList();
    }

}
