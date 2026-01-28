import React from "react";
import {FlowEditor} from "@/components/editor";

export const TabFlow = ()=>{

    return (
        <div style={{ height: 'calc(100vh - 100px)',width:'100%',position:'relative'  }}>
            <FlowEditor/>
        </div>
    )
}