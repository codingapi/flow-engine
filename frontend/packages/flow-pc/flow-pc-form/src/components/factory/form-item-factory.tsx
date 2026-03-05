import {FormField} from "@flow-engine/flow-types";
import React from "react";

import {FormItemString} from "@/components/item/string";
import {FormItemNumber} from "@/components/item/number";
import {FormItemBoolean} from "@/components/item/boolean";
import {DataType} from "@flow-engine/flow-types";
import {FormItemDate} from "@/components/item/date";


export class FormItemFactory {

    private readonly cache:Map<DataType,React.ComponentType<FormField>>;

    private static instance:FormItemFactory = new FormItemFactory();

    private constructor() {
        this.cache = new Map();
        this.cache.set('NUMBER',FormItemNumber);
        this.cache.set('STRING',FormItemString);
        this.cache.set('DATE',FormItemDate);
        this.cache.set('BOOLEAN',FormItemBoolean);
    }

    public static getInstance():FormItemFactory {
        return FormItemFactory.instance;
    }

    public createFrom(formType:DataType){
        return this.cache.get(formType);
    }

}