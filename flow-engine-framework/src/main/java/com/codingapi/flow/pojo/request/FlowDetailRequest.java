package com.codingapi.flow.pojo.request;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  流程详情请求
 */
@Data
@NoArgsConstructor
public class FlowDetailRequest {
    /**
     *  详情id，可以是workId或者是recordId
     */
    private String id;
    /**
     * 流程的操作人Id
     */
    private long operatorId;

    public boolean isCreateWorkflow() {
        return !id.matches("^[0-9]+$");
    }

    public FlowDetailRequest(long id,long operatorId){
        this.id = String.valueOf(id);
        this.operatorId = operatorId;
    }

    public FlowDetailRequest(String id, long operatorId) {
        this.id = id;
        this.operatorId = operatorId;
    }
}
