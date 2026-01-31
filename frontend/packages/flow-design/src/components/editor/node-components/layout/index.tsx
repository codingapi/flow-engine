import React from "react";
import {NodeTaps} from "@/components/editor/node-components/taps";
import {TabBase} from "@/components/editor/node-components/taps/base";


type NodeLayoutType = 'tap' | 'fall';

interface NodeLayoutProps{
    type:NodeLayoutType;
}

export const NodeLayout:React.FC<NodeLayoutProps> = (props)=>{
    if(props.type==='tap'){
        return <NodeTaps/>
    }
    return (
        <TabBase/>
    )
}