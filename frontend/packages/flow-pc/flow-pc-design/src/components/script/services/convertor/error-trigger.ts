export class ErrorTriggerConvertor {

    public static goNode(node:{label:string,value:string}){
        return `// @SCRIPT_TITLE 回退至${node.label} 
def run(request){ 
    return $bind.createErrorThrow('${node.value}');
}`
    };

}