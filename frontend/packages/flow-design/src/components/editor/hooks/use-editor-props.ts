import '@flowgram.ai/fixed-layout-editor/index.css';
import {useMemo} from 'react';
import {createMinimapPlugin} from '@flowgram.ai/minimap-plugin';
import {defaultFixedSemiMaterials} from '@flowgram.ai/fixed-semi-materials';
import {ConstantKeys, FixedLayoutProps, FlowLayoutDefault, FlowRendererKey} from '@flowgram.ai/fixed-layout-editor';

import {NodeRender} from '../nodes/node-render';
import {nodeRegistries} from '../nodes/node-registries';
import {initialData} from '../initial-data';
import {Adder} from '../nodes/adder';
import {createDownloadPlugin} from "@flowgram.ai/export-plugin";
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
             * 画布相关配置
             * Canvas-related configurations
             */
            playground: {
                ineractiveType: 'MOUSE',
                /**
                 * Prevent Mac browser gestures from turning pages
                 * 阻止 mac 浏览器手势翻页
                 */
                preventGlobalGesture: true,
            },
            /**
             * Whether it is read-only or not, the node cannot be dragged in read-only mode
             */
            readonly: false,
            /**
             * Set default layout
             */
            defaultLayout: FlowLayoutDefault.VERTICAL_FIXED_LAYOUT, // or FlowLayoutDefault.HORIZONTAL_FIXED_LAYOUT
            /**
             * Style config
             */
            constants: {
                // [ConstantKeys.NODE_SPACING]: 24,
                // [ConstantKeys.BRANCH_SPACING]: 20,
                // [ConstantKeys.INLINE_SPACING_BOTTOM]: 24,
                // [ConstantKeys.INLINE_BLOCKS_INLINE_SPACING_BOTTOM]: 13,
                // [ConstantKeys.ROUNDED_LINE_RADIUS]: 32,
                // [ConstantKeys.ROUNDED_LINE_X_RADIUS]: 8,
                // [ConstantKeys.ROUNDED_LINE_Y_RADIUS]: 10,
                // [ConstantKeys.INLINE_BLOCKS_INLINE_SPACING_TOP]: 23,
                // [ConstantKeys.INLINE_BLOCKS_PADDING_BOTTOM]: 30,
                [ConstantKeys.COLLAPSED_SPACING]: 10,
                [ConstantKeys.BASE_COLOR]: '#B8BCC1',
                [ConstantKeys.BASE_ACTIVATED_COLOR]: '#82A7FC',
            },

            /**
             * Drag/Drop config
             */
            dragdrop: {
                /**
                 * Callback when drag drop
                 */
                onDrop: (ctx, dropData) => {
                    // console.log(
                    //   '>>> onDrop: ',
                    //   dropData.dropNode.id,
                    //   dropData.dragNodes.map(n => n.id),
                    // );
                },
                canDrop: (ctx, dropData) =>
                    // console.log(
                    //   '>>> canDrop: ',
                    //   dropData.isBranch,
                    //   dropData.dropNode.id,
                    //   dropData.dragNodes.map(n => n.id),
                    // );
                    true,
            },
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
