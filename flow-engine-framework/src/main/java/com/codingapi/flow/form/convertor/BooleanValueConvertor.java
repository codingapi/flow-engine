package com.codingapi.flow.form.convertor;

import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.IValueConvertor;

public class BooleanValueConvertor implements IValueConvertor {

    @Override
    public boolean support(DataType dataType) {
        return dataType==DataType.BOOLEAN;
    }

    @Override
    public Object getValue(Object value) {
        return String.valueOf(value).equals("true");
    }
}
