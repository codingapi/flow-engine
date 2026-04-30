package com.codingapi.flow.form.convertor;

import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.IValueConvertor;

public class DoubleValueConvertor implements IValueConvertor {

    @Override
    public boolean support(DataType dataType) {
        return dataType==DataType.DOUBLE;
    }

    @Override
    public Object getValue(Object value) {
        if(value!=null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            }catch (Exception ignore){
                return null;
            }
        }
        return null;
    }
}
