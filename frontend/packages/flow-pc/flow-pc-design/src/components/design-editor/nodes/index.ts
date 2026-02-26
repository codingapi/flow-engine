import {type FlowNodeRegistry} from '../typings';
import {StartNodeRegistry} from './start';
import {EndNodeRegistry} from './end';
import {ConditionNodeRegistry} from "@/components/design-editor/nodes/condition";
import {ConditionBranchNodeRegistry} from "@/components/design-editor/nodes/condition-branch";
import {ApprovalNodeRegistry} from "@/components/design-editor/nodes/approval";
import {DelayNodeRegistry} from "@/components/design-editor/nodes/delay";
import {HandleNodeRegistry} from "@/components/design-editor/nodes/handle";
import {InclusiveNodeRegistry} from "@/components/design-editor/nodes/inclusive";
import {InclusiveBranchNodeRegistry} from "@/components/design-editor/nodes/inclusive-branch";
import {NotifyNodeRegistry} from "@/components/design-editor/nodes/notify";
import {ParallelNodeRegistry} from "@/components/design-editor/nodes/parallel";
import {ParallelBranchNodeRegistry} from "@/components/design-editor/nodes/parallel-branch";
import {RouterNodeRegistry} from "@/components/design-editor/nodes/router";
import {SubProcessNodeRegistry} from "@/components/design-editor/nodes/sub-process";
import {TriggerNodeRegistry} from "@/components/design-editor/nodes/trigger";

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
