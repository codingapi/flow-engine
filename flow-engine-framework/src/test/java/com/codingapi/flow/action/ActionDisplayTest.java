package com.codingapi.flow.action;

import com.codingapi.flow.action.factory.FlowActionFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 动作展示（按钮样式）序列化测试
 */
class ActionDisplayTest {

    @Test
    void should_keep_button_style_when_action_round_trip() {
        // given 设计器保存的按钮样式（issue #244）：样式保存在 display.style 的 JSON 字符串中
        String style = "{\"backgroundColor\":\"1677ff\",\"borderColor\":\"ff4d4f\",\"borderRadius\":\"4\",\"borderSize\":\"1\"}";

        Map<String, Object> display = new HashMap<>();
        display.put("title", "通过");
        display.put("style", style);
        display.put("icon", "");
        display.put("script", "");

        Map<String, Object> data = new HashMap<>();
        data.put("id", "action-1");
        data.put("type", "CUSTOM");
        data.put("title", "通过");
        data.put("enable", true);
        data.put("display", display);

        // when 保存（fromMap）后再加载（toMap）
        IFlowAction action = FlowActionFactory.getInstance().createAction(data);
        Map<String, Object> map = action.toMap();

        // then 样式不丢失
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) map.get("display");
        assertEquals(style, result.get("style"));
    }
}
