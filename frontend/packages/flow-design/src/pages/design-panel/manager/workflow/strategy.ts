import {ObjectUtils} from "@flow-engine/flow-core";

export class WorkflowStrategyManager {

    public static TYPE_KEY = 'strategyType';

    constructor() {
    }

    public toForm(strategies: any[]): any {
        let value = {};
        for (const key in strategies){
           const currentValue = strategies[key];
           value = Object.assign(value, {
               [currentValue[WorkflowStrategyManager.TYPE_KEY]]:{
                   ...currentValue
               },
           });
        }
        value = {
            strategies: value
        }
        return value;
    }


    public toList(values: any) {
        if(ObjectUtils.isEmptyObject(values)){
            return null;
        }
        const strategies:any[] = [];
        for (const key in values.strategies){
            const item = values.strategies[key];
            strategies.push({
                [WorkflowStrategyManager.TYPE_KEY]:key,
                ...item,
            });
        }
        return {
            strategies: strategies
        };
    }
}