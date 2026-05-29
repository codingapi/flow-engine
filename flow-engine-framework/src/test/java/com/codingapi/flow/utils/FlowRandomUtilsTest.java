package com.codingapi.flow.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlowRandomUtilsTest {

    private final static RandomStringUtils randomString = RandomStringUtils.secure();

    @Test
    void generateStringId() {
        long count = 100_0000L;
        long start = System.currentTimeMillis();
        Set<String> sets = new HashSet<>();
        for (long i = 0; i < count; i++) {
            String id = randomString.nextAlphanumeric(18);
            assertNotNull(id);
            assertEquals(18, id.length());
            assertFalse(sets.contains(id));
            sets.add(id);
        }
        long end = System.currentTimeMillis();
        System.out.println("generateStringId count:" + count + ",execute time:" + (end - start));
    }
}