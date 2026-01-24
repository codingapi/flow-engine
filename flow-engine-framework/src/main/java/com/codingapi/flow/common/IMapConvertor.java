package com.codingapi.flow.common;

import lombok.SneakyThrows;

import java.util.Map;

/**
 * map 转换
 */
public interface IMapConvertor {

    Map<String, Object> toMap();

    @SneakyThrows
    public static <T extends IMapConvertor> T fromMap(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) return null;
        return clazz.getDeclaredConstructor().newInstance();
    }
}
