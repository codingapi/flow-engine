import React from "react";
import {Flex} from "antd";
import {useNodeDisplayManager} from "@/components/editor/hooks";

export const TabBase:React.FC = () => {

    const nodeDisplayManager = useNodeDisplayManager();
    const strategyRenderManager = nodeDisplayManager.getStrategyRenderManager();

    return (
        <Flex
            justify="center"
            vertical={true}
            align={"center"}
            style={{
                width: "100%",
                padding: 8,
            }}
        >
            {strategyRenderManager.render('OperatorLoadStrategy')}
            {strategyRenderManager.render('NodeTitleStrategy')}
            {strategyRenderManager.render('MultiOperatorAuditStrategy')}
            {strategyRenderManager.render('SameOperatorAuditStrategy')}
            {strategyRenderManager.render('ErrorTriggerStrategy')}
            {strategyRenderManager.render('DelayStrategy')}
            {strategyRenderManager.render('ResubmitStrategy')}
            {strategyRenderManager.render('AdviceStrategy')}
            {strategyRenderManager.render('TimeoutStrategy')}
            {strategyRenderManager.render('RevokeStrategy')}
            {strategyRenderManager.render('TriggerStrategy')}
            {strategyRenderManager.render('RecordMergeStrategy')}
            {strategyRenderManager.render('SubProcessStrategy')}
            {strategyRenderManager.render('ConditionBranchStrategy')}
            {strategyRenderManager.render('InclusiveBranchStrategy')}
            {strategyRenderManager.render('ParallelBranchStrategy')}

        </Flex>
    )
}