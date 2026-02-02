import {FlowNode} from "@/pages/design-panel/types";
import {StrategyRenderFactory} from "@/components/editor/node-components/strategy";

export class NodeDisplayManager {

    private readonly nodes: FlowNode[];
    private readonly currentId: string;

    public static readonly STRATEGY_KEY = 'strategyType';

    public constructor(currentId: string, nodes: FlowNode[]) {
        this.currentId = currentId;
        this.nodes = nodes;
    }

    public showAction() {
        const node = this.getNode(this.currentId);
        if (node) {
            if (node.actions && node.actions.length > 0) {
                return true;
            }
        }
        return false;
    }


    public showPromission() {
        const node = this.getNode(this.currentId);
        if (node) {
            if (node.strategies && node.strategies.length > 0) {
                for (const strategy of node.strategies) {
                    if (strategy[NodeDisplayManager.STRATEGY_KEY] === 'FormFieldPermissionStrategy') {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private getNode(id: string) {
        for (const node of this.nodes) {
            if (node.id === id) {
                return node;
            }
        }
        return null;
    }

    private getNodeStrategyKeys(){
        const node = this.getNode(this.currentId);
        if(node){
            const strategies = node.strategies || [];
            let strategyKeys:string[] = [];
            for (const strategy of strategies) {
                const strategyKey = strategy[NodeDisplayManager.STRATEGY_KEY];
                strategyKeys.push(strategyKey);
            }
            return strategyKeys;
        }
        return [];
    }

    public getStrategyRenderManager(){
        return new StrategyRenderFactory(this.getNodeStrategyKeys());
    }
}
