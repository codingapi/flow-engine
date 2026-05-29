package com.codingapi.flow.script.request;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.springboot.script.annotation.ScriptFunction;
import com.codingapi.springboot.script.annotation.ScriptType;
import lombok.AllArgsConstructor;

/**
 *  流程发起请求对象 request
 *  def run(request){
 *      request.getOperatorName()
 *  }
 */
@AllArgsConstructor
@ScriptType(description = "流程发起请求验证对象")
public class GroovyWorkflowRequest {

    private final IFlowOperator currentOperator;
    private final Workflow workflow;

    @ScriptFunction(name = "getCurrentOperator",description = "当前操作人")
    public IFlowOperator getCurrentOperator() {
        return currentOperator;
    }

    @ScriptFunction(name = "getWorkflow",description = "流程对象")
    public Workflow getWorkflow() {
        return workflow;
    }
}
