import {describe, expect, test} from "@rstest/core";
import workflow from "./workflow.json"
import {WorkflowNodeConvertor} from "@/pages/design-panel/presenters/convertor/node";
import {WorkflowEdgeConvertor} from "@/pages/design-panel/presenters/convertor/edge";
import {WorkflowRenderConvertor} from "@/pages/design-panel/presenters/convertor/render";

describe.sequential('workflow', () => {

    test('format', () => {
        console.log(workflow);

        const workflowNodeConvertor = new WorkflowNodeConvertor(workflow as any);
        const nodes = workflowNodeConvertor.toNodes();
        expect(nodes.length).toBe(20);

        const workflowEdgeConvertor = new WorkflowEdgeConvertor(workflow as any);
        const edges = workflowEdgeConvertor.toEdges();
        expect(edges.length).toBe(24);

        const workflowRenderConvertor = new WorkflowRenderConvertor({
            nodes: nodes,
            edges: edges,
        } as any);

        const renderData = workflowRenderConvertor.toRender();
        console.log(renderData.nodes);
        expect(renderData.nodes.length).toBe(3);

    });

});