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
        if(value!=null) {
            try {
                return String.valueOf(value).equals("true");
            } catch (Exception ignore) {
                return null;
            }
        }
        return null;
    }
}
