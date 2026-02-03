import {FlowEdge, FlowNode, Workflow} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";

export class WorkflowApiConvertor {

    private readonly workflow: Workflow;
    private readonly nodes: FlowNode[];

    private readonly filterNodes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];

    private readonly edges: FlowEdge[];

    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.nodes = workflow.nodes || [];
        this.edges = [];
    }

    public toApi() {
        return {
            ...this.workflow,
            edges: this.loadEdges(),
            nodes: this.loadNodes(),
        };
    }

    private loadNodes() {
        return this.fetchNodes(this.nodes);
    }

    private fetchNodes(nodes: FlowNode[]) {
        const list: FlowNode[] = [];
        for (const item of nodes) {
            if (this.filterNodes.includes(item.type)) {
                const blocks = item.blocks || [];
                let nextNodes = this.fetchNodeBlocks(blocks);
                if (nextNodes.length > 0) {
                    list.push(...nextNodes);
                }
            } else {
                list.push(item);
            }
        }
        return list;
    }

    private fetchNodeBlocks(blocks: FlowNode[]) {
        const list: FlowNode[] = [];
        for (const item of blocks) {
            const blocks = item.blocks || [];
            if (blocks.length > 0) {
                if (!this.filterNodes.includes(item.type)) {
                    list.push(item);
                }
                let nextNodes = this.fetchNodeBlocks(blocks);
                if (nextNodes.length > 0) {
                    list.push(...nextNodes);
                }
            } else {
                list.push(item);
            }
        }
        return list;
    }

    preNodes: FlowNode[] = [];
    private loadEdges() {

        for (let i = 0; i < this.nodes.length - 1; i++) {
            const fromNode = this.nodes[i];
            const nextNodes = this.loadNextNodes(fromNode);
            if(this.filterNodes.includes(fromNode.type)) {
                this.createFromEdges(this.preNodes, nextNodes[0]);
                this.preNodes.splice(0,this.preNodes.length);
            }else {
                this.preNodes.splice(0,this.preNodes.length);
                this.createToEdges(fromNode, nextNodes);
            }

            for (const nextNode of nextNodes) {
                this.loadNextEdges(nextNode, this.loadNextBlocks(nextNode.blocks || []));
            }
        }
        return this.edges;
    }

    private loadNextNodes(node: FlowNode): FlowNode[] {
        const nextNodes: FlowNode[] = [];
        let matchNode = false;
        for (const item of this.nodes) {
            if (matchNode) {
                if (this.filterNodes.includes(item.type)) {
                    const blocks = item.blocks || [];
                    nextNodes.push(...blocks);
                } else {
                    nextNodes.push(item);
                }
                break;
            }
            if (item.id === node.id) {
                matchNode = true;
            }
        }
        return nextNodes;
    }

    private createToEdges(fromNode: FlowNode, toNodes: FlowNode[]) {
        this.preNodes.push(...toNodes);
        for (const nextNode of toNodes) {
            this.edges.push({
                from: fromNode.id,
                to: nextNode.id,
            });
        }
    }

    private createFromEdges(fromNodes: FlowNode[], toNode: FlowNode) {
        for (const fromNode of fromNodes) {
            const fromNodeIds = this.edges.map(edge => {return edge.from});
            if(!fromNodeIds.includes(fromNode.id)) {
                this.edges.push({
                    from: fromNode.id,
                    to: toNode.id,
                });
            }
        }
    }


    private loadNextEdges(fromNode: FlowNode, nextNodes: FlowNode[]) {
        this.createToEdges(fromNode, nextNodes);
        for (const nextNode of nextNodes) {
            if (nextNode.blocks) {
                this.loadNextEdges(nextNode, nextNode.blocks);
            }
        }
    }


    private loadNextBlocks(blocks: FlowNode[]) {
        const list: FlowNode[] = [];
        for (const item of blocks) {
            if (this.filterNodes.includes(item.type)) {
                list.push(...this.loadNextBlocks(item.blocks || []));
            } else {
                list.push(item);
            }
        }
        return list;
    }
}

