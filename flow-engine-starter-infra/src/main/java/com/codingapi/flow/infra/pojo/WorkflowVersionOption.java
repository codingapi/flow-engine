package com.codingapi.flow.infra.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkflowVersionOption {

    private String label;
    private String value;
    private boolean current;

    public WorkflowVersionOption(long value, String label, boolean current) {
        this.value = String.valueOf(value);
        this.label = label;
        this.current = current;
    }


}
