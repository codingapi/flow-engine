import React from "react";

/**
 *  视图绑定插件，提供视图组件注册和获取功能
 */
export class ViewBindPlugin {

    private readonly cache:Map<string,React.ComponentType<any>>;

    private  static readonly instance:ViewBindPlugin = new ViewBindPlugin();

    private constructor(){
        this.cache = new Map();
    }

    public static getInstance(){
        return this.instance;
    }

    public register(name:string,view:React.ComponentType<any>){
        this.cache.set(name,view);
    }

    public get(name:string){
        return this.cache.get(name);
    }

}