package com.codingapi.flow.form.convertor;

import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.IValueConvertor;

public class StringValueConvertor implements IValueConvertor {

    @Override
    public boolean support(DataType dataType) {
        return dataType==DataType.STRING;
    }

    @Override
    public Object getValue(Object value) {
        return String.valueOf(value);
    }
}
