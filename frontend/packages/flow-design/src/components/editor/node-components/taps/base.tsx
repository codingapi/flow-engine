import React, {useContext} from "react";
import {Flex} from "antd";
import {NodeRenderContext} from "@/components/editor/context";
import {StrategyManager} from "@/components/editor/node-components/strategy";

export const TabBase:React.FC = () => {
    const {node} = useContext(NodeRenderContext);
    const strategies = node.getNodeRegistry()?.meta.strategies || [];
    const strategyManager = React.useCallback(()=>{
        return new StrategyManager(strategies);
    },[strategies]);
    return (
        <Flex
            justify="center"
            style={{
                width: "100%",
            }}
        >
            {strategyManager().render('Operator')}
            {strategyManager().render('CustomTitle')}
            {strategyManager().render('Trigger')}
        </Flex>
    )
}