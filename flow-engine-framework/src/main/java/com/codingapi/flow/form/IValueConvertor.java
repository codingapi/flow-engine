package com.codingapi.flow.form;

public interface IValueConvertor {

    boolean support(DataType dataType);

    Object getValue(Object value);
}
