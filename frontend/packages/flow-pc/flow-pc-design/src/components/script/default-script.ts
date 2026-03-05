
// 默认发起人范围设置脚本，任意人员
export const SCRIPT_DEFAULT_OPERATOR_CREATE =
`// @SCRIPT_TITLE 任意人员
// @SCRIPT_META {"type":"any"}
def run(request){
    return true;
}
`;


// 默认操作人配置脚本，流程创建者
export const SCRIPT_DEFAULT_OPERATOR_LOAD =
`// @SCRIPT_TITLE 流程创建者  
// @SCRIPT_META {"type":"creator"}
def run(request){
    return [request.getCreatedOperator()]
}
`;

// 默认节点标题配置脚本，您有一条待办消息
export const SCRIPT_DEFAULT_NODE_TITLE =
`// @SCRIPT_TITLE 您有一条待办消息
def run(request){
    return "您有一条待办消息"
}
        `


// 默认异常触发，回退至开始节点
export const SCRIPT_DEFAULT_ERROR_TRIGGER =
`// @SCRIPT_TITLE 回退至开始节点
// @SCRIPT_META {"type":"node","node":"START"} 
def run(request){ 
    return $bind.createErrorThrow(request.getStartNode());
}
`

// 默认条件脚本，默认条件（允许执行）
export const SCRIPT_DEFAULT_CONDITION =
`// @SCRIPT_TITLE 默认条件（允许执行）
def run(request){
    return true;
}
`

// 默认路由脚本，发起节点
export const SCRIPT_DEFAULT_ROUTER =
`// @SCRIPT_TITLE 发起节点
// @SCRIPT_META {"node":"START"}
def run(request){
    return request.getStartNode().getId();
}
`

// 默认触发脚本，示例触发节点（打印触发日志）
export const SCRIPT_DEFAULT_TRIGGER =
`// @SCRIPT_TITLE 示例触发节点（打印触发日志） 
def run(request){ 
    print('hello trigger node.\\n'); 
}
`

// 子流程脚本，创建当前流程
export const SCRIPT_DEFAULT_SUB_PROCESS =
`// @SCRIPT_TITLE 创建当前流程  
def run(request){ 
    return request.toCreateRequest() 
}
`