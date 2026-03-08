import {actionOptions, FlowAction} from "@flow-engine/flow-types";

export class FlowActionManager {
    private readonly data: FlowAction[];

    public constructor(data: FlowAction[]) {
        this.data = data;
    }

    public getCurrentNodeActionOptions() {
        const actions = this.data.filter(item => item.type !== "CUSTOM");
        const options: {label: string; value: string}[] = [];
        for (const action of actions) {
            const type = action.type;
            const option = actionOptions.find(item => item.value === type);
            if (option) {
                options.push(option);
            }
        }
        return options;
    }

}