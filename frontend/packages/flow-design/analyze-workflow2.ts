import {WorkflowNodeConvertor} from "./src/pages/design-panel/presenters/convertor/node";
import {WorkflowEdgeConvertor} from "./src/pages/design-panel/presenters/convertor/edge";
import workflow2 from "./tests/data/workflow2.json";

const nodeConvertor = new WorkflowNodeConvertor(workflow2);
const convertedNodes = nodeConvertor.toNodes();

const edgeConvertor = new WorkflowEdgeConvertor(workflow2);
const edges = edgeConvertor.toEdges();

console.log("=== Workflow2 Analysis ===");
console.log("Nodes count:", convertedNodes.length);
console.log("\nEdges count:", edges.length);

console.log("\n=== Generated Edges ===");
edges.forEach((edge, index) => {
    const fromNode = convertedNodes.find(n => n.id === edge.from);
    const toNode = convertedNodes.find(n => n.id === edge.to);
    console.log(`${index + 1}. ${edge.from} (${fromNode?.type}) -> ${edge.to} (${toNode?.type})`);
});