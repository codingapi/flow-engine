package com.codingapi.flow.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

/**
 * 随机工具类
 */
public class RandomUtils {

    private final static RandomStringUtils randomString = RandomStringUtils.secure();
    private final static Random random = new Random();

    /**
     * 生成18位随机流程id
     */
    public static String generateStringId() {
        // 1000亿的数据范围内重复的概率0.027%（约 1/3700）
        return randomString.nextAlphanumeric(18);
    }


    public static String generateWorkflowCode() {
        return randomString.nextAlphanumeric(10);
    }

    public static int randomInt(int size) {
        return random.nextInt(size);
    }
}
