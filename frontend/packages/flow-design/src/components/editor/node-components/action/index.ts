import {actionOptions, ActionType} from "@/components/editor/typings/node-type";
import {nanoid} from "nanoid";

export class ActionManager {
    private readonly data: any[];
    private readonly onChange: (data: any[]) => void;

    public constructor(data: any[], onChange: (data: any[]) => void) {
        this.onChange = onChange;
        this.data = data;
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

    public getDatasource(actions: ActionType[]): any[] {
        if (this.data) {
            return this.data;
        }
        const list = actions.map(type => {
            const title = actionOptions.filter(value => value.value === type)[0]?.label || '未命名操作';
            return {
                id: nanoid(),
                enable: type !== 'CUSTOM',
                title: title,
                type: type,
            }
        });
        this.onChange(list);
        return list;
    }


}