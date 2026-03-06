/**
 *  路由脚本工具
 */
export class RouterScriptUtils {

    public static update(node:{label:string,value:string}){
        return `// @SCRIPT_TITLE ${node.label}  
// @SCRIPT_META {"node":"${node.value}"} 
def run(request){
    return '${node.value}';
}`
    };

}