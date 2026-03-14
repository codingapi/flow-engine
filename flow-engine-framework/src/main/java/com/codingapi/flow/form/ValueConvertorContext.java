package com.codingapi.flow.form;

import com.codingapi.flow.form.convertor.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ValueConvertorContext {

    private final List<IValueConvertor> convertors;

    @Getter
    private final static ValueConvertorContext instance = new ValueConvertorContext();

    private ValueConvertorContext(){
        this.convertors = new ArrayList<>();
        this.init();
    }

    public void addConvert(IValueConvertor valueConvertor){
        this.convertors.add(valueConvertor);
    }

    public void clear(){
        this.convertors.clear();
    }

    private void init(){
        this.convertors.add(new StringValueConvertor());
        this.convertors.add(new IntegerValueConvertor());
        this.convertors.add(new BooleanValueConvertor());
        this.convertors.add(new LongValueConvertor());
        this.convertors.add(new DoubleValueConvertor());
    }

    public Object convert(DataType dataType,Object value){
        for (IValueConvertor convertor:this.convertors){
            if(convertor.support(dataType)){
                return convertor.getValue(value);
            }
        }
        return value;
    }
}
