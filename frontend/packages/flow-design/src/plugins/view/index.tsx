import React from "react";
import {FormInstance} from "antd";

export type ViewComponentProps  = {
    form:FormInstance;
}

export class ViewPlugin{

    private readonly cache:Map<string,React.ComponentType<ViewComponentProps>>;

    private  static readonly instance:ViewPlugin = new ViewPlugin();

    private constructor(){
        this.cache = new Map();
    }

    public static getInstance(){
        return this.instance;
    }

    public register(name:string,view:React.ComponentType<ViewComponentProps>){
        this.cache.set(name,view);
    }

    public get(name:string){
        return this.cache.get(name);
    }

}