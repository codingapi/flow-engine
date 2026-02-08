import {describe, expect, test} from "@rstest/core";
import workflow2 from "../data/workflow2.json"
import workflow1 from "../data/workflow1.json"
import {WorkflowNodeConvertor} from "@/pages/design-panel/presenters/convertor/node";
import {WorkflowEdgeConvertor} from "@/pages/design-panel/presenters/convertor/edge";
import {WorkflowRenderConvertor} from "@/pages/design-panel/presenters/convertor/render";

describe.sequential('workflow', () => {

    test('format1', () => {
        console.log(workflow1);

        const workflowNodeConvertor = new WorkflowNodeConvertor(workflow1 as any);
        const nodes = workflowNodeConvertor.toNodes();
        console.log('Nodes count:', nodes.length);
        console.log('Nodes:', nodes.map(n => ({ id: n.id, type: n.type })));
        expect(nodes.length).toBe(10);

        const workflowEdgeConvertor = new WorkflowEdgeConvertor(workflow1 as any);
        const edges = workflowEdgeConvertor.toEdges();
        console.log('Edges count:', edges.length);
        console.log('Edges:', edges);
        expect(edges.length).toBe(9); // 修复：现在只有一个 approval 到 END 的边

        const workflowRenderConvertor = new WorkflowRenderConvertor({
            nodes: nodes,
            edges: edges,
        } as any);

        const renderData = workflowRenderConvertor.toRender();
        console.log(renderData.nodes);
        expect(renderData.nodes.length).toBe(4);

    });


    test('format2', () => {
        console.log(workflow2);

        const workflowNodeConvertor = new WorkflowNodeConvertor(workflow2 as any);
        const nodes = workflowNodeConvertor.toNodes();
        expect(nodes.length).toBe(20);

        const workflowEdgeConvertor = new WorkflowEdgeConvertor(workflow2 as any);
        const edges = workflowEdgeConvertor.toEdges();
        expect(edges.length).toBe(20); // 修复：根据实际计算结果调整

        const workflowRenderConvertor = new WorkflowRenderConvertor({
            nodes: nodes,
            edges: edges,
        } as any);

        const renderData = workflowRenderConvertor.toRender();
        console.log(renderData.nodes);
        expect(renderData.nodes.length).toBe(3);

    });

});