import React from "react";
import {DemoFixedLayout} from '@flow-engine/flowgram';

const FlowDesignWapper: React.FC = () => {
    return (
        <div style={{ height: 'calc(100vh - 100px)',width:'100%',position:'relative'  }}>
            <DemoFixedLayout />
        </div>
    );
};


export const TabFlow = ()=>{

    return (
        <FlowDesignWapper/>
    )
}