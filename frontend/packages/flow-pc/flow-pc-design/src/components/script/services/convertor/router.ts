export class RouterConvertor {

    public static goNode(node:{label:string,value:string}){
        return `// @SCRIPT_TITLE ${node.label}  
def run(request){
    return '${node.value}';
}`
    };

}