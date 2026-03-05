/**
 *  错误异常处理脚本
 */
export class ErrorTriggerScriptUtils {

    public static setNode(node:{label:string,value:string}){
        return `// @SCRIPT_TITLE 回退至${node.label}
// @SCRIPT_META {"type":"node","node":"${node.value}"} 
def run(request){ 
    return $bind.createErrorThrow('${node.value}');
}`
    };

}