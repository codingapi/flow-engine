import React from 'react';
import {FlowNodeJSON} from "@/components/design-editor/typings";
import {FormRenderProps,} from '@flowgram.ai/fixed-layout-editor';

export const NodeFormContext = React.createContext<FormRenderProps<FlowNodeJSON['data']>>({} as any);

