import {WorkflowNodeConvertor} from "./src/pages/design-panel/presenters/convertor/node";
import {WorkflowEdgeConvertor} from "./src/pages/design-panel/presenters/convertor/edge";
import workflow3 from "./tests/data/workflow3.json";

console.log("=== Workflow3 ===");
console.log("Workflow nodes count:", workflow3.nodes.length);

const nodeConvertor = new WorkflowNodeConvertor(workflow3);
const convertedNodes = nodeConvertor.toNodes();

console.log("\n=== Converted Nodes ===");
console.log("Count:", convertedNodes.length);
convertedNodes.forEach(node => {
    console.log(`- ${node.id} (${node.type})`);
    if (node.blocks && node.blocks.length > 0) {
        node.blocks.forEach(block => {
            console.log(`  - ${block.id} (${block.type})`);
        });
    }
});

const edgeConvertor = new WorkflowEdgeConvertor(workflow3);
const edges = edgeConvertor.toEdges();

console.log("\n=== Generated Edges ===");
console.log("Count:", edges.length);
edges.forEach((edge, index) => {
    const fromNode = convertedNodes.find(n => n.id === edge.from);
    const toNode = convertedNodes.find(n => n.id === edge.to);
    console.log(`${index + 1}. ${edge.from} (${fromNode?.type}) -> ${edge.to} (${toNode?.type})`);
});

// 手动计算预期的edges数量
console.log("\n=== Expected Edges Analysis ===");

// 节点层次结构：
// START (e5h643JBWJJ1vJi0Hw)
//   ├─ CONDITION_BRANCH (zbYbfl27LX85kr3ist)
//   │   └─ END
//   └─ CONDITION_BRANCH (Yu7Cc022Jo2BqpoPbP)
//       ├─ CONDITION_BRANCH (ScNjJFZyODhoFYJbVt)
//       │   └─ END
//       └─ CONDITION_BRANCH (QfuY8B1zv7R6b7LeZ6)
//           └─ CONDITION_BRANCH (180HGVMuxsCwHrxekU)
//           └─ CONDITION_BRANCH (Grn8aoY1geqX3GZEDF)
//               └─ END

console.log("Expected edges:");
const expectedEdges = [
    "1. e5h643JBWJJ1vJi0Hw -> zbYbfl27LX85kr3ist",
    "2. e5h643JBWJJ1vJi0Hw -> Yu7Cc022Jo2BqpoPbP",
    "3. zbYbfl27LX85kr3ist -> t9RE5kjkI3zHbL3Bkt",
    "4. Yu7Cc022Jo2BqpoPbP -> ScNjJFZyODhoFYJbVt",
    "5. Yu7Cc022Jo2BqpoPbP -> QfuY8B1zv7R6b7LeZ6",
    "6. ScNjJFZyODhoFYJbVt -> t9RE5kjkI3zHbL3Bkt",
    "7. QfuY8B1zv7R6b7LeZ6 -> 180HGVMuxsCwHrxekU",
    "8. QfuY8B1zv7R6b7LeZ6 -> Grn8aoY1geqX3GZEDF",
    "9. 180HGVMuxsCwHrxekU -> t9RE5kjkI3zHbL3Bkt",
    "10. Grn8aoY1geqX3GZEDF -> t9RE5kjkI3zHbL3Bkt"
];

expectedEdges.forEach(edge => console.log(edge));
console.log(`Total expected: ${expectedEdges.length}`);

console.log("\n=== Missing Edges ===");
expectedEdges.forEach(expected => {
    const found = edges.some(
        e => e.from + " -> " + e.to === expected.split(". ")[1]
    );
    if (!found) {
        console.log(expected);
    }
});