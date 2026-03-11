package com.codingapi.flow.api.pojo;

import lombok.Data;

@Data
public class WorkflowUpdateVersionNameRequest {

    private long id;
    private String versionName;
}
