import {FlowNode} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";
import {nanoid} from "nanoid";

export class WorkflowUtils {

    private readonly nodeTitles: Map<NodeType, string>;

    private readonly nodeMapping: Map<NodeType, NodeType>;

    private readonly nodes: FlowNode[];
    private readonly nodeList: FlowNode[];

    constructor() {
        this.nodeTitles = new Map<NodeType, string>();
        this.nodeTitles.set('START', '开始节点');
        this.nodeTitles.set('END', '结束节点');
        this.nodeTitles.set('APPROVAL', '审批节点');
        this.nodeTitles.set('HANDLE', '办理节点');
        this.nodeTitles.set('CONDITION', '条件控制');
        this.nodeTitles.set('CONDITION_BRANCH', '条件节点');
        this.nodeTitles.set('PARALLEL', '并行控制');
        this.nodeTitles.set('PARALLEL_BRANCH', '并行节点');
        this.nodeTitles.set('INCLUSIVE', '包容控制');
        this.nodeTitles.set('INCLUSIVE_BRANCH', '包容节点');
        this.nodeTitles.set('TRIGGER', '触发节点');
        this.nodeTitles.set('NOTIFY', '抄送节点');
        this.nodeTitles.set('ROUTER', '路由节点');
        this.nodeTitles.set('SUB_PROCESS', '子流程节点');

        this.nodeMapping = new Map<NodeType, NodeType>();
        this.nodeMapping.set('CONDITION', 'CONDITION_BRANCH');
        this.nodeMapping.set('INCLUSIVE', 'INCLUSIVE_BRANCH');
        this.nodeMapping.set('PARALLEL', 'PARALLEL_BRANCH');
        this.nodes = [];
        this.nodeList = [];
    }


    public createWorkflow() {
        return {
            id: nanoid(),
            title: '测试流程',
            code: 'test',
            strategies: [],
            edges: [],
            nodes: this.nodes,
            form: {
                name: 'test',
                code: 'test',
                subForms: [],
                fields: []
            }
        };
    }


    public createNode(nodeType: NodeType, id = (nodeType + "_" + nanoid(2))): FlowNode {
        const blocks: FlowNode[] = [];
        const branchNode = this.nodeMapping.get(nodeType);
        if (branchNode) {
            for (let i = 1; i <= 2; i++) {
                const block = this.createNode(branchNode, id + i);
                this.nodeList.push(block);
                blocks.push(block);
            }
        }
        const node = {
            id: id,
            name: this.nodeTitles.get(nodeType) || '',
            actions: [],
            strategies: [],
            type: nodeType,
            blocks: blocks,
            order: 0
        }
        this.nodeList.push(node);
        return node;
    }


    public createNodeAndSave(nodeType: NodeType, id = (nodeType + "_" + nanoid(2))) {
        const node = this.createNode(nodeType, id);
        this.nodes.push(node);
        return node;
    }


    public addBlock(sourceId: string, targetId: string, blockIndex: number) {
        const sourceNode = this.getNodeById(sourceId);
        const targetNode = this.getNodeById(targetId);
        if (sourceNode && targetNode) {
            const blocks = sourceNode.blocks||[];
            for (let i = 0; i < blocks.length; i++) {
                const block = blocks[i];
                if(i===blockIndex){
                    const children = block.blocks ||[];
                    block.blocks = [...children,targetNode];
                }
            }
        }
    }

    private getNodeById(id:string){
        for(const node of this.nodeList) {
            if(node.id === id){
                return node;
            }
        }
        return null;
    }

}