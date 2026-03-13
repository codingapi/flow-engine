package com.codingapi.flow.form.convertor;

import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.IValueConvertor;

public class IntegerValueConvertor implements IValueConvertor {

    @Override
    public boolean support(DataType dataType) {
        return dataType==DataType.INTEGER;
    }

    @Override
    public Object getValue(Object value) {
        return Integer.parseInt(String.valueOf(value));
    }
}
