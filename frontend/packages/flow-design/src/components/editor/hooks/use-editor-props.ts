import '@flowgram.ai/fixed-layout-editor/index.css';
import { useMemo } from 'react';
import { createMinimapPlugin } from '@flowgram.ai/minimap-plugin';
import { defaultFixedSemiMaterials } from '@flowgram.ai/fixed-semi-materials';
import { FlowRendererKey, FixedLayoutProps } from '@flowgram.ai/fixed-layout-editor';

import { NodeRender } from '../nodes/node-render';
import { nodeRegistries } from '../nodes/node-registries';
import { initialData } from '../initial-data';
import { Adder } from '../nodes/adder';

export function useEditorProps(): FixedLayoutProps {
    return useMemo<FixedLayoutProps>(
        () => ({
            plugins: () => [
                createMinimapPlugin({
                    enableDisplayAllNodes: true,
                }),
            ],
            nodeRegistries,
            initialData,
            onAllLayersRendered: (ctx) => {
                setTimeout(() => {
                    ctx.playground.config.fitView(ctx.document.root.bounds.pad(30));
                }, 10);
            },
            materials: {
                renderDefaultNode: NodeRender,
                components: {
                    ...defaultFixedSemiMaterials,
                    [FlowRendererKey.ADDER]: Adder,
                },
            },
        }),
        []
    );
}
