package com.codingapi.flow.script.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyScriptUtils {

    /**
     * 清除脚本中的单行注释
     */

    public static String clearComments(String script) {
        if (script == null || script.isBlank()) {
            return "";
        }
        return script
                .replaceAll("//.*$", "")
                .trim();
    }

    /**
     * 提取脚本中的 return 表达式
     */

    public static String getReturnScript(String script) {
        try {
            String result = clearComments(script);
            // 匹配 def run(...) { ... }
            Pattern funcPattern = Pattern.compile(
                    "def\\s+run\\s*\\([^)]*\\)\\s*\\{([\\s\\S]*)\\}"
            );
            Matcher funcMatcher = funcPattern.matcher(result);
            if (funcMatcher.find()) {
                result = funcMatcher.group(1);
            }
            // 匹配 return xxx
            Pattern returnPattern = Pattern.compile(
                    "return\\s+(.+?);?\\s*$",
                    Pattern.MULTILINE
            );

            Matcher returnMatcher = returnPattern.matcher(result);
            if (returnMatcher.find()) {
                result = returnMatcher.group(1).trim();
            } else {
                // 如果没有找到 return 语句，可能整个脚本就是表达式
                result = result.trim();
            }
            return result;
        } catch (Exception e) {
            return "";
        }
    }
}
