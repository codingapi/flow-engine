import React from "react";
import {
    FixedLayoutPluginContext,
    FlowNodeEntity,
    FlowNodeJSON as FlowNodeJSONDefault,
    FlowNodeMeta as FlowNodeMetaDefault,
    FlowNodeRegistry as FlowNodeRegistryDefault,
} from '@flowgram.ai/fixed-layout-editor';

export interface FlowNodeJSON extends FlowNodeJSONDefault {
    data: {
        title?: string;
        actions?: any[];
        order?:number;
        [key: string]: any;
    };
}

export interface FlowNodeMeta extends FlowNodeMetaDefault {
    sidebarDisable?: boolean;
    style?: React.CSSProperties;
    editTitleDisable?: boolean
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
