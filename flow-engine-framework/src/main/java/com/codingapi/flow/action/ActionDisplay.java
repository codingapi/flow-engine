package com.codingapi.flow.action;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.codingapi.flow.script.action.ActionDisplayScript;
import com.codingapi.flow.session.FlowSession;

/**
 * 动作显示
 */
@Data
public class ActionDisplay {
    private String title;
    private String style;
    private String icon;
    private ActionDisplayScript script;

    private ActionDisplay(String title) {
        this.script = ActionDisplayScript.defaultScript();
        this.title = title;
    }

    public static ActionDisplay defaultDisplay(String title){
        return new ActionDisplay(title);
    }


    private ActionDisplay() {
    }


    public boolean show(FlowSession flowSession){
        if(this.script!=null){
            return this.script.execute(flowSession);
        }else{
            return true;
        }
    }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("style", style);
        map.put("icon", icon);
        map.put("script", script!=null?script.getScript():null);
        return map;
    }

    public static ActionDisplay fromMap(Map<String, Object> map) {
        ActionDisplay display = new ActionDisplay();
        display.setTitle((String) map.get("title"));
        display.setStyle((String) map.get("style"));
        display.setIcon((String) map.get("icon"));
        String script = (String) map.get("script");
        if(StringUtils.hasLength(script)){
            display.setScript(new ActionDisplayScript(script));
        }
        return display;
    }
}
