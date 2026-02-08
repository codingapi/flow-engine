import {WorkflowNodeConvertor} from "./src/pages/design-panel/presenters/convertor/node";
import {WorkflowEdgeConvertor} from "./src/pages/design-panel/presenters/convertor/edge";
import workflow3 from "./tests/data/workflow3.json";

const nodeConvertor = new WorkflowNodeConvertor(workflow3);
const convertedNodes = nodeConvertor.toNodes();

console.log("=== Converted Nodes ===");
console.log("Count:", convertedNodes.length);
convertedNodes.forEach(node => {
    console.log(`- ${node.id} (${node.type})`);
    if (node.blocks && node.blocks.length > 0) {
        node.blocks.forEach(block => {
            console.log(`  - ${block.id} (${block.type})`);
        });
    }
});

console.log("\n=== Find CqBtNJfTn4vD33Hjd9 ===");
const cqNode = convertedNodes.find(n => n.id === 'CqBtNJfTn4vD33Hjd9');
if (cqNode) {
    console.log("Found CqBtNJfTn4vD33Hjd9 node");
    console.log(cqNode);
} else {
    console.log("CqBtNJfTn4vD33Hjd9 not found in converted nodes");
}