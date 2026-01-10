package com.codingapi.flow.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilsTest {

    @Test
    void generateCode() {
        String code = RandomUtils.generateNodeCode();
        assertNotNull(code);
        assertEquals(8, code.length());
        System.out.println(code);
    }
}