import {type FlowNodeRegistry} from '../typings';
import {StartNodeRegistry} from './start';
import {EndNodeRegistry} from './end';
import {ConditionNodeRegistry} from "@/components/editor/nodes/condition";
import {ConditionBranchNodeRegistry} from "@/components/editor/nodes/condition-branch";
import {ApprovalNodeRegistry} from "@/components/editor/nodes/approval";
import {DelayNodeRegistry} from "@/components/editor/nodes/delay";
import {HandleNodeRegistry} from "@/components/editor/nodes/handle";
import {InclusiveNodeRegistry} from "@/components/editor/nodes/inclusive";
import {InclusiveBranchNodeRegistry} from "@/components/editor/nodes/inclusive-branch";
import {NotifyNodeRegistry} from "@/components/editor/nodes/notify";
import {ParallelNodeRegistry} from "@/components/editor/nodes/parallel";
import {ParallelBranchNodeRegistry} from "@/components/editor/nodes/parallel-branch";
import {RouterNodeRegistry} from "@/components/editor/nodes/router";
import {SubProcessNodeRegistry} from "@/components/editor/nodes/sub-process";
import {TriggerNodeRegistry} from "@/components/editor/nodes/trigger";

export const FlowNodeRegistries: FlowNodeRegistry[] = [
    ApprovalNodeRegistry,
    ConditionNodeRegistry,
    ConditionBranchNodeRegistry,
    DelayNodeRegistry,
    EndNodeRegistry,
    HandleNodeRegistry,
    InclusiveNodeRegistry,
    InclusiveBranchNodeRegistry,
    NotifyNodeRegistry,
    ParallelNodeRegistry,
    ParallelBranchNodeRegistry,
    RouterNodeRegistry,
    StartNodeRegistry,
    SubProcessNodeRegistry,
    TriggerNodeRegistry,
];
