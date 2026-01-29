import { IFlowValue } from '@flowgram.ai/form-materials';
import {
  FlowNodeJSON as FlowNodeJSONDefault,
  FlowNodeRegistry as FlowNodeRegistryDefault,
  FixedLayoutPluginContext,
  FlowNodeEntity,
  FlowNodeMeta as FlowNodeMetaDefault,
} from '@flowgram.ai/fixed-layout-editor';

import { type JsonSchema } from './json-schema';

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
