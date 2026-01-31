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
            vertical={true}
            align={"center"}
            style={{
                width: "100%",
            }}
        >
            {strategyManager().render('OperatorLoadStrategy')}
            {strategyManager().render('NodeTitleStrategy')}
            {strategyManager().render('MultiOperatorAuditStrategy')}
            {strategyManager().render('SameOperatorAuditStrategy')}
            {strategyManager().render('ErrorTriggerStrategy')}
            {strategyManager().render('DelayStrategy')}
            {strategyManager().render('ResubmitStrategy')}
            {strategyManager().render('AdviceStrategy')}
            {strategyManager().render('TimeoutStrategy')}
            {strategyManager().render('RevokeStrategy')}
            {strategyManager().render('TriggerStrategy')}
            {strategyManager().render('RecordMergeStrategy')}
            {strategyManager().render('SubProcessStrategy')}

        </Flex>
    )
}