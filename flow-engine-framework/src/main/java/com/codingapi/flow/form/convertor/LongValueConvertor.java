package com.codingapi.flow.form.convertor;

import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.IValueConvertor;

public class LongValueConvertor implements IValueConvertor {

    @Override
    public boolean support(DataType dataType) {
        return dataType==DataType.LONG;
    }

    @Override
    public Object getValue(Object value) {
        return Long.parseLong(String.valueOf(value));
    }
}
