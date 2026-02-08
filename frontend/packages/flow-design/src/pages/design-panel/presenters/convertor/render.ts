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

    private readonly nodeList: FlowNode[];
    private readonly nodeIdList: string[];

    private readonly filterNodes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];
    private readonly filterBranchNodes: NodeType[] = ['CONDITION_BRANCH', 'PARALLEL_BRANCH', 'INCLUSIVE_BRANCH'];


    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.nodes = workflow.nodes || [];
        this.edges = workflow.edges || [];
        this.nodeList = [];
        this.nodeIdList = [];
    }

    public toRender() {
        return {
            ...this.workflow,
            nodes: this.loadNodes(),
        };
    }

    private loadNodes() {
        const currentNode = this.getNodeByType('START');
        if (currentNode) {
            this.addNodeList(currentNode);
            this.fetchNextNode(currentNode);
        }
        return this.nodeList;
    }



    private fetchNextNode(currentNode: FlowNode) {
        let nextNodes: FlowNode[] = [];
        if (this.isCreateNextNodes(currentNode)) {
            nextNodes = this.createNextNodes(currentNode);
        } else {
            nextNodes = this.loadNextNodes(currentNode);
        }
        if (nextNodes.length > 0) {
            for (const nextNode of nextNodes) {
                this.addNodeList(nextNode);
                this.fetchNextNode(nextNode);
            }
        }
    }


    private addNodeList(...nodes: FlowNode[]) {
        for (const node of nodes) {
            if (!this.nodeIdList.includes(node.id)) {
                this.nodeList.push(node);
                this.nodeIdList.push(node.id);
            }
        }
    }

    private isStopNode(node: FlowNode) {
        return node.type === 'END' || node.type === 'ROUTER';
    }


    private loadOverBlocks(node: FlowNode) {
        const overBlocks: FlowNode[] = [];

        let blocks = node.blocks || [];

        if (blocks.length > 0) {
            for (const block of blocks) {
                const overs = this.loadOverBlocks(block);
                if (overs.length > 0) {
                    overBlocks.push(...overs);
                } else {
                    overBlocks.push(block);
                }
            }
        }

        return overBlocks;
    }


    private loadEdgeNodes(node: FlowNode) {
        const nextNodes: FlowNode[] = [];
        for (const edge of this.edges) {
            if (edge.from === node.id) {
                const toNode = this.getNodeById(edge.to);
                if (toNode) {
                    nextNodes.push(toNode);
                }
            }
        }
        return nextNodes;
    }

    private loadNextNodes(node: FlowNode): FlowNode[] {
        const nextNodes: FlowNode[] = this.loadEdgeNodes(node);

        if (nextNodes.length === 0) {
            if (node.blocks) {
                const overBlocks = this.loadOverBlocks(node);
                for (const over of overBlocks) {
                    const nextList = this.loadEdgeNodes(over);
                    for (const nextNode of nextList) {
                        if (!nextNodes.includes(nextNode)) {
                            nextNodes.push(nextNode);
                        }
                    }
                }
            }
        }

        return nextNodes;
    }


    private isCreateNextNodes(node:FlowNode){
        const nextNodes: FlowNode[] = [];

        for (const edge of this.edges) {
            if (edge.from === node.id) {
                const toNode = this.getNodeById(edge.to);
                if (toNode) {
                    if (toNode.type !== 'END') { // 只过滤 END 节点，保留 ROUTER 节点
                        nextNodes.push(toNode);
                    }
                }
            }
        }

        if (nextNodes.length > 1) {
            const nextNode = nextNodes[0];
            if (this.filterBranchNodes.includes(nextNode.type)) {
                return true;
            }
        }
        return false;
    }


    private createNextNodes(node: FlowNode): FlowNode[] {
        const nextNodes: FlowNode[] = [];

        for (const edge of this.edges) {
            if (edge.from === node.id) {
                const toNode = this.getNodeById(edge.to);
                if (toNode) {
                    if (toNode.type !== 'END') { // 只过滤 END 节点，保留 ROUTER 节点
                        nextNodes.push(toNode);
                    }
                }
            }
        }

        if (nextNodes.length > 1) {
            const nextNode = nextNodes[0];
            if (this.filterBranchNodes.includes(nextNode.type)) {
                const parentNodes = this.createParentNode(nextNode.type.split("_")[0] as NodeType);
                return [
                    {
                        ...parentNodes,
                        blocks: nextNodes.map(item => {
                            return {
                                ...item,
                                blocks: this.createNextNodes(item)
                            }
                        })
                    }
                ]
            }
        }

        return nextNodes;
    }

    private createParentNode(type: NodeType): FlowNode {
        return {
            id: nanoid(),
            type,
            name: filterNodeTitles.get(type) || '',
            order: 0,
            actions: [],
            strategies: [],
            blocks: []
        }
    }

    private getNodeById(id: string) {
        for (const node of this.nodes) {
            if (node.id === id) {
                return node;
            }
        }
        return undefined;
    }


    private getNodeByType(type: NodeType) {
        for (let node of this.nodes) {
            if (node.type === type) {
                return node;
            }
        }
        return undefined;
    }

}

