import '@flowgram.ai/fixed-layout-editor/index.css';
import { useMemo } from 'react';
import { createMinimapPlugin } from '@flowgram.ai/minimap-plugin';
import { defaultFixedSemiMaterials } from '@flowgram.ai/fixed-semi-materials';
import { FlowRendererKey, FixedLayoutProps } from '@flowgram.ai/fixed-layout-editor';

import { NodeRender } from '../nodes/node-render';
import { nodeRegistries } from '../nodes/node-registries';
import { initialData } from '../initial-data';
import { Adder } from '../nodes/adder';
import { createDownloadPlugin } from "@flowgram.ai/export-plugin";
import {debounce} from "lodash-es";

export function useEditorProps(): FixedLayoutProps {
    return useMemo<FixedLayoutProps>(
        () => ({
            plugins: () => [
                /**
                 *  minimap 小地图
                 */
                createMinimapPlugin({
                    disableLayer: true,
                    enableDisplayAllNodes: true,
                    canvasStyle: {
                        canvasWidth: 182,
                        canvasHeight: 102,
                        canvasPadding: 50,
                        canvasBackground: 'rgba(245, 245, 245, 1)',
                        canvasBorderRadius: 10,
                        viewportBackground: 'rgba(235, 235, 235, 1)',
                        viewportBorderRadius: 4,
                        viewportBorderColor: 'rgba(201, 201, 201, 1)',
                        viewportBorderWidth: 1,
                        viewportBorderDashLength: 2,
                        nodeColor: 'rgba(255, 255, 255, 1)',
                        nodeBorderRadius: 2,
                        nodeBorderWidth: 0.145,
                        nodeBorderColor: 'rgba(6, 7, 9, 0.10)',
                        overlayColor: 'rgba(255, 255, 255, 0)',
                    },
                }),
                /**
                 * Download plugin
                 * 下载插件
                 */
                createDownloadPlugin({}),
            ],
            nodeRegistries,
            initialData,
            /**
             * Redo/Undo enable
             */
            history: {
                enable: true,
                enableChangeNode: true, // Listen Node engine data change
                onApply: debounce((ctx:any, opt:any) => {
                    if (ctx.document.disposed) return;
                    // Listen change to trigger auto save
                    console.log('auto save: ', ctx.document.toJSON());
                }, 100),
            },
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
