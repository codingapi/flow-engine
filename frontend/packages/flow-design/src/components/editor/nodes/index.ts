import {type FlowNodeRegistry} from '../typings';
import {StartNodeRegistry} from './start';
import {EndNodeRegistry} from './end';
import {SwitchNodeRegistry} from "@/components/editor/nodes/switch";
import {CaseNodeRegistry} from "@/components/editor/nodes/case";

export const FlowNodeRegistries: FlowNodeRegistry[] = [
    StartNodeRegistry,
    EndNodeRegistry,
    SwitchNodeRegistry,
    CaseNodeRegistry,
];
