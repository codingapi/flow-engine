package com.codingapi.flow.action;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ActionDisplay {
    private String title;
    private String style;
    private String icon;

    public ActionDisplay(String title) {
        this.title = title;
    }

    private ActionDisplay() {

    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("style", style);
        map.put("icon", icon);
        return map;
    }

    public static ActionDisplay fromMap(Map<String, Object> map) {
        ActionDisplay display = new ActionDisplay();
        display.setTitle((String) map.get("title"));
        display.setStyle((String) map.get("style"));
        display.setIcon((String) map.get("icon"));
        return display;
    }
}
