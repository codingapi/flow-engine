/**
 *  路由脚本工具
 */
export class RouterScriptUtils {

    public static goNode(node:{label:string,value:string}){
        return `// @SCRIPT_TITLE ${node.label}  
def run(request){
    return '${node.value}';
}`
    };

}