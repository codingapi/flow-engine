package com.codingapi.flow.pojo.body;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowAdviceBody {

    /**
     * 流程动作
     */
    private String action;
    /**
     * 审批意见
     */
    private String advice;
    /**
     * 创建者
     */
    private long operatorId;


    public void verify(){
        if(action==null||action.isEmpty()){
            throw new IllegalArgumentException("action can not be null");
        }
        if(operatorId<=0){
            throw new IllegalArgumentException("operatorId can not be null");
        }
    }
}
