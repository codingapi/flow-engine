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
        if(value!=null) {
            try {
                return Long.parseLong(String.valueOf(value));
            }catch (Exception ignore){
                return null;
            }
        }
        return null;
    }
}
