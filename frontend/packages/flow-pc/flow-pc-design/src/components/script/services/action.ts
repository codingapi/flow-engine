import {actionOptions, ActionType} from "@flow-engine/flow-types";
import {nanoid} from "nanoid";
import {IdUtils} from "@/utils";
import {FlowAction} from "@flow-engine/flow-types";

export class ActionManager {
    private readonly data: FlowAction[];
    private readonly onChange: (data: FlowAction[]) => void;

    public constructor(data: FlowAction[], onChange: (data: FlowAction[]) => void) {
        this.onChange = onChange;
        this.data = data;
    }

    public getCurrentNodeActionOptions() {
        const actions = this.data.filter(item => item.type !== "CUSTOM");
        const options:any[] = [];
        for (const action of actions) {
            const type = action.type;
            const option =  actionOptions.find(item => item.value === type);
            if (option) {
                options.push(option);
            }
        }
        return options;
    }

    public enable(id: any, value: boolean) {
        const data = this.data.map(item => {
            if (item.id === id) {
                return {
                    ...item,
                    enable: value,
                }
            }
            return item;
        });
        this.onChange(data);
    }


    public delete(id:string){
        const data = this.data.filter(item => item.id !== id);
        this.onChange(data);
    }

    public update(action: any) {
        const actionId = action.id;

        if(actionId) {
            const data = this.data.map(item => {
                if (item.id === actionId) {
                    return {
                        ...item,
                        title: action.title,
                        display: {
                            ...action
                        }
                    }
                }
                return item;
            });
            this.onChange(data);
        }else {
            const custom = {
                ...action,
                type:'CUSTOM',
                enable:true,
                id:IdUtils.generateId(),
            }
            this.onChange([...this.data,custom]);
        }

    }


    public getDatasource(actions: ActionType[]): any[] {
        if (this.data) {
            return this.data.map(item => {
                const enable = item.enable;
                return {
                    ...item,
                    enable: enable === undefined ? true : enable,
                }
            });
        }
        const list = actions.map(type => {
            const title = actionOptions.filter(value => value.value === type)[0]?.label || '未命名操作';
            return {
                id: nanoid(),
                enable: true,
                title: title,
                type: type,
            } as FlowAction;
        });
        this.onChange(list);
        return list;
    }


}