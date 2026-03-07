import React from "react";
import {Input} from "antd";


interface ActionIconProps{
    value?:string;
    onChange?:(value:string) => void;
}

export const ActionIcon:React.FC<ActionIconProps> = (props)=>{
    return (
        <>
            <Input placeholder={"请输入按钮图标"}/>
        </>
    )
}