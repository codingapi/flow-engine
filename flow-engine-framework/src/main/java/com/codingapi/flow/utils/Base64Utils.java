package com.codingapi.flow.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Utils {

    public static String toJson(String base64) {
        if (base64 == null) {
            return null;
        }
        // 去掉前缀
        if (base64.contains(",")) {
            base64 = base64.substring(base64.indexOf(",") + 1);
        }
        try {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e) {
                decoded = Base64.getUrlDecoder().decode(base64);
            }
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Base64 string: " + base64.substring(0, 30), e);
        }
    }


}