package com.codingapi.flow.infra.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkflowOption {

    private String label;
    private String value;

    public WorkflowOption(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public WorkflowOption(String label, long value) {
        this.label = label;
        this.value = String.valueOf(value);
    }


}
