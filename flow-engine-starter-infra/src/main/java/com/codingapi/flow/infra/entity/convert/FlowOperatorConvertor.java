package com.codingapi.flow.infra.entity.convert;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import jakarta.persistence.AttributeConverter;

public class FlowOperatorConvertor implements AttributeConverter<IFlowOperator,Long> {

    @Override
    public Long convertToDatabaseColumn(IFlowOperator operator) {
        if (operator == null) {
            return null;
        }
        return operator.getUserId();
    }

    @Override
    public IFlowOperator convertToEntityAttribute(Long dbData) {
        if(dbData==null || dbData<=0){
            return null;
        }
        return GatewayContext.getInstance().getFlowOperator(dbData);
    }
}
