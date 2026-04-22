package com.codingapi.flow.script.request;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  流程groovy脚本请求对象 request
 *  def run(request){
 *      request.getOperatorName()
 *  }
 */
@Getter
@AllArgsConstructor
public class GroovyWorkflowRequest {

    private final IFlowOperator currentOperator;
    private final Workflow workflow;

}
