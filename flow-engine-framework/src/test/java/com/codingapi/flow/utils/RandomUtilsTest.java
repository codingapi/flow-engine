package com.codingapi.flow.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilsTest {

    @Test
    void generateStringId() {
        long count = 100_0000L;
        long start = System.currentTimeMillis();
        Set<String> sets = new HashSet<>();
        for (long i = 0; i < count; i++) {
            String id = RandomUtils.generateStringId();
            assertNotNull(id);
            assertEquals(18, id.length());
            assertFalse(sets.contains(id));
            sets.add(id);
        }
        long end = System.currentTimeMillis();
        System.out.println("generateStringId count:" + count + ",execute time:" + (end - start));
    }
}