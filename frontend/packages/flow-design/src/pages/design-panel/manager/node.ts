import {FlowNode} from "@/pages/design-panel/types";

export class NodeManager {

    public static readonly STRATEGY_SUFFIX = 'Strategy';
    public static readonly STRATEGY_KEY = 'strategyType';

    public constructor() {
    }

    public toRender(nodes: FlowNode[]) {
        return nodes.map(node => {
            return {
                id: node.id,
                type: node.type,
                data: {
                    title: node.name,
                    order: node.order,
                    actions: node.actions,
                    ...this.toStrategyRender(node),
                }
            }
        });
    }

    public toData(nodes: any[]) {
        return nodes.map(node => {
            const data = node.data;
            return {
                id: node.id,
                type: node.type,
                name: data?.title,
                order: data?.order ? data?.order + '' : '0',
                actions: data?.actions || [],
                strategies: this.toStrategyData(data)
            }
        });
    }


    private toStrategyRender(node: FlowNode) {
        const strategies = node.strategies || [];

        const strategyMap: any = {};

        for (let i = 0; i < strategies.length; i++) {
            const strategy = strategies[i];
            const key = strategy[NodeManager.STRATEGY_KEY];
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
            if (key.endsWith(NodeManager.STRATEGY_SUFFIX)) {
                const strategy = node[key];
                strategies.push({
                    [NodeManager.STRATEGY_KEY]: key,
                    ...strategy,
                });
            }
        }
        return strategies;
    }

}