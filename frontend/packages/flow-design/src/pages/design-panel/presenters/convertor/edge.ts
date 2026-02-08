import {FlowEdge, FlowNode, Workflow} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";

export class WorkflowEdgeConvertor {

    private readonly workflow: Workflow;
    private readonly nodes: FlowNode[];

    private readonly filterNodes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];

    private readonly stopNodes: NodeType[] = ['END', 'ROUTER'];

    private readonly edges: FlowEdge[];
    preNodes: FlowNode[] = [];

    // 用于存储所有节点（包括嵌套在 blocks 中的）以便快速查找
    private allNodes: FlowNode[] = [];

    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.nodes = workflow.nodes || [];
        this.edges = [];
        this.allNodes = this.collectAllNodes(this.nodes);
    }

    // 递归收集所有节点，包括嵌套在 blocks 中的
    private collectAllNodes(nodes: FlowNode[]): FlowNode[] {
        const result: FlowNode[] = [];
        for (const node of nodes) {
            result.push(node);
            if (node.blocks && node.blocks.length > 0) {
                result.push(...this.collectAllNodes(node.blocks));
            }
        }
        return result;
    }

    public toEdges() {
        return this.loadEdges();
    }


    private loadEdges() {

        for (let i = 0; i < this.nodes.length - 1; i++) {
            const fromNode = this.nodes[i];

            // 如果是终止节点，跳过创建边
            if(this.stopNodes.includes(fromNode.type)) {
                continue;
            }

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
        
        // 修复：确保只有一个审批节点到 END 的边，同时移除 ROUTER 到 END 的边
        const endEdges = this.edges.filter(edge => {
            const fromNode = this.allNodes.find(node => node.id === edge.from);
            const toNode = this.allNodes.find(node => node.id === edge.to);
            return toNode?.type === 'END';
        });
        
        // 移除 ROUTER 到 END 的边
        const routerToEndEdges = endEdges.filter(edge => {
            const fromNode = this.allNodes.find(node => node.id === edge.from);
            return fromNode?.type === 'ROUTER';
        });
        
        for (const edge of routerToEndEdges) {
            const index = this.edges.findIndex(e => e.from === edge.from && e.to === edge.to);
            if (index !== -1) {
                this.edges.splice(index, 1);
            }
        }
        
        // 确保只有一个审批节点到 END 的边
        const approvalToEndEdges = this.edges.filter(edge => {
            const fromNode = this.allNodes.find(node => node.id === edge.from);
            const toNode = this.allNodes.find(node => node.id === edge.to);
            return fromNode?.type === 'APPROVAL' && toNode?.type === 'END';
        });
        
        if (approvalToEndEdges.length > 1) {
            // 只保留第一条到 END 的边
            for (let i = 1; i < approvalToEndEdges.length; i++) {
                const index = this.edges.findIndex(edge => edge.from === approvalToEndEdges[i].from && edge.to === approvalToEndEdges[i].to);
                if (index !== -1) {
                    this.edges.splice(index, 1);
                }
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
        // 如果 fromNode 是终止节点，不创建任何边
        if (this.stopNodes.includes(fromNode.type)) {
            return;
        }
        this.createToEdges(fromNode, nextNodes);
        for (const nextNode of nextNodes) {
            if (nextNode.blocks && nextNode.blocks.length > 0) {
                // 对于容器节点，不递归处理其子节点（子节点会在其他地方处理）
                if (!this.filterNodes.includes(nextNode.type)) {
                    this.loadNextEdges(nextNode, nextNode.blocks);
                }
            }
        }
    }


    private loadNextBlocks(blocks: FlowNode[]) {
        const list: FlowNode[] = [];
        for (const item of blocks) {
            if (this.filterNodes.includes(item.type)) {
                list.push(...this.loadNextBlocks(item.blocks || []));
            } else {
                // 保留所有非过滤节点，包括 ROUTER（但 ROUTER 节点不会有输出边）
                list.push(item);
            }
        }
        return list;
    }
}

