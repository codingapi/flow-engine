package com.codingapi.flow.utils;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * 随机工具类
 */
public class RandomUtils {

    private final static RandomStringUtils random = RandomStringUtils.secure();

    /**
     * 生成18位随机流程id
     */
    public static String generateStringId() {
        // 1000亿的数据范围内重复的概率0.027%（约 1/3700）
    	return random.nextAlphanumeric(18);
    }
}
