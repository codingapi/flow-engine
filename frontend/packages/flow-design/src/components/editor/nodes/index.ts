import {type FlowNodeRegistry} from '../typings';
import {StartNodeRegistry} from './start';
import {EndNodeRegistry} from './end';
import {CustomNodeRegistry} from "@/components/editor/nodes/custom";

export const FlowNodeRegistries: FlowNodeRegistry[] = [
    StartNodeRegistry,
    EndNodeRegistry,
    CustomNodeRegistry,
];
