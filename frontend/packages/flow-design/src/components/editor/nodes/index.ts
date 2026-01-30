import {type FlowNodeRegistry} from '../typings';
import {StartNodeRegistry} from './start';
import {EndNodeRegistry} from './end';
import {ConditionNodeRegistry} from "@/components/editor/nodes/condition";
import {ConditionBranchNodeRegistry} from "@/components/editor/nodes/condition-branch";

export const FlowNodeRegistries: FlowNodeRegistry[] = [
    StartNodeRegistry,
    EndNodeRegistry,
    ConditionNodeRegistry,
    ConditionBranchNodeRegistry,
];
