import {FlowNode, Workflow} from "@/pages/design-panel/types";
import {NodeType} from "@/components/editor/typings/node-type";

/**
 * WorkflowNodeConvertor - 将原始workflow数据转换为节点列表
 * 
 * 核心规则：
 * 1. 排除容器节点（CONDITION、PARALLEL、INCLUSIVE）
 * 2. 保留所有业务节点和分支节点
 * 3. 递归处理嵌套结构
 */
export class WorkflowNodeConvertor {

    private readonly workflow: Workflow;
    private readonly nodes: FlowNode[];

    // 需要排除的容器节点类型
    private readonly containerNodes: NodeType[] = ['CONDITION', 'PARALLEL', 'INCLUSIVE'];

    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.nodes = workflow.nodes || [];
    }

    /**
     * 获取所有有效节点（排除容器节点）
     */
    public toNodes(): FlowNode[] {
        const result: FlowNode[] = [];
        const seen = new Set<string>();
        
        for (const node of this.nodes) {
            this.collectNodes(node, result, seen);
        }
        
        return result;
    }

    /**
     * 递归收集节点
     * 
     * 规则：
     * - 容器节点（CONDITION/PARALLEL/INCLUSIVE）本身不保留，但递归处理其blocks
     * - 其他节点保留，并递归处理其blocks
     */
    private collectNodes(node: FlowNode, result: FlowNode[], seen: Set<string>): void {
        // 避免重复处理
        if (seen.has(node.id)) {
            return;
        }
        seen.add(node.id);
        
        if (this.containerNodes.includes(node.type)) {
            // 容器节点本身不保留，但递归处理其blocks
            if (node.blocks && node.blocks.length > 0) {
                for (const block of node.blocks) {
                    this.collectNodes(block, result, seen);
                }
            }
        } else {
            // 普通节点：保留，并递归处理其blocks
            result.push(node);
            
            // 递归处理blocks中的节点
            if (node.blocks && node.blocks.length > 0) {
                for (const block of node.blocks) {
                    this.collectNodes(block, result, seen);
                }
            }
        }
    }

}
