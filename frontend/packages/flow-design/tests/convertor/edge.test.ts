import {describe, expect, test} from "@rstest/core";
import {WorkflowEdgeConvertor} from "@/pages/design-panel/presenters/convertor/edge";
import {WorkflowUtils} from "./utils";



describe.sequential('WorkflowConvert', () => {

    test('toEdge1', () => {
        const workflowUtils = new WorkflowUtils();
        workflowUtils.createNodeAndSave('START');
        workflowUtils.createNodeAndSave('END');
        const workflowEdgeConvertor = new WorkflowEdgeConvertor(workflowUtils.createWorkflow());
        const edges = workflowEdgeConvertor.toEdges();
        console.log(edges);
        expect(edges.length).toBe(1);
    });

    test('toEdge2', () => {
        const workflowUtils = new WorkflowUtils();
        workflowUtils.createNodeAndSave('START');
        const condition1 = workflowUtils.createNodeAndSave('CONDITION');
        const condition2 = workflowUtils.createNode('CONDITION');
        workflowUtils.addBlock(condition1.id,condition2.id,0);
        workflowUtils.createNodeAndSave('END');
        const workflowEdgeConvertor = new WorkflowEdgeConvertor(workflowUtils.createWorkflow());
        const edges = workflowEdgeConvertor.toEdges();
        console.log(edges);
        expect(edges.length).toBe(7);
    });

    test('toEdge3', () => {
        const workflowUtils = new WorkflowUtils();
        workflowUtils.createNodeAndSave('START','start');
        const condition1 = workflowUtils.createNodeAndSave('CONDITION','1condition');
        const approval1 = workflowUtils.createNode('APPROVAL','approval1');
        const condition2 = workflowUtils.createNode('CONDITION','2condition');
        workflowUtils.addBlock(condition1.id,condition2.id,0);
        workflowUtils.addBlock(condition1.id,approval1.id,1);
        workflowUtils.createNodeAndSave('END','end');
        const workflow = workflowUtils.createWorkflow();
        const workflowEdgeConvertor = new WorkflowEdgeConvertor(workflow);
        const edges = workflowEdgeConvertor.toEdges();
        console.log(edges);
        expect(edges.length).toBe(8);
    });
});