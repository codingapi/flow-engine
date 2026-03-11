package com.codingapi.flow.infra.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkflowVersionOption {

    private long id;
    private String versionName;
    private boolean current;
    private long updatedTime;

    public WorkflowVersionOption(long id, String versionName, boolean current,long updatedTime) {
        this.id = id;
        this.versionName = versionName;
        this.current = current;
        this.updatedTime = updatedTime;
    }


}
