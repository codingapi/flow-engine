import {FlowEdge, FlowNode, Workflow} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";

export class WorkflowEdgeConvertor {

    private readonly workflow: Workflow;
    private readonly nodes: FlowNode[];

    private readonly blockNodeTypes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];
    private readonly branchNodeTypes: NodeType[] = ['CONDITION_BRANCH', 'PARALLEL_BRANCH', 'INCLUSIVE_BRANCH'];
    private readonly stopNodeTypes: NodeType[] = ['END', 'ROUTER'];

    private readonly edges: FlowEdge[];
    private readonly nodeList: FlowNode[];


    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.nodes = workflow.nodes || [];
        this.edges = [];
        this.nodeList = [];
        this.eachNodes(this.nodes);
    }

    public toEdges() {
        return this.edges;
    }


    private isBlocksNode(node: FlowNode) {
        return this.blockNodeTypes.includes(node.type);
    }

    private isBranchNode(node: FlowNode) {
        return this.branchNodeTypes.includes(node.type);
    }

    /**
     *
     * 遍历穿行的节点
     * @param nodes
     * @private
     */
    private eachNodes(nodes: FlowNode[]) {
        const length = nodes.length;
        for (let i = 0; i < length; i++) {
            const from = nodes[i];
            const to = (i + 1 < length) ? nodes[i + 1] : undefined;
            this.addEdge(from, to);
        }
    }

    /**
     * 遍历并行的节点
     * @param nodes
     * @private
     */
    private blockNodes(nodes: FlowNode[]) {
        for (const node of nodes) {
            const nextBlocks = node.blocks || [];
            if (nextBlocks.length > 0) {
                const to = nextBlocks[0];
                this.addEdge(node, to);
            }
            this.eachNodes(nextBlocks);
        }
    }

    private saveEdges(from: FlowNode, toNodes: FlowNode[]) {
        this.nodeList.push(from);
        for (const to of toNodes) {
            this.edges.push({
                from: from.id,
                to: to.id,
            });
            this.nodeList.push(to);
        }
    }


    private addEdge(from: FlowNode, to: FlowNode | undefined) {
        if (to) {
            // 如果是blocks的节点
            if (this.isBlocksNode(to)) {
                const blocks = to.blocks || [];
                this.saveEdges(from, blocks);

                this.blockNodes(blocks);
                return;
            }

            if (this.isBlocksNode(from)) {
                const overs = this.loadOverNodes(from);
                for (const over of overs) {
                    this.addEdge(over, to);
                }
                return;
            }

            this.saveEdges(from, [to]);
        }
    }


    private loadOverNodes(node: FlowNode) {
        const list: FlowNode[] = [];
        const overs: FlowNode[] = this.fetchOverNodes(node.blocks || []);
        for (const node of overs) {
            if (!this.stopNodeTypes.includes(node.type)) {
                list.push(node);
            }
        }
        return list;
    }


    private fetchOverNodes(nodes: FlowNode[]) {
        const list: FlowNode[] = [];
        for (const node of nodes) {
            const next = this.loadNextNodes(node);
            if (next.length === 0) {
                list.push(node);
            } else {
                const children = this.fetchOverNodes(next);
                if (children.length > 0) {
                    list.push(...children);
                }
            }
        }
        return list;
    }

    private loadNextNodes(node: FlowNode) {
        const toNodes: FlowNode[] = [];
        for (const edge of this.edges) {
            if (edge.from === node.id) {
                const to = this.getNodeById(edge.to);
                if (to) {
                    toNodes.push(to);
                }
            }
        }
        return toNodes;
    }


    private getNodeById(id: string) {
        for (const node of this.nodeList) {
            if (node.id === id) {
                return node;
            }
        }
        return null;
    }

}