
// 默认发起人范围设置脚本，任意人员
export const DEFAULT_OPERATOR_CREATE_SCRIPT = `
    // @SCRIPT_TITLE 任意人员
    def run(request){
        return true;
    }
    `;


// 默认操作人配置脚本，流程创建者
export const DEFAULT_OPERATOR_LOAD_SCRIPT = `
    // @SCRIPT_TITLE 流程创建者  
    def run(request){
        return [request.getCreatedOperator()]
    }
    `;

// 默认节点标题配置脚本，您有一条待办消息
export const DEFAULT_NODE_TITLE_SCRIPT = `
        // @SCRIPT_TITLE 您有一条待办消息
        def run(request){
            return "您有一条待办消息"
        }
        `

