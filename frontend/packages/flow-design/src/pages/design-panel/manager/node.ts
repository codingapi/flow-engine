import {FlowNode} from "@/pages/design-panel/types";

export class NodeManager {

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