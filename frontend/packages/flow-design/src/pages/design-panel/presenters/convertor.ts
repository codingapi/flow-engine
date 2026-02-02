import {FlowEdge, FlowNode, Workflow} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";

export class WorkflowApiConvertor {

    private readonly workflow: Workflow;

    private readonly filterNodes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];

    public constructor(workflow: Workflow) {
        this.workflow = workflow;
    }

    public toApi() {
        // TODO 未完成，将blocks转化为edges
        return {
            ...this.workflow,
            nodes: this.loadNodes(),
            edges: this.loadEdges()
        };
    }

    public toRender() {
        // TODO 未完成，将edges转化为blocks
        return {
            ...this.workflow,
            nodes: this.workflow.nodes
        }
    }

    private loadNodes() {
        const list: FlowNode[] = [];
        const nodes = this.workflow.nodes || [];
        for (const node of nodes) {
            const items = this.fetchNodes(node);
            list.push(...items);
        }
        return list;
    }


    private fetchNodes(node: FlowNode) {
        const list: FlowNode[] = [];
        if (this.filterNodes.includes(node.type)) {
            const blocks = node.blocks || [];
            for (const block of blocks) {
                const items = this.fetchNodes(block);
                list.push(...items);
            }
        } else {
            list.push(node);
        }
        return list;
    }


    private loadEdges() {
        const nodes = this.workflow.nodes || [];
        const edges: FlowEdge[] = [];

        const length = nodes.length;
        for (let i = 0; i < nodes.length; i++) {
            const currentNode = nodes[i];
            const nextNode = i + 1 < length ? nodes[i + 1] : currentNode;
            const list = this.fetchEdges(currentNode, nextNode);
            edges.push(...list);
        }

        return edges;
    }


    private fetchEdges(currentNode: FlowNode, nextNode: FlowNode) {
        const edges: FlowEdge[] = [];
        const nextBlocks = nextNode.blocks || [];
        if (nextBlocks.length > 0) {
            for (let i = 0; i < nextBlocks.length; i++) {
                const currentBlock = nextBlocks[i];
                const list = this.fetchEdges(currentNode, currentBlock);
                edges.push(...list);
            }
        } else {
            if (nextNode.id !== currentNode.id) {
                edges.push({
                    form: currentNode.id,
                    to: nextNode.id,
                })
            }
        }
        return edges;
    }


}

