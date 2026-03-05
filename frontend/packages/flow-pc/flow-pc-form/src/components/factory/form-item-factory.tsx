import {FormField} from "@flow-engine/flow-types";
import React from "react";

import {FormItemString} from "@/components/item/string";
import {FormItemNumber} from "@/components/item/number";
import {FormItemFloat} from "@/components/item/float";
import {FormItemBoolean} from "@/components/item/boolean";


export class FormItemFactory {

    private readonly cache:Map<string,React.ComponentType<FormField>>;

    private static instance:FormItemFactory = new FormItemFactory();

    private constructor() {
        this.cache = new Map();
        this.cache.set('number',FormItemNumber);
        this.cache.set('string',FormItemString);
        this.cache.set('float',FormItemFloat);
        this.cache.set('boolean',FormItemBoolean);
    }

    public static getInstance():FormItemFactory {
        return FormItemFactory.instance;
    }

    public createFrom(formType:string){
        return this.cache.get(formType);
    }

}