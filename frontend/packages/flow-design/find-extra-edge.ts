import {WorkflowNodeConvertor} from "./src/pages/design-panel/presenters/convertor/node";
import {WorkflowEdgeConvertor} from "./src/pages/design-panel/presenters/convertor/edge";
import workflow3 from "./tests/data/workflow3.json";

const nodeConvertor = new WorkflowNodeConvertor(workflow3);
const convertedNodes = nodeConvertor.toNodes();

const edgeConvertor = new WorkflowEdgeConvertor(workflow3);
const edges = edgeConvertor.toEdges();

console.log("=== Generated Edges ===");
console.log("Count:", edges.length);
edges.forEach((edge, index) => {
    const fromNode = convertedNodes.find(n => n.id === edge.from);
    const toNode = convertedNodes.find(n => n.id === edge.to);
    console.log(`${index + 1}. ${edge.from} (${fromNode?.type}) -> ${edge.to} (${toNode?.type})`);
});

// 预期的edges
const expectedEdges = [
    { from: 'e5h643JBWJJ1vJi0Hw', to: 'zbYbfl27LX85kr3ist' },
    { from: 'e5h643JBWJJ1vJi0Hw', to: 'Yu7Cc022Jo2BqpoPbP' },
    { from: 'zbYbfl27LX85kr3ist', to: 't9RE5kjkI3zHbL3Bkt' },
    { from: 'Yu7Cc022Jo2BqpoPbP', to: 'ScNjJFZyODhoFYJbVt' },
    { from: 'Yu7Cc022Jo2BqpoPbP', to: 'QfuY8B1zv7R6b7LeZ6' },
    { from: 'ScNjJFZyODhoFYJbVt', to: 't9RE5kjkI3zHbL3Bkt' },
    { from: 'QfuY8B1zv7R6b7LeZ6', to: '180HGVMuxsCwHrxekU' },
    { from: 'QfuY8B1zv7R6b7LeZ6', to: 'Grn8aoY1geqX3GZEDF' },
    { from: '180HGVMuxsCwHrxekU', to: 't9RE5kjkI3zHbL3Bkt' },
    { from: 'Grn8aoY1geqX3GZEDF', to: 't9RE5kjkI3zHbL3Bkt' }
];

console.log("\n=== Expected vs Generated ===");
expectedEdges.forEach((expected, index) => {
    const found = edges.find(edge => edge.from === expected.from && edge.to === expected.to);
    if (found) {
        console.log(`✅ ${index + 1}. ${expected.from} -> ${expected.to}`);
    } else {
        console.log(`❌ ${index + 1}. ${expected.from} -> ${expected.to}`);
    }
});

console.log("\n=== Extra Edges ===");
edges.forEach(edge => {
    const isExpected = expectedEdges.some(expected => 
        edge.from === expected.from && edge.to === expected.to
    );
    if (!isExpected) {
        console.log(`❓ ${edge.from} -> ${edge.to}`);
    }
});