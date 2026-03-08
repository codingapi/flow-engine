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
                display: node.display,
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
            display: data?.display,
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


interface MappingData {
    node: string;

    [key: string]: any;
}


export class NodeManger {
    // 设计时最新的数据
    private readonly nodes: FlowNode[];
    private readonly nodeList: FlowNode[];

    constructor(nodes: FlowNode[]) {
        this.nodes = nodes;
        this.nodeList = [];
        this.fetchNodes(this.nodes);
    }

    private fetchNodes(nodes: FlowNode[]) {
        for (const node of nodes) {
            this.nodeList.push(node);
            const blocks = node.blocks || [];
            if (node.blocks && node.blocks.length > 0) {
                this.fetchNodes(blocks);
            }
        }
    }

    /**
     * 获取可以退回的节点
     * @param nodeId
     */
    public getBackNodes(nodeId: string) {
        const list: FlowNode[] = [];
        for (const node of this.nodeList) {
            if (node.id === nodeId) {
                break;
            }
            if (node.display) {
                list.push(node);
            }
        }
        return list;
    }


    public getSize() {
        return this.nodes.length;
    }

    public getNodeByType(type:string){
        for (const node of this.nodeList) {
            if (node.type === type) {
                return node;
            }
        }
        return null;
    }


    /**
     * 获取节点
     * @param id 节点id
     */
    public getNode(id: string) {
        for (const node of this.nodeList) {
            if (node.id === id) {
                return node;
            }
        }
        return null;
    }
}

/**
 *  节点关系分析管理，分析所有的节点，可回退的节点等信息
 */
export class NodeRouterManager {
    private readonly nodes: FlowNode[];
    private readonly nodeList: FlowNode[];

    private readonly displayNodeTypes: string[];

    constructor(nodes: FlowNode[]) {
        this.nodes = nodes;
        this.displayNodeTypes = [];
        this.nodeList = [];
        this.initDisplayNodeTypes();
        this.initNodeList();
    }

    private initDisplayNodeTypes() {
        this.displayNodeTypes.push('APPROVAL');
        this.displayNodeTypes.push('DELAY');
        this.displayNodeTypes.push('END');
        this.displayNodeTypes.push('HANDLE');
        this.displayNodeTypes.push('NOTIFY');
        this.displayNodeTypes.push('ROUTER');
        this.displayNodeTypes.push('START');
        this.displayNodeTypes.push('SUB_PROCESS');
        this.displayNodeTypes.push('TRIGGER');
    }


    private initNodeList() {
        this.fetchNodes(this.nodes);
    }

    private isDisplayNode(node: FlowNode) {
        const nodeType = node.type;
        return this.displayNodeTypes.includes(nodeType);
    }

    private fetchNodes(nodes: FlowNode[]) {
        for (const node of nodes) {
            if (this.isDisplayNode(node)) {
                this.nodeList.push(node);
            }
            const blocks = node.blocks || [];
            if (node.blocks && node.blocks.length > 0) {
                this.fetchNodes(blocks);
            }
        }
    }

    public size() {
        return this.nodes.length;
    }

    /**
     * 查看全部的可选节点
     */
    public getNodes() {
        return this.nodeList.map(node => {
            return {
                label: node.name,
                value: node.id
            }
        });
    }

    /**
     *  获取可退回的节点
     *  @param nodeId
     */
    public getBackNodes(nodeId: string) {
        const list: FlowNode[] = [];
        let matchCurrent = false;
        for (const node of this.nodeList) {
            if (!matchCurrent) {
                list.push(node);
            }
            if (node.id === nodeId) {
                matchCurrent = true;
            }
        }
        return list.map(node => {
            return {
                label: node.name,
                value: node.id
            }
        });
    }

    /**
     * 处理默认数据
     * @param data
     */
    public mapping(data: MappingData) {
        const nodeId = data.node;
        let matchNode = null;
        for (const node of this.nodeList) {
            if (matchNode) {
                break;
            }
            if (node.type === nodeId) {
                matchNode = node;
            }
        }
        if (matchNode) {
            return {
                ...data,
                node: matchNode.id
            }
        }
        return data
    }
}