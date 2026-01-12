package com.codingapi.flow.utils;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * 随机工具类
 */
public class RandomUtils {

    private final static RandomStringUtils random = RandomStringUtils.secure();

    /**
     * 生成8位随机流程编码
     */
    public static String generateNodeCode() {
        return random.nextAlphanumeric(8);
    }

    /**
     * 生成18位随机流程id
     */
    public static String generateStringId() {
    	return random.nextAlphanumeric(18);
    }
}
