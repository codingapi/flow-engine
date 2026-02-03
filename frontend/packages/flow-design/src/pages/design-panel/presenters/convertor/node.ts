import {FlowNode, Workflow} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";

export class WorkflowNodeConvertor {

    private readonly workflow: Workflow;
    private readonly nodes: FlowNode[];

    private readonly filterNodes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];

    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.nodes = workflow.nodes || [];
    }

    public toNodes() {
        return this.loadNodes();
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

}

