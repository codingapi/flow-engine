package com.codingapi.flow.script;

/**
 *  默认脚本定义
 */
public class ScriptDefaultConstants {

    /**
     *  默认自定义动作脚本
     */
    public static final String SCRIPT_DEFAULT_ACTION_CUSTOM = """
            // @SCRIPT_TITLE 默认条件 触发通过
            // @SCRIPT_META {"trigger":"PASS"}
            def run(request){
                return 'PASS';
            }
            """;


    /**
     *  默认拒绝动作脚本
     */
    public static final String SCRIPT_DEFAULT_ACTION_REJECT = """
            // @SCRIPT_TITLE 返回开始节点
            // @SCRIPT_META {"type":"START"}
            def run(request){
                return request.getStartNode().getId();
            }
            """;

    /**
     *  默认条件脚本
     */
    public static final String SCRIPT_DEFAULT_CONDITION = """
            // @SCRIPT_TITLE 默认条件（允许执行）
            def run(request){
                return true;
            }
            """;


    /**
     *  默认异常触发脚本
     */
    public static final String SCRIPT_DEFAULT_ERROR_TRIGGER = """
            // @SCRIPT_TITLE 回退至开始节点 
            // @SCRIPT_META {"type":"node","node":"START"}
            def run(request){ 
                return request.getStartNode().getId();
            }
            """;


    /**
     *  默认节点标题脚本
     */
    public static final String SCRIPT_DEFAULT_NODE_TITLE = """
            // @SCRIPT_TITLE 你有一条待办 
            def run(request){
                return '你有一条待办'
            }
            """;

    /**
     *  默认人员加载脚本
     */
    public static final String SCRIPT_DEFAULT_OPERATOR_LOAD = """
            // @SCRIPT_TITLE 流程创建者 
            // @SCRIPT_META {"type":"creator"}
            def run(request){
                return [request.getCreatedOperatorId()]
            }
            """;


    /**
     *  默认人员匹配脚本
     */
    public static final String SCRIPT_DEFAULT_OPERATOR_MATCH = """
            // @SCRIPT_TITLE 任意用户 
            // @SCRIPT_META {"type":"any"}
            def run(request){
                return true
            }
            """;


    /**
     * 默认路由脚本
     */
    public static final String SCRIPT_DEFAULT_ROUTER = """
            // @SCRIPT_TITLE 发起节点 
            // @SCRIPT_META {"node":"START"}
            def run(request){
                return request.getStartNode().getId();
            }
            """;


    /**
     *  默认子流程脚本
     */
    public static final String SCRIPT_DEFAULT_SUB_PROCESS = """
            // @SCRIPT_TITLE 创建当前流程 
            def run(request){ 
                return request.toCreateRequest() 
            }
            """;


    /**
     * 默认触发脚本
     */
    public static final String SCRIPT_DEFAULT_TRIGGER = """
            // @SCRIPT_TITLE 示例触发节点（打印触发日志） 
            def run(request){ 
                print('hello trigger node.\\n'); 
            }
            """;

}
