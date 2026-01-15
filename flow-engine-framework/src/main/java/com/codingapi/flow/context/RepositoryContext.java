package com.codingapi.flow.context;

import com.codingapi.flow.repository.FlowOperatorRepository;
import com.codingapi.flow.user.IFlowOperator;
import lombok.Getter;
import lombok.Setter;

public class RepositoryContext {

    @Getter
    private final static RepositoryContext instance = new RepositoryContext();

    private RepositoryContext() {
    }

    @Setter
    @Getter
    private FlowOperatorRepository flowOperatorRepository;

    public IFlowOperator getFlowOperator(long userId) {
        return flowOperatorRepository.get(userId);
    }

}
