package com.codingapi.example.gateway.impl;

import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
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

    @Override
    public List<IFlowOperator> findByIds(List<Long> ids) {
        return userRepository.findUserByIdIn(ids)
                .stream()
                .map(user -> (IFlowOperator) user)
                .toList();
    }

}
