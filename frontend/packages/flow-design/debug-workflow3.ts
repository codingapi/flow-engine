import {WorkflowNodeConvertor} from "./src/pages/design-panel/presenters/convertor/node";
import {WorkflowEdgeConvertor} from "./src/pages/design-panel/presenters/convertor/edge";
import workflow3 from "./tests/data/workflow3.json";

console.log("=== Workflow3 Node Analysis ===");
console.log("Original workflow3 nodes count:", workflow3.nodes.length);
console.log("Nodes:");
workflow3.nodes.forEach(node => {
    console.log(`- ${node.id} (${node.type})`);
    if (node.blocks && node.blocks.length > 0) {
        node.blocks.forEach(block => {
            console.log(`  - ${block.id} (${block.type})`);
            if (block.blocks && block.blocks.length > 0) {
                block.blocks.forEach(subBlock => {
                    console.log(`    - ${subBlock.id} (${subBlock.type})`);
                    if (subBlock.blocks && subBlock.blocks.length > 0) {
                        subBlock.blocks.forEach(subSubBlock => {
                            console.log(`      - ${subSubBlock.id} (${subSubBlock.type})`);
                        });
                    }
                });
            }
        });
    }
});

const nodeConvertor = new WorkflowNodeConvertor(workflow3);
const convertedNodes = nodeConvertor.toNodes();

console.log("\n=== Converted Nodes ===");
console.log("Converted nodes count:", convertedNodes.length);
convertedNodes.forEach(node => {
    console.log(`- ${node.id} (${node.type})`);
});

const edgeConvertor = new WorkflowEdgeConvertor(workflow3);
const edges = edgeConvertor.toEdges();

console.log("\n=== Generated Edges ===");
console.log("Edges count:", edges.length);
edges.forEach((edge, index) => {
    const fromNode = convertedNodes.find(n => n.id === edge.from);
    const toNode = convertedNodes.find(n => n.id === edge.to);
    console.log(`${index + 1}. ${edge.from} (${fromNode?.type}) -> ${edge.to} (${toNode?.type})`);
});