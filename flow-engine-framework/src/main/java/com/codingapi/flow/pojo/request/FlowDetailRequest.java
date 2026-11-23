package com.codingapi.flow.pojo.request;

import com.codingapi.flow.operator.OperatorIdentity;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  流程详情请求
 */
@Data
@NoArgsConstructor
public class FlowDetailRequest {
    /**
     *  详情id，可以是workCode或者是recordId
     */
    private String id;
    /**
     * 流程的操作人Id
     */
    private long operatorId;
    /**
     * 流程操作人的身份标识（issue #288），为空表示默认身份
     */
    private String operatorIdentity;

    public boolean isCreateWorkflow() {
        return !id.matches("^[0-9]+$");
    }

    public FlowDetailRequest(long id,long operatorId){
        this(id, operatorId, OperatorIdentity.DEFAULT);
    }

    public FlowDetailRequest(String id, long operatorId) {
        this(id, operatorId, OperatorIdentity.DEFAULT);
    }

    public FlowDetailRequest(long id, long operatorId, String operatorIdentity) {
        this(String.valueOf(id), operatorId, operatorIdentity);
    }

    public FlowDetailRequest(String id, long operatorId, String operatorIdentity) {
        this.id = id;
        this.operatorId = operatorId;
        this.operatorIdentity = operatorIdentity;
    }
}
