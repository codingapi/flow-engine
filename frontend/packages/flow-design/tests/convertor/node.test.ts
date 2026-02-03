import {describe, expect, test} from "@rstest/core";
import {WorkflowNodeConvertor} from "@/pages/design-panel/presenters/convertor/node";
import {WorkflowUtils} from "./utils";

describe.sequential('WorkflowConvert', () => {
    test('toNode1', () => {
        const workflowUtils = new WorkflowUtils();
        workflowUtils.createNodeAndSave('START');
        workflowUtils.createNodeAndSave('END');
        const workflowNodeConvertor = new WorkflowNodeConvertor(workflowUtils.createWorkflow());
        const nodes = workflowNodeConvertor.toNodes();
        console.log(nodes);
        expect(nodes.length).toBe(2);
    });

    test('toNode2', () => {
        const workflowUtils = new WorkflowUtils();
        workflowUtils.createNodeAndSave('START');
        const condition1 = workflowUtils.createNodeAndSave('CONDITION');
        const condition2 = workflowUtils.createNode('CONDITION');
        workflowUtils.addBlock(condition1.id,condition2.id,0);
        workflowUtils.createNodeAndSave('END');
        const workflowNodeConvertor = new WorkflowNodeConvertor(workflowUtils.createWorkflow());
        const nodes = workflowNodeConvertor.toNodes();
        console.log(nodes);
        expect(nodes.length).toBe(6);
    });

    test('toNode3', () => {
        const workflowUtils = new WorkflowUtils();
        workflowUtils.createNodeAndSave('START','start');
        const condition1 = workflowUtils.createNodeAndSave('CONDITION','1condition');
        const approval1 = workflowUtils.createNode('APPROVAL','approval1');
        const condition2 = workflowUtils.createNode('CONDITION','2condition');
        workflowUtils.addBlock(condition1.id,condition2.id,0);
        workflowUtils.addBlock(condition1.id,approval1.id,1);
        workflowUtils.createNodeAndSave('END','end');
        const workflowNodeConvertor = new WorkflowNodeConvertor(workflowUtils.createWorkflow());
        const nodes = workflowNodeConvertor.toNodes();
        console.log(nodes);
        expect(nodes.length).toBe(7);
    });
});