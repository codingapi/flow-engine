import {describe, expect, test} from "@rstest/core";
import {WorkflowRenderConvertor} from "@/pages/design-panel/presenters/convertor/render";
import {WorkflowUtils} from "./utils";
import {WorkflowEdgeConvertor} from "@/pages/design-panel/presenters/convertor/edge";
import {WorkflowNodeConvertor} from "@/pages/design-panel/presenters/convertor/node";

describe.sequential('WorkflowConvert', () => {

    test('toRender1', () => {
        const workflowUtils = new WorkflowUtils();
        workflowUtils.createNodeAndSave('START', 'start');
        const condition1 = workflowUtils.createNodeAndSave('CONDITION', '1condition');
        const approval1 = workflowUtils.createNode('APPROVAL', 'approval1');
        const condition2 = workflowUtils.createNode('CONDITION', '2condition');
        workflowUtils.addBlock(condition1.id, condition2.id, 0);
        workflowUtils.addBlock(condition1.id, approval1.id, 1);
        workflowUtils.createNodeAndSave('END', 'end');
        const workflow = workflowUtils.createWorkflow();

        const workflowNodeConvertor = new WorkflowNodeConvertor(workflow);
        const nodes = workflowNodeConvertor.toNodes();
        const workflowEdgeConvertor = new WorkflowEdgeConvertor(workflow);
        const edges = workflowEdgeConvertor.toEdges();
        const workflowRenderConvertor = new WorkflowRenderConvertor({
            nodes: nodes,
            edges: edges,
        } as any);
        const renderData = workflowRenderConvertor.toRender();
        console.log(renderData.nodes);
        expect(renderData.nodes.length).toBe(3);
    });

});