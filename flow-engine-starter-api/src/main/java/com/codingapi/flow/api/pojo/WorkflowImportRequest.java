package com.codingapi.flow.api.pojo;

import com.codingapi.flow.transfer.WorkflowImportMode;
import lombok.Data;

/**
 * 流程导入请求。
 */
@Data
public class WorkflowImportRequest {

    private String file;

    private WorkflowImportMode mode = WorkflowImportMode.INCREMENTAL;
}
