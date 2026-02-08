import {FlowEdge, FlowNode, Workflow} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";

export class WorkflowEdgeConvertor {

    private readonly workflow: Workflow;
    private readonly nodes: FlowNode[];

    private readonly filterNodes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];

    private readonly stopNodes: NodeType[] = ['END', 'ROUTER'];

    private readonly edges: FlowEdge[];
    preNodes: FlowNode[] = [];

    // 用于存储所有节点（包括嵌套在 blocks 中的）以便快速查找，但排除容器节点（与WorkflowNodeConvertor一致）
    private allNodes: FlowNode[] = [];

    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.nodes = workflow.nodes || [];
        this.edges = [];
        this.allNodes = this.collectAllNodes(this.nodes);
    }

    // 递归收集所有节点，包括嵌套在 blocks 中的，但排除容器节点（与WorkflowNodeConvertor一致）
    private collectAllNodes(nodes: FlowNode[]): FlowNode[] {
        const result: FlowNode[] = [];
        for (const node of nodes) {
            if (this.filterNodes.includes(node.type)) {
                // 容器节点本身不保留，但递归处理其blocks
                if (node.blocks && node.blocks.length > 0) {
                    result.push(...this.collectAllNodes(node.blocks));
                }
            } else {
                // 普通节点：保留，并递归处理其blocks
                result.push(node);
                if (node.blocks && node.blocks.length > 0) {
                    result.push(...this.collectAllNodes(node.blocks));
                }
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
        
        // 修复：移除 ROUTER 到 END 的边
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
        
        // 处理nextNodes时，需要递归展开所有嵌套的容器节点（CONDITION、PARALLEL、INCLUSIVE）
        const processedNodes: FlowNode[] = [];
        for (const node of nextNodes) {
            if (this.filterNodes.includes(node.type)) {
                // 如果是容器类型节点，直接递归其blocks
                if (node.blocks && node.blocks.length > 0) {
                    processedNodes.push(...this.loadNextBlocks(node.blocks));
                }
            } else {
                processedNodes.push(node);
            }
        }
        
        this.createToEdges(fromNode, processedNodes);
        
        for (const nextNode of processedNodes) {
            if (nextNode.blocks && nextNode.blocks.length > 0) {
                this.loadNextEdges(nextNode, nextNode.blocks);
            }
        }
    }

    private loadNextBlocks(blocks: FlowNode[]): FlowNode[] {
        const list: FlowNode[] = [];
        for (const item of blocks) {
            if (this.filterNodes.includes(item.type)) {
                // 容器节点（CONDITION、PARALLEL、INCLUSIVE）需要递归处理其blocks
                if (item.blocks && item.blocks.length > 0) {
                    list.push(...this.loadNextBlocks(item.blocks));
                }
            } else {
                // 普通节点直接添加，但不递归处理其blocks（避免重复添加）
                list.push(item);
            }
        }
        return list;
    }
}