export class WorkflowStrategyManager {
    private readonly strategies:any[];

    public static TYPE_KEY = 'strategyType';

    public static KEY_INTERFERE_STRATEGY = 'InterfereStrategy';
    public static KEY_URGE_STRATEGY = 'UrgeStrategy';

    constructor(strategies:any[]) {
        this.strategies = strategies;
    }

    public toForm(){
        let value = {};
        for (const key in this.strategies){
           const currentValue = this.strategies[key];
           value = Object.assign(value,{
               [key]:currentValue,
           });
        }
        value = {
            strategies: value
        }
        return value;
    }

    public getStrategy(key: string): any {
        const strategies:any[] = this.strategies;
        for(const strategy of strategies){
            if(strategy[key]){
                return strategy;
            }
        }
        return null;
    }


}