import {IFlowValue} from '@flowgram.ai/form-materials';
import {
    FixedLayoutPluginContext,
    FlowNodeEntity,
    FlowNodeJSON as FlowNodeJSONDefault,
    FlowNodeMeta as FlowNodeMetaDefault,
    FlowNodeRegistry as FlowNodeRegistryDefault,
} from '@flowgram.ai/fixed-layout-editor';

import {type JsonSchema} from './json-schema';
import {ActionType, NodeStrategyType} from "@/components/editor/typings/node-type";

export interface FlowNodeJSON extends FlowNodeJSONDefault {
    data: {
        title?: string;
        inputsValues?: Record<string, IFlowValue>;
        inputs?: JsonSchema;
        outputs?: JsonSchema;
        [key: string]: any;
    };
}

export interface FlowNodeMeta extends FlowNodeMetaDefault {
    sidebarDisable?: boolean;
    style?: React.CSSProperties;
    editTitleDisable?: boolean
    strategies?: NodeStrategyType[]
    actions?:ActionType[]
}

export interface FlowNodeRegistry extends FlowNodeRegistryDefault {
    meta?: FlowNodeMeta;
    info: {
        icon: string;
        description: string;
    };
    canAdd?: (ctx: FixedLayoutPluginContext, from: FlowNodeEntity) => boolean;
    canDelete?: (ctx: FixedLayoutPluginContext, from: FlowNodeEntity) => boolean;
    onAdd?: (ctx: FixedLayoutPluginContext, from: FlowNodeEntity) => FlowNodeJSON;
}

export type FlowDocumentJSON = {
    nodes: FlowNodeJSON[];
};
