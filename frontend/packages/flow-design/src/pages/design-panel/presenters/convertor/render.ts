import {FlowEdge, FlowNode, Workflow} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";
import {nanoid} from "nanoid";

const filterNodeTitles = new Map<string, string>();

filterNodeTitles.set('CONDITION', '条件控制');
filterNodeTitles.set('PARALLEL', '并行控制');
filterNodeTitles.set('INCLUSIVE', '包容控制');


export class WorkflowRenderConvertor {

    private readonly workflow: Workflow;
    private readonly nodes: FlowNode[];
    private readonly edges: FlowEdge[];

    private readonly blockNodes: FlowNode[];

    private readonly blockNodeTypes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];
    private readonly branchNodeTypes: NodeType[] = ['CONDITION_BRANCH', 'PARALLEL_BRANCH', 'INCLUSIVE_BRANCH'];


    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.nodes = workflow.nodes || [];
        this.edges = workflow.edges || [];
        this.blockNodes = [];
        this.loadNodes();
    }

    public toRender() {
        return {
            ...this.workflow,
            nodes: this.blockNodes,
        };
    }


    private loadNodes() {
        let currentNodes = this.loadFirstNodes();
        if (currentNodes) {
            this.blockNodes.push(...currentNodes);
            let nextNodes = this.loadNextNodes(currentNodes);
            while (nextNodes.length > 0) {
                this.blockNodes.push(...nextNodes);
                currentNodes = nextNodes;
                nextNodes = this.loadNextNodes(currentNodes);
            }
        }
    }

    private isBranchNode(nodes: FlowNode[]) {
        if (nodes.length > 0) {
            for (const node of nodes) {
                if (this.branchNodeTypes.includes(node.type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private createBlocksNode(nodes:FlowNode[]):FlowNode[]{
        if(nodes.length > 0){
            const node = nodes[0];
            const blockType = node.type.split("_")[0] as NodeType;
            return [
                {
                    id: nanoid(),
                    type:blockType,
                    name: filterNodeTitles.get(blockType) || '',
                    order: 0,
                    actions: [],
                    strategies: [],
                    blocks: this.appendNodeBlocks(nodes),
                }
            ]
        }
        return [];
    }


    private appendNodeBlocks(blocks:FlowNode[]){
        for (const block of blocks) {
            block.blocks = this.loadNextNodes([block]);
        }
        return blocks;
    }


    private loadNextNodes(nodes:FlowNode[]){
        const list = this.loadNextEdgeNodes(nodes);
        if(this.isBranchNode(list)){
            return this.createBlocksNode(list);
        }
        return list;
    }

    private loadNextEdgeNodes(nodes: FlowNode[]) {
        const list: FlowNode[] = [];
        const nodeIds:string[] = [];
        if (nodes.length > 0) {
            const node = nodes[0];
            for (const edge of this.edges) {
                if (edge.from === node.id) {
                    const node = this.getNodeById(edge.to);
                    if (node) {
                        if(!nodeIds.includes(node.id)) {
                            list.push(node);
                            nodeIds.push(node.id);
                        }
                    }
                }
            }
        }
        return list;
    }


    private isNode(nodes:FlowNode[]){
        if (nodes.length > 0){
            if(nodes.length ===1){

            }
        }
        return false;
    }


    /**
     *  分析加载第一个节点
     */
    private loadFirstNodes() {
        const map = new Map<string, number>;
        for (const edge of this.edges) {
            const to = edge.to;
            const count = map.get(to) || 0;
            map.set(to, count + 1);
        }

        for (const edge of this.edges) {
            const from = edge.from;
            if (!map.has(edge.from)) {
                const node = this.getNodeById(from);
                if (node) {
                    return [node];
                }
            }
        }
        return [];
    }


    private getNodeById(id: string) {
        for (const node of this.nodes) {
            if (node.id === id) {
                return node;
            }
        }
        return undefined;
    }


}

