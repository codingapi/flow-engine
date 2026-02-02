import {NodeStrategyType} from "@/components/editor/typings/node-type";
import React from "react";
import {AdviceStrategy} from "@/components/editor/node-components/strategy/advice";
import {DelayStrategy} from "@/components/editor/node-components/strategy/deplay";
import {ErrorTriggerStrategy} from "@/components/editor/node-components/strategy/error-trigger";
import {MultiOperatorAuditStrategy} from "@/components/editor/node-components/strategy/multi-operator-audit";
import {NodeTitleStrategy} from "@/components/editor/node-components/strategy/node-title";
import {OperatorLoadStrategy} from "@/components/editor/node-components/strategy/operator-load";
import {RecordMergeStrategy} from "@/components/editor/node-components/strategy/record-merge";
import {ResubmitStrategy} from "@/components/editor/node-components/strategy/resubmit";
import {RevokeStrategy} from "@/components/editor/node-components/strategy/revoke";
import {SameOperatorAuditStrategy} from "@/components/editor/node-components/strategy/same-operator-audit";
import {SubProcessStrategy} from "@/components/editor/node-components/strategy/sub-process";
import {TimeoutStrategy} from "@/components/editor/node-components/strategy/timeout";
import {TriggerStrategy} from "@/components/editor/node-components/strategy/trigger";
import {RouterStrategy} from "@/components/editor/node-components/strategy/router";
import {ConditionBranchStrategy} from "@/components/editor/node-components/strategy/branch/condition";
import {InclusiveBranchStrategy} from "@/components/editor/node-components/strategy/branch/inclusive";
import {ParallelBranchStrategy} from "@/components/editor/node-components/strategy/branch/parallel";

const strategiesMap: Map<NodeStrategyType, React.ComponentType> = new Map();
strategiesMap.set('AdviceStrategy', AdviceStrategy);
strategiesMap.set('DelayStrategy', DelayStrategy);
strategiesMap.set('ErrorTriggerStrategy', ErrorTriggerStrategy);
strategiesMap.set('MultiOperatorAuditStrategy', MultiOperatorAuditStrategy);
strategiesMap.set('NodeTitleStrategy', NodeTitleStrategy);
strategiesMap.set('OperatorLoadStrategy', OperatorLoadStrategy);
strategiesMap.set('RecordMergeStrategy', RecordMergeStrategy);
strategiesMap.set('ResubmitStrategy', ResubmitStrategy);
strategiesMap.set('RevokeStrategy', RevokeStrategy);
strategiesMap.set('SameOperatorAuditStrategy', SameOperatorAuditStrategy);
strategiesMap.set('SubProcessStrategy', SubProcessStrategy);
strategiesMap.set('TimeoutStrategy', TimeoutStrategy);
strategiesMap.set('TriggerStrategy', TriggerStrategy);
strategiesMap.set('RouterStrategy', RouterStrategy);
strategiesMap.set('ConditionBranchStrategy', ConditionBranchStrategy);
strategiesMap.set('InclusiveBranchStrategy', InclusiveBranchStrategy);
strategiesMap.set('ParallelBranchStrategy', ParallelBranchStrategy);


export class StrategyRenderFactory {

    private readonly strategies: string[] = [];

    public constructor(strategies: string[]) {
        this.strategies = strategies;
    }

    public getComponent(type: NodeStrategyType): React.ComponentType | undefined {
        for (const key in this.strategies) {
            if (this.strategies[key] === type) {
                return strategiesMap.get(type);
            }
        }
        return undefined;
    }

    public render(type: NodeStrategyType) {
        const Component = this.getComponent(type);
        if (Component) {
            return <Component/>;
        }
    }

}