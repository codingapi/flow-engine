import {FlowNode} from "@/components/design-panel/types";

/**
 *  节点数据转换服务
 */
export class NodeConvertorManager {

    public static readonly STRATEGY_SUFFIX = 'Strategy';
    public static readonly STRATEGY_KEY = 'strategyType';

    public toRender(nodes: FlowNode[]) {
        return nodes.map(node => {
            return this.toItemRender(node);
        });
    }

    public toItemRender(node: FlowNode): any {
        const blocks: any[] = node?.blocks || [];
        return {
            id: node.id,
            type: node.type,
            data: {
                title: node.name,
                order: node.order,
                actions: node.actions,
                script: node.script,
                view: node.view,
                ...this.toStrategyRender(node),
            },
            blocks: blocks.map(item => this.toItemRender(item))
        }
    }

    public toData(nodes: any[]) {
        return nodes.map(node => {
            return this.toDataItem(node);
        });
    }


    public toDataItem(node: any): any {
        const data = node.data;
        const blocks: any[] = node?.blocks || [];
        return {
            id: node.id,
            type: node.type,
            name: data?.title,
            order: data?.order ? data?.order + '' : '0',
            view: data?.view,
            actions: data?.actions || [],
            strategies: this.toStrategyData(data),
            script: data?.script,
            blocks: blocks.map(item => {
                return this.toDataItem(item)
            }),
        }
    }

    private toStrategyRender(node: FlowNode) {
        const strategies = node.strategies || [];

        const strategyMap: any = {};

        for (let i = 0; i < strategies.length; i++) {
            const strategy = strategies[i];
            const key = strategy[NodeConvertorManager.STRATEGY_KEY];
            strategyMap[key] = {
                ...strategy
            }
        }

        return strategyMap
    }


    private toStrategyData(node: any) {
        const keys = Object.keys(node);
        const strategies: any[] = [];
        for (const key of keys) {
            if (key.endsWith(NodeConvertorManager.STRATEGY_SUFFIX)) {
                const strategy = node[key];
                strategies.push({
                    [NodeConvertorManager.STRATEGY_KEY]: key,
                    ...strategy,
                });
            }
        }
        return strategies;
    }

}

/**
 *  节点关系分析管理，分析所有的节点，可回退的节点等信息
 */
export class NodeRouterManager {
    private readonly nodes: FlowNode[];

    constructor(nodes: FlowNode[]) {
        this.nodes = nodes;
    }

    public size() {
        return this.nodes.length;
    }

    /**
     * 查看全部的可选节点
     * TODO 需要对blocks的节点进行展开查看，并提出条件控制节点
     */
    public getNodes() {
        return this.nodes;
    }

    /**
     *  获取可退回的节点
     *  TODO 需要对blocks的节点进行展开查看，并提出条件控制节点
     *  @param nodeId
     */
    public getBackNodes(nodeId: string) {
        return this.nodes;
    }
}